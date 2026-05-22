package com.mawaai.love.app.design.ai.preservation

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sin

@Singleton
class MaterialRenderer @Inject constructor() {

    suspend fun render(
        sketch: ImprovedSketch,
        categoryId: String,
        material: MaterialTarget = materialFor(categoryId)
    ): RenderedDesign = withContext(Dispatchers.Default) {
        val source = sketch.bitmap
        val thread = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(thread)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(72, 0, 0, 0)
            maskFilter = BlurMaskFilter(1.4f, BlurMaskFilter.Blur.NORMAL)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(88, 255, 246, 210)
        }

        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val alpha = Color.alpha(pixels[y * source.width + x])
                if (alpha <= 8) continue
                val stitchPulse = 0.82f + 0.18f * sin((x + y) / 5.0).toFloat()
                bodyPaint.color = threadColor(material, (alpha * stitchPulse).toInt().coerceIn(0, 255))
                canvas.drawPoint(x + 1f, y + 1f, shadowPaint)
                canvas.drawPoint(x.toFloat(), y.toFloat(), bodyPaint)
                if ((x + y) % 7 == 0) canvas.drawPoint(x - 1f, y - 1f, highlightPaint)
            }
        }
        RenderedDesign(thread, material)
    }

    private fun threadColor(material: MaterialTarget, alpha: Int): Int =
        when (material) {
            MaterialTarget.EMBROIDERY_GOLD -> Color.argb(alpha, 212, 168, 70)
            MaterialTarget.EMBROIDERY_DARK -> Color.argb(alpha, 32, 28, 24)
        }

    private fun materialFor(categoryId: String): MaterialTarget =
        when (categoryId) {
            "abaya", "thob_sudani" -> MaterialTarget.EMBROIDERY_GOLD
            else -> MaterialTarget.EMBROIDERY_DARK
        }
}
