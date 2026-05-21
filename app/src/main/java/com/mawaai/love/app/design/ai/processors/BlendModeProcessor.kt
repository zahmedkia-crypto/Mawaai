package com.mawaai.love.app.design.ai.processors

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.mawaai.love.app.core.opencv.OpenCVBootstrap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton

enum class BlendMode { NORMAL, MULTIPLY, OVERLAY, SCREEN, FABRIC_REALISTIC }

@Singleton
class BlendModeProcessor @Inject constructor() {

    /**
     * Composes [overlay] onto [base] using [mode]. Per-pixel blend strength is
     * the product of:
     *  - [overlayAlpha] — uniform scalar in `[0, 1]`.
     *  - the overlay's own alpha channel — the natural cutoff for warped or
     *    pre-segmented artwork. [PerspectiveWarpProcessor] fills out-of-quad
     *    pixels with `alpha = 0`, so the base shows through unmodified there.
     *  - [mask], if provided — additional per-pixel multiplier sourced from
     *    the mask bitmap's alpha channel. Pass `foreground.extractAlpha()`
     *    (optionally `.copy(ARGB_8888)`) to constrain the blend to a
     *    segmented foreground only.
     *
     * The result alpha is always [base]'s alpha — composited bitmaps stay
     * opaque regardless of the overlay's transparency.
     */
    suspend fun blend(
        base: Bitmap,
        overlay: Bitmap,
        mode: BlendMode = BlendMode.NORMAL,
        overlayAlpha: Double = 0.5,
        mask: Bitmap? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        if (!OpenCVBootstrap.ensureLoaded()) {
            return@withContext blendWithAndroidCanvas(base, overlay, mode, overlayAlpha, mask)
        }

        matScope {
            val baseMat = take(Mat())
            val overlayMat = take(Mat())
            Utils.bitmapToMat(base, baseMat)
            Utils.bitmapToMat(overlay, overlayMat)
            if (overlayMat.size() != baseMat.size()) {
                Imgproc.resize(overlayMat, overlayMat, baseMat.size())
            }

            val baseF = take(Mat())
            val ovF = take(Mat())
            baseMat.convertTo(baseF, CvType.CV_32FC4, 1.0 / 255.0)
            overlayMat.convertTo(ovF, CvType.CV_32FC4, 1.0 / 255.0)

            val rawBlend = computeRawBlend(mode, baseF, ovF)
            val effectiveMask = buildEffectiveMask(baseMat.size(), overlayAlpha, ovF, mask)
            val mixedRgb = mixRgbWithMask(baseF, rawBlend, effectiveMask)
            val withBaseAlpha = restoreBaseAlpha(mixedRgb, baseF)

            val out = take(Mat())
            withBaseAlpha.convertTo(out, CvType.CV_8UC4, 255.0)

            val result = Bitmap.createBitmap(out.cols(), out.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(out, result)
            result
        }
    }

    /**
     * Pure-Android fallback used when [OpenCVBootstrap] reports the native
     * library is unavailable. Resizes the overlay, multiplies an optional
     * [mask] into its alpha channel via `DST_IN`, then composites using the
     * closest [PorterDuff.Mode] analogue of each [BlendMode]. Visually close
     * to the OpenCV path — no crash, no missing layer.
     */
    private fun blendWithAndroidCanvas(
        base: Bitmap,
        overlay: Bitmap,
        mode: BlendMode,
        overlayAlpha: Double,
        mask: Bitmap?
    ): Bitmap {
        val result = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val scaledOverlay = if (overlay.width == base.width && overlay.height == base.height) {
            overlay
        } else {
            Bitmap.createScaledBitmap(overlay, base.width, base.height, true)
        }

        val maskedOverlay = if (mask != null) applyAlphaMask(scaledOverlay, mask) else scaledOverlay

        val porterDuffMode = when (mode) {
            BlendMode.NORMAL -> PorterDuff.Mode.SRC_OVER
            BlendMode.MULTIPLY -> PorterDuff.Mode.MULTIPLY
            BlendMode.OVERLAY -> PorterDuff.Mode.OVERLAY
            BlendMode.SCREEN -> PorterDuff.Mode.SCREEN
            // PorterDuff has no FABRIC_REALISTIC analogue; OVERLAY is the
            // closest perceptual match (light areas brighten, dark areas
            // darken). The OpenCV path renders the real diffuse/specular
            // formula — only the fallback approximates.
            BlendMode.FABRIC_REALISTIC -> PorterDuff.Mode.OVERLAY
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (overlayAlpha.coerceIn(0.0, 1.0) * 255).toInt()
            xfermode = PorterDuffXfermode(porterDuffMode)
            isFilterBitmap = true
        }
        canvas.drawBitmap(maskedOverlay, 0f, 0f, paint)

        if (maskedOverlay !== scaledOverlay) maskedOverlay.recycle()
        if (scaledOverlay !== overlay) scaledOverlay.recycle()
        return result
    }

    /**
     * Returns a new [Bitmap] where [overlay]'s alpha has been multiplied by
     * [mask]'s alpha pixel-by-pixel (via `DST_IN`). Caller owns the result.
     */
    private fun applyAlphaMask(overlay: Bitmap, mask: Bitmap): Bitmap {
        val w = overlay.width
        val h = overlay.height
        val scaledMask = if (mask.width == w && mask.height == h) {
            mask
        } else {
            Bitmap.createScaledBitmap(mask, w, h, true)
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(overlay, 0f, 0f, null)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            isFilterBitmap = true
        }
        canvas.drawBitmap(scaledMask, 0f, 0f, maskPaint)
        if (scaledMask !== mask) scaledMask.recycle()
        return result
    }
}

// ───────────────────────────────────────────────────────────────────────────
// File-private MatScope helpers. Keeping them out of the class avoids the
// member-extension-receiver ambiguity and lets the OpenCV math read top-to-
// bottom inside blend(). MatScope is `internal`; these are `private` so the
// visibility narrows correctly.
// ───────────────────────────────────────────────────────────────────────────

private fun MatScope.computeRawBlend(mode: BlendMode, baseF: Mat, ovF: Mat): Mat {
    val raw = take(Mat())
    when (mode) {
        BlendMode.NORMAL -> ovF.copyTo(raw)
        BlendMode.MULTIPLY -> Core.multiply(baseF, ovF, raw)
        BlendMode.SCREEN -> computeScreen(baseF, ovF, raw)
        BlendMode.OVERLAY -> computeOverlay(baseF, ovF, raw)
        BlendMode.FABRIC_REALISTIC -> computeFabricRealistic(baseF, ovF, raw)
    }
    return raw
}

private fun MatScope.computeScreen(baseF: Mat, ovF: Mat, dst: Mat) {
    // 1 - (1 - base) * (1 - overlay), per channel.
    val baseInv = take(Mat())
    val ovInv = take(Mat())
    complement(baseF, baseInv)
    complement(ovF, ovInv)
    val product = take(Mat())
    Core.multiply(baseInv, ovInv, product)
    complement(product, dst)
}

private fun MatScope.computeOverlay(baseF: Mat, ovF: Mat, dst: Mat) {
    // Per-channel Photoshop OVERLAY:
    //   base < 0.5 → 2 * base * overlay
    //   base ≥ 0.5 → 1 - 2 * (1 - base) * (1 - overlay)
    //
    // Both branches are computed for every pixel; `Core.compare` produces a
    // multi-channel CV_8U mask (one byte per channel) and `Mat.copyTo` with
    // that mask splices per-channel, so the branch selection is exact per
    // R/G/B/A regardless of the others.
    //
    // ⚠ Note (audit fix #10, 2026-05-13): Photoshop's canonical OVERLAY
    // selects the branch via the base's *luminance*, not per-channel.
    // The per-channel split here can produce slightly different (still
    // plausible) results on saturated colored bases — typically a hair
    // more contrast where the channels straddle 0.5. For mostly-gray
    // bases the two are visually identical. The per-channel form was
    // chosen for performance: a luminance-driven select needs an extra
    // 0.299R+0.587G+0.114B reduction + a 4-channel splat of the result.
    // If a user-visible mismatch surfaces, swap to the luma path here
    // and update the FABRIC_REALISTIC luminance computation to share a
    // helper.
    val multiplyBranch = take(Mat())
    Core.multiply(baseF, ovF, multiplyBranch, 2.0)

    val baseInv = take(Mat())
    complement(baseF, baseInv)
    val ovInv = take(Mat())
    complement(ovF, ovInv)
    val invProduct = take(Mat())
    Core.multiply(baseInv, ovInv, invProduct, 2.0)
    val screenBranch = take(Mat())
    complement(invProduct, screenBranch)

    val ltHalf = take(Mat())
    Core.compare(baseF, Scalar.all(0.5), ltHalf, Core.CMP_LT)

    screenBranch.copyTo(dst)
    multiplyBranch.copyTo(dst, ltHalf)
}

private fun MatScope.computeFabricRealistic(baseF: Mat, ovF: Mat, dst: Mat) {
    // Diffuse: base * overlay (the pattern dyes the fabric).
    // Specular: base unchanged where the fabric is bright (the highlight
    // overwhelms the pattern on real cloth folds / silk sheen).
    // Mask: smoothstep(SPECULAR_LOW, SPECULAR_HIGH, baseLuminance).
    //
    // Combined into one multiply, identical to
    //   diffuse * (1 - spec) + base * spec:
    //     base * (overlay * (1 - spec) + spec)
    val channels = ArrayList<Mat>()
    Core.split(baseF, channels)
    channels.forEach { take(it) }

    val lumR = take(Mat())
    val lumG = take(Mat())
    val lumB = take(Mat())
    Core.multiply(channels[0], Scalar.all(0.299), lumR)
    Core.multiply(channels[1], Scalar.all(0.587), lumG)
    Core.multiply(channels[2], Scalar.all(0.114), lumB)
    val lum = take(Mat())
    Core.add(lumR, lumG, lum)
    Core.add(lum, lumB, lum)

    val spec1 = take(Mat())
    Core.subtract(lum, Scalar.all(SPECULAR_LOW), spec1)
    Core.multiply(spec1, Scalar.all(1.0 / (SPECULAR_HIGH - SPECULAR_LOW)), spec1)
    Core.max(spec1, Scalar.all(0.0), spec1)
    Core.min(spec1, Scalar.all(1.0), spec1)

    val spec4 = take(Mat())
    Core.merge(listOf(spec1, spec1, spec1, spec1), spec4)
    val invSpec = take(Mat())
    complement(spec4, invSpec)

    val modifier = take(Mat())
    Core.multiply(ovF, invSpec, modifier)
    Core.add(modifier, spec4, modifier)

    Core.multiply(baseF, modifier, dst)
}

private fun MatScope.buildEffectiveMask(
    targetSize: Size,
    overlayAlpha: Double,
    ovF: Mat,
    explicitMask: Bitmap?
): Mat {
    // Start from overlay's alpha channel scaled by the uniform overlayAlpha.
    val ovChannels = ArrayList<Mat>()
    Core.split(ovF, ovChannels)
    ovChannels.forEach { take(it) }
    val mask1 = take(Mat())
    ovChannels[3].copyTo(mask1)
    val clamped = overlayAlpha.coerceIn(0.0, 1.0)
    Core.multiply(mask1, Scalar.all(clamped), mask1)

    if (explicitMask != null) {
        // OpenCV's bitmapToMat requires ARGB_8888 (or RGB_565). Convert if
        // needed; the conversion preserves an ALPHA_8 mask in the alpha
        // channel of the new ARGB_8888 bitmap.
        val maskMatBitmap = if (explicitMask.config == Bitmap.Config.ARGB_8888) {
            explicitMask
        } else {
            explicitMask.copy(Bitmap.Config.ARGB_8888, false)
        }
        val maskMat = take(Mat())
        Utils.bitmapToMat(maskMatBitmap, maskMat)
        if (maskMatBitmap !== explicitMask) maskMatBitmap.recycle()
        if (maskMat.size() != targetSize) Imgproc.resize(maskMat, maskMat, targetSize)
        val maskChannels = ArrayList<Mat>()
        Core.split(maskMat, maskChannels)
        maskChannels.forEach { take(it) }
        // The mask is expected to encode confidence in the alpha channel
        // (the natural output of Bitmap.extractAlpha + ARGB_8888 copy).
        val maskValueF = take(Mat())
        maskChannels[3].convertTo(maskValueF, CvType.CV_32F, 1.0 / 255.0)
        Core.multiply(mask1, maskValueF, mask1)
    }

    val mask4 = take(Mat())
    Core.merge(listOf(mask1, mask1, mask1, mask1), mask4)
    return mask4
}

private fun MatScope.mixRgbWithMask(baseF: Mat, rawBlend: Mat, mask4: Mat): Mat {
    // result = base * (1 - mask) + rawBlend * mask
    val invMask = take(Mat())
    complement(mask4, invMask)
    val basePart = take(Mat())
    Core.multiply(baseF, invMask, basePart)
    val blendPart = take(Mat())
    Core.multiply(rawBlend, mask4, blendPart)
    val out = take(Mat())
    Core.add(basePart, blendPart, out)
    return out
}

private fun MatScope.restoreBaseAlpha(mixedRgb: Mat, baseF: Mat): Mat {
    // Keep R/G/B from mixedRgb but copy A from baseF — the composited
    // bitmap should stay opaque even when the overlay was transparent.
    val mixedChannels = ArrayList<Mat>()
    Core.split(mixedRgb, mixedChannels)
    mixedChannels.forEach { take(it) }
    val baseChannels = ArrayList<Mat>()
    Core.split(baseF, baseChannels)
    baseChannels.forEach { take(it) }
    val out = take(Mat())
    Core.merge(
        listOf(mixedChannels[0], mixedChannels[1], mixedChannels[2], baseChannels[3]),
        out
    )
    return out
}

// Smoothstep range for the fabric specular component. Below SPECULAR_LOW
// the overlay multiplies into the base (the pattern dyes the fabric).
// Above SPECULAR_HIGH the base highlight is preserved unchanged so the
// pattern fades on bright folds.
private const val SPECULAR_LOW = 0.55
private const val SPECULAR_HIGH = 0.85
// `complement(src, dst)` lives in MatScope.kt — shared with
// GarmentColorEngine after the 2026-05-13 audit found duplicate copies.
