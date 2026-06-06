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
import com.mawaai.love.app.design.ai.intelligence.SurfaceCatalog
import com.mawaai.love.app.design.ai.intelligence.SurfaceProfile
import com.mawaai.love.app.design.rendering.RelightingEngine
import com.mawaai.love.app.design.rendering.SurfaceWarpEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Places a freshly rendered design [Bitmap] onto a presentation scene for
 * sharing / export. The compositor now runs the design through surface-aware
 * warp + relighting before it is drawn, so mockups move away from a flat pasted
 * overlay and toward believable product photography.
 */
@Singleton
class MockupCompositor @Inject constructor(
    private val warpEngine: SurfaceWarpEngine,
    private val relightingEngine: RelightingEngine,
) {

    /**
     * Compose [design] onto [mockup]'s scene.
     *
     * [outputSize] defaults to 2048 for premium exports while keeping the old
     * call sites source-compatible. Pass 1024 from low-memory previews if needed.
     */
    suspend fun compose(
        design: Bitmap,
        mockup: ProductMockupEntity,
        outputSize: Int = OUTPUT_SIZE,
    ): Bitmap = withContext(Dispatchers.IO) {
        val profile = resolveProfile(mockup)
        val out = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        paintBackdrop(canvas, mockup, outputSize)
        paintVignette(canvas, outputSize)
        paintDesign(canvas, design, mockup, profile, outputSize)

        out
    }

    private fun resolveProfile(mockup: ProductMockupEntity): SurfaceProfile {
        mockup.surfaceMatchCsv
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .firstNotNullOfOrNull { SurfaceCatalog.byId(it) }
            ?.let { return it }

        return when (mockup.category.uppercase()) {
            "HENNA" -> SurfaceProfile.SkinHandFull
            "GARMENT", "ABAYA" -> SurfaceProfile.FabricAbaya
            "WALL" -> SurfaceProfile.WallPlaster
            "CERAMIC" -> SurfaceProfile.CeramicTile
            else -> SurfaceProfile.CeramicTile
        }
    }

    private fun paintBackdrop(canvas: Canvas, mockup: ProductMockupEntity, outputSize: Int) {
        val accent = parseHexOrDefault(mockup.accentColor, defaultAccent = ACCENT_FALLBACK)
        val accentLight = lighten(accent, amount = 0.28f)
        val accentDark = darken(accent, amount = 0.55f)
        val gradient = LinearGradient(
            0f,
            0f,
            0f,
            outputSize.toFloat(),
            accentLight,
            accentDark,
            Shader.TileMode.CLAMP,
        )
        val backdropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = gradient }
        canvas.drawRect(0f, 0f, outputSize.toFloat(), outputSize.toFloat(), backdropPaint)
    }

    private fun paintVignette(canvas: Canvas, outputSize: Int) {
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(VIGNETTE_ALPHA, 0, 0, 0)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }
        val inset = outputSize / VIGNETTE_INSET_DIVISOR.toFloat()
        canvas.drawRect(0f, 0f, outputSize.toFloat(), inset, vignettePaint)
        canvas.drawRect(0f, outputSize - inset, outputSize.toFloat(), outputSize.toFloat(), vignettePaint)
        canvas.drawRect(0f, 0f, inset, outputSize.toFloat(), vignettePaint)
        canvas.drawRect(outputSize - inset, 0f, outputSize.toFloat(), outputSize.toFloat(), vignettePaint)
    }

    private fun paintDesign(
        canvas: Canvas,
        design: Bitmap,
        mockup: ProductMockupEntity,
        profile: SurfaceProfile,
        outputSize: Int,
    ) {
        val warped = warpEngine.warp(design, profile)
        val lit = relightingEngine.relight(warped, profile)

        val targetSize = (outputSize * DESIGN_FIT_RATIO).toInt()
        val designAspect = lit.width.toFloat() / lit.height.toFloat()
        val dstWidth: Int
        val dstHeight: Int
        if (designAspect >= 1f) {
            dstWidth = targetSize
            dstHeight = (targetSize / designAspect).toInt()
        } else {
            dstWidth = (targetSize * designAspect).toInt()
            dstHeight = targetSize
        }
        val dstLeft = (outputSize - dstWidth) / 2
        val dstTop = (outputSize - dstHeight) / 2
        val dstRect = Rect(dstLeft, dstTop, dstLeft + dstWidth, dstTop + dstHeight)
        val srcRect = Rect(0, 0, lit.width, lit.height)

        paintContactShadow(canvas, dstRect)

        val designPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
            if (mockup.category.equals("HENNA", ignoreCase = true)) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            }
        }
        canvas.drawBitmap(lit, srcRect, dstRect, designPaint)

        if (warped !== design && !warped.isRecycled) warped.recycle()
        if (!lit.isRecycled) lit.recycle()
    }

    private fun paintContactShadow(canvas: Canvas, dstRect: Rect) {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(SHADOW_ALPHA, 0, 0, 0)
        }
        val shadowRect = RectF(dstRect).apply { offset(0f, SHADOW_OFFSET) }
        canvas.drawRoundRect(shadowRect, SHADOW_CORNER_RADIUS, SHADOW_CORNER_RADIUS, shadowPaint)
    }

    private fun parseHexOrDefault(hex: String?, defaultAccent: Int): Int =
        try {
            if (hex.isNullOrBlank()) defaultAccent else Color.parseColor(hex)
        } catch (_: IllegalArgumentException) {
            defaultAccent
        }

    private fun lighten(color: Int, amount: Float): Int {
        val r = ((Color.red(color) + (255 - Color.red(color)) * amount)).toInt().coerceIn(0, 255)
        val g = ((Color.green(color) + (255 - Color.green(color)) * amount)).toInt().coerceIn(0, 255)
        val b = ((Color.blue(color) + (255 - Color.blue(color)) * amount)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun darken(color: Int, amount: Float): Int {
        val r = (Color.red(color) * (1f - amount)).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * (1f - amount)).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * (1f - amount)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private companion object {
        const val OUTPUT_SIZE = 2048
        const val DESIGN_FIT_RATIO = 0.82f
        const val VIGNETTE_ALPHA = 36
        const val VIGNETTE_INSET_DIVISOR = 18
        const val SHADOW_ALPHA = 72
        const val SHADOW_OFFSET = 10f
        const val SHADOW_CORNER_RADIUS = 28f
        val ACCENT_FALLBACK: Int = Color.rgb(0xB8, 0x6B, 0x3A)
    }
}