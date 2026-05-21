package com.mawaai.love.app.design.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.hypot

// Action palette — matches existing MawaaiColors (Rose Gold, etc.)
// but kept inline because these are graphics-pipeline constants,
// not Compose colors. Future palette tweaks can pull from a JSON
// catalog if needed.
private const val CREAM_BG = 0xFFFAF3E6.toInt()
private const val ROSE_GRADIENT_TOP = 0xFFE8B4B8.toInt()
private const val GOLD_GRADIENT_BOTTOM = 0xFFC8860A.toInt()
private const val VIGNETTE_EDGE = 0x80000000.toInt()      // 50% black at corners
private const val ACCENT_ROSE = 0x40D47A5C                // 25% alpha henna rose
private const val BRIGHTNESS_OFFSET = 40f                 // ColorMatrix offset (≈16%)

// Phase 20 constants. Tuned on a small set of test sketches
// (~20) — adjust if user feedback indicates the changes are
// too subtle or too aggressive.

// Soft-symmetry blend weight: 0.6 keeps 40% of the original
// asymmetric strokes visible. Drop to 0.85 for "near-perfect"
// symmetry; raise to 0.4 to barely shift the composition.
private const val SYMMETRY_BLEND: Float = 0.6f

// Inked-pixel alpha threshold. Mirrors LocalDrawingAnalyzer's
// 24/255 cutoff — keep these in sync if tuned.
private const val INKED_ALPHA_THRESHOLD: Int = 24

// Palette analysis: 12 buckets × 30° each. Top 3 hues drive
// the rebalance.
private const val PALETTE_HUE_BUCKETS: Int = 12
private const val PALETTE_BUCKETS: Int = 3
// Pixels less saturated than this are ignored — outlines /
// grayscale / shadow strokes shouldn't get hue-shifted.
private const val PALETTE_MIN_SATURATION: Float = 0.18f
// How far each pixel's hue moves toward its nearest dominant
// bucket. 0.5 = halfway, 1.0 = clamped to bucket center.
private const val PALETTE_NUDGE_FRACTION: Float = 0.5f

internal fun drawSolidBackground(input: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
    Canvas(output).apply {
        drawColor(CREAM_BG)
        drawBitmap(input, 0f, 0f, null)
    }
    return output
}

internal fun drawGradientBackground(input: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, 0f, input.height.toFloat(),
            ROSE_GRADIENT_TOP, GOLD_GRADIENT_BOTTOM,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, input.width.toFloat(), input.height.toFloat(), paint)
    canvas.drawBitmap(input, 0f, 0f, null)
    return output
}

internal fun drawVignette(input: Bitmap): Bitmap {
    val output = input.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(output)
    val cx = input.width / 2f
    val cy = input.height / 2f
    val radius = hypot(cx.toDouble(), cy.toDouble()).toFloat()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx, cy, radius,
            intArrayOf(0x00000000, 0x00000000, VIGNETTE_EDGE),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, input.width.toFloat(), input.height.toFloat(), paint)
    return output
}

internal fun drawMirrorLeftToRight(input: Bitmap): Bitmap {
    val output = input.copy(Bitmap.Config.ARGB_8888, true)
    val halfWidth = input.width / 2
    if (halfWidth <= 0) return output
    val leftHalf = Bitmap.createBitmap(input, 0, 0, halfWidth, input.height)
    val canvas = Canvas(output)
    // Mirror horizontally by negative-scaling, then translate by the
    // full width so the source's column 0 lands at `input.width - 1`
    // and the source's column `halfWidth - 1` lands at `halfWidth`.
    val matrix = Matrix().apply {
        preScale(-1f, 1f)
        postTranslate(input.width.toFloat(), 0f)
    }
    canvas.drawBitmap(leftHalf, matrix, null)
    leftHalf.recycle()
    return output
}

internal fun drawAccentTint(input: Bitmap): Bitmap {
    val output = input.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ACCENT_ROSE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }
    canvas.drawRect(0f, 0f, input.width.toFloat(), input.height.toFloat(), paint)
    return output
}

internal fun drawLightenColorMatrix(input: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, BRIGHTNESS_OFFSET,
                    0f, 1f, 0f, 0f, BRIGHTNESS_OFFSET,
                    0f, 0f, 1f, 0f, BRIGHTNESS_OFFSET,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
    canvas.drawBitmap(input, 0f, 0f, paint)
    return output
}

/**
 * Phase 20 — `ThickenThinStrokes`. Pure-Android approximation of a
 * 3×3 morphological dilation: draws the source bitmap four times at
 * 1-px offsets in the four cardinal directions, plus once at the
 * origin. Pixels with non-zero alpha "spread" to their immediate
 * neighbours, so a 1-px stroke widens to ~3 px and a 2-px stroke
 * widens to ~4 px. Empty regions stay empty (alpha = 0 stays 0).
 *
 * The four-directional spread is the lightest-weight approximation
 * that still produces visible thickening; an OpenCV `Imgproc.dilate`
 * with a 3×3 cross kernel would be ~equivalent and ~30% faster, but
 * this action ships even when OpenCV is unavailable.
 */
internal fun drawThickenStrokes(input: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = false }
    // Five draws: center + 4 cardinal-direction 1-px shifts. Order
    // doesn't matter for SRC_OVER alpha compositing.
    canvas.drawBitmap(input, 0f, 0f, paint)
    canvas.drawBitmap(input, -1f, 0f, paint)
    canvas.drawBitmap(input, 1f, 0f, paint)
    canvas.drawBitmap(input, 0f, -1f, paint)
    canvas.drawBitmap(input, 0f, 1f, paint)
    return output
}

