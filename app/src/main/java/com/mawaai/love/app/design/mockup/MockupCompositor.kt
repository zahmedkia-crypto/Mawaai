package com.mawaai.love.app.design.mockup

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import com.mawaai.love.app.data.database.entities.ProductMockupEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MT-034: places a freshly rendered design [Bitmap] onto a presentation scene
 * (the [ProductMockupEntity]) for sharing / export.
 *
 * Native Compose / Canvas implementation -- no OpenCV required for v1. The
 * compositor reads the mockup's [ProductMockupEntity.accentColor] /
 * [ProductMockupEntity.lighting] / [ProductMockupEntity.perspective] hints
 * and builds a soft gradient backdrop + centred design overlay that fits the
 * existing `MUL TIPLY @ 0.78-0.82` blend pattern documented in
 * `assets/templates/henna/templates.json` (`_surface_defaults`).
 *
 * Output is a square 1024x1024 Bitmap suitable for direct hand-off to the
 * MediaStore export pipeline. Higher-fidelity quad-warp + ControlNet
 * relighting (Lovable's Phase 8 compositor) is intentionally out of scope
 * here; this is the on-device v1 that ships without OpenCV.
 */
@Singleton
class MockupCompositor @Inject constructor() {

    /**
     * Compose [design] onto [mockup]'s scene.
     *
     * @return A new 1024x1024 RGB Bitmap. The caller owns the bitmap and is
     * responsible for recycling it once exported.
     */
    suspend fun compose(design: Bitmap, mockup: ProductMockupEntity): Bitmap =
        withContext(Dispatchers.IO) {
            val out = Bitmap.createBitmap(OUTPUT_SIZE, OUTPUT_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)

            paintBackdrop(canvas, mockup)
            paintVignette(canvas)
            paintDesign(canvas, design, mockup)
            paintFooter(canvas, mockup)

            out
        }

    private fun paintBackdrop(canvas: Canvas, mockup: ProductMockupEntity) {
        val accent = parseHexOrDefault(mockup.accentColor, defaultAccent = ACCENT_FALLBACK)
        val accentLight = lighten(accent, AMOUNT = 0.28f)
        val accentDark = darken(accent, AMOUNT = 0.55f)

        // Vertical gradient: warm top, dark bottom -- mirrors a window-lit studio.
        val gradient = LinearGradient(
            /* x0 = */ 0f,
            /* y0 = */ 0f,
            /* x1 = */ 0f,
            /* y1 = */ OUTPUT_SIZE.toFloat(),
            /* color0 = */ accentLight,
            /* color1 = */ accentDark,
            Shader.TileMode.CLAMP,
        )
        val backdropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = gradient }
        canvas.drawRect(0f, 0f, OUTPUT_SIZE.toFloat(), OUTPUT_SIZE.toFloat(), backdropPaint)
    }

    private fun paintVignette(canvas: Canvas) {
        // Soft inner shadow round the edges so the design floats on the page.
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(VIGNETTE_ALPHA, 0, 0, 0)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }
        val inset = OUTPUT_SIZE / VIGNETTE_INSET_DIVISOR.toFloat()
        canvas.drawRect(0f, 0f, OUTPUT_SIZE.toFloat(), inset, vignettePaint)
        canvas.drawRect(0f, OUTPUT_SIZE - inset, OUTPUT_SIZE.toFloat(), OUTPUT_SIZE.toFloat(), vignettePaint)
        canvas.drawRect(0f, 0f, inset, OUTPUT_SIZE.toFloat(), vignettePaint)
        canvas.drawRect(OUTPUT_SIZE - inset, 0f, OUTPUT_SIZE.toFloat(), OUTPUT_SIZE.toFloat(), vignettePaint)
    }

    private fun paintDesign(canvas: Canvas, design: Bitmap, mockup: ProductMockupEntity) {
        // Aspect-fit the design into the central 80% of the canvas.
        val targetSize = (OUTPUT_SIZE * DESIGN_FIT_RATIO).toInt()
        val designAspect = design.width.toFloat() / design.height.toFloat()
        val dstWidth: Int
        val dstHeight: Int
        if (designAspect >= 1f) {
            dstWidth = targetSize
            dstHeight = (targetSize / designAspect).toInt()
        } else {
            dstWidth = (targetSize * designAspect).toInt()
            dstHeight = targetSize
        }
        val dstLeft = (OUTPUT_SIZE - dstWidth) / 2
        val dstTop = (OUTPUT_SIZE - dstHeight) / 2
        val dstRect = Rect(dstLeft, dstTop, dstLeft + dstWidth, dstTop + dstHeight)
        val srcRect = Rect(0, 0, design.width, design.height)

        // Soft drop-shadow under the design so it lifts off the backdrop.
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(SHADOW_ALPHA, 0, 0, 0)
        }
        val shadowRect = RectF(dstRect).apply { offset(0f, SHADOW_OFFSET) }
        canvas.drawRoundRect(shadowRect, SHADOW_CORNER_RADIUS, SHADOW_CORNER_RADIUS, shadowPaint)

        // Composite the design with NORMAL src-over for now. Henna scenes can
        // upgrade to PorterDuff.Mode.MULTIPLY in a follow-up commit once we
        // pass a per-mockup blend hint through ProductMockupEntity.
        val designPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
        if (mockup.category == "HENNA") {
            designPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        }
        canvas.drawBitmap(design, srcRect, dstRect, designPaint)
    }

    private fun paintFooter(canvas: Canvas, mockup: ProductMockupEntity) {
        // Light text token for sharing -- a single attribution line at the
        // bottom centre. Kept minimal so user-generated designs stay the
        // visual focus.
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(FOOTER_ALPHA, 0xFF, 0xFF, 0xFF)
            textSize = FOOTER_TEXT_SIZE
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val label = "Mawaai \u00b7 ${mockup.name}"
        canvas.drawText(label, OUTPUT_SIZE / 2f, OUTPUT_SIZE - FOOTER_MARGIN, footerPaint)
    }

    private fun parseHexOrDefault(hex: String?, defaultAccent: Int): Int =
        try {
            if (hex.isNullOrBlank()) defaultAccent else Color.parseColor(hex)
        } catch (_: IllegalArgumentException) {
            defaultAccent
        }

    private fun lighten(@Suppress("UNUSED_PARAMETER") color: Int, AMOUNT: Float): Int {
        val r = ((Color.red(color) + (255 - Color.red(color)) * AMOUNT)).toInt().coerceIn(0, 255)
        val g = ((Color.green(color) + (255 - Color.green(color)) * AMOUNT)).toInt().coerceIn(0, 255)
        val b = ((Color.blue(color) + (255 - Color.blue(color)) * AMOUNT)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun darken(color: Int, AMOUNT: Float): Int {
        val r = (Color.red(color) * (1f - AMOUNT)).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * (1f - AMOUNT)).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * (1f - AMOUNT)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private companion object {
        const val OUTPUT_SIZE = 1024
        const val DESIGN_FIT_RATIO = 0.80f
        const val VIGNETTE_ALPHA = 40
        const val VIGNETTE_INSET_DIVISOR = 16
        const val SHADOW_ALPHA = 60
        const val SHADOW_OFFSET = 6f
        const val SHADOW_CORNER_RADIUS = 24f
        const val FOOTER_ALPHA = 200
        const val FOOTER_TEXT_SIZE = 28f
        const val FOOTER_MARGIN = 36f
        val ACCENT_FALLBACK: Int = Color.rgb(0xB8, 0x6B, 0x3A) // bridal henna brown
    }
}