/**
 * Phase 20 — `FixSymmetry`. Soft mirror: builds a fully-symmetric
 * version of the input by averaging the left half with the
 * horizontally-flipped right half, then blends that symmetric copy
 * back over the original at [SYMMETRY_BLEND] (0.6) weight. Pure
 * `MirrorHorizontally` overwrites half the canvas; this preserves
 * roughly 40% of the original asymmetric strokes while pulling
 * symmetric drift back into balance.
 */
internal fun drawFixSymmetry(input: Bitmap): Bitmap {
    val w = input.width
    val h = input.height
    if (w <= 1) return input.copy(Bitmap.Config.ARGB_8888, true)

    // Build the mirrored copy.
    val mirrored = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val mCanvas = Canvas(mirrored)
    val flipMatrix = Matrix().apply {
        preScale(-1f, 1f)
        postTranslate(w.toFloat(), 0f)
    }
    mCanvas.drawBitmap(input, flipMatrix, null)

    // Average original + mirror by drawing each at 50% alpha onto a
    // fresh bitmap. This produces the "perfectly symmetric" version.
    val symmetric = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val sCanvas = Canvas(symmetric)
    val halfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = 128 // 50%
        isFilterBitmap = true
    }
    sCanvas.drawBitmap(input, 0f, 0f, halfPaint)
    sCanvas.drawBitmap(mirrored, 0f, 0f, halfPaint)
    mirrored.recycle()

    // Blend the symmetric copy over the original at SYMMETRY_BLEND
    // weight. Results in a ~60% symmetric, ~40% original mix.
    val output = input.copy(Bitmap.Config.ARGB_8888, true)
    val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = (SYMMETRY_BLEND * 255).toInt()
        isFilterBitmap = true
    }
    Canvas(output).drawBitmap(symmetric, 0f, 0f, blendPaint)
    symmetric.recycle()
    return output
}

/**
 * Phase 20 — `BalancePalette`. Buckets every inked pixel's hue into
 * 12 30°-wide buckets, picks the top [PALETTE_BUCKETS] (3) by count,
 * and shifts each pixel's hue 50% toward its nearest bucket center.
 * Saturation and value are preserved — only the hue moves.
 *
 * Pixels with sub-[PALETTE_MIN_SATURATION] saturation (grayscale,
 * outlines) are left untouched: nudging their hue would tint the
 * outline color with no perceptual benefit. Pixels with alpha < 24
 * (the analyzer's "inked" threshold) are also left untouched so
 * the alpha channel stays clean.
 *
 * Pure-Android implementation. Uses [android.graphics.Color.colorToHSV]
 * + [android.graphics.Color.HSVToColor] for conversion — same call
 * signature as the canvas color picker. ~80 ms on a 1024×1024 bitmap.
 */
internal fun drawBalancePalette(input: Bitmap): Bitmap {
    val w = input.width
    val h = input.height
    val pixels = IntArray(w * h)
    input.getPixels(pixels, 0, w, 0, 0, w, h)

    val hsv = FloatArray(3)
    val histogram = IntArray(PALETTE_HUE_BUCKETS)
    for (px in pixels) {
        val a = (px ushr 24) and 0xFF
        if (a < INKED_ALPHA_THRESHOLD) continue
        android.graphics.Color.colorToHSV(px, hsv)
        if (hsv[1] < PALETTE_MIN_SATURATION) continue
        val bucket = ((hsv[0] / 360f) * PALETTE_HUE_BUCKETS).toInt()
            .coerceIn(0, PALETTE_HUE_BUCKETS - 1)
        histogram[bucket]++
    }

    val topBuckets = histogram
        .withIndex()
        .sortedByDescending { it.value }
        .take(PALETTE_BUCKETS)
        .filter { it.value > 0 }
        .map { it.index }
    if (topBuckets.size < 2) return input.copy(Bitmap.Config.ARGB_8888, true)

    val centers = topBuckets.map { it * (360f / PALETTE_HUE_BUCKETS) + (180f / PALETTE_HUE_BUCKETS) }

    for (i in pixels.indices) {
        val px = pixels[i]
        val a = (px ushr 24) and 0xFF
        if (a < INKED_ALPHA_THRESHOLD) continue
        android.graphics.Color.colorToHSV(px, hsv)
        if (hsv[1] < PALETTE_MIN_SATURATION) continue
        val nearest = centers.minBy { angularDistance(it, hsv[0]) }
        val signedDelta = signedAngularDelta(hsv[0], nearest)
        hsv[0] = (hsv[0] + signedDelta * PALETTE_NUDGE_FRACTION + 360f) % 360f
        pixels[i] = (a shl 24) or (android.graphics.Color.HSVToColor(hsv) and 0x00FFFFFF)
    }

    val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    output.setPixels(pixels, 0, w, 0, 0, w, h)
    return output
}

private fun angularDistance(a: Float, b: Float): Float {
    val d = abs(a - b) % 360f
    return if (d > 180f) 360f - d else d
}

private fun signedAngularDelta(from: Float, to: Float): Float {
    var d = (to - from + 540f) % 360f - 180f
    if (d == -180f) d = 180f
    return d
}
