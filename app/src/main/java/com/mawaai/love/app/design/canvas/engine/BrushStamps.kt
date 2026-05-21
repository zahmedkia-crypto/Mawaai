package com.mawaai.love.app.design.canvas.engine

import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset
import com.mawaai.love.app.design.canvas.model.BrushSettings
import com.mawaai.love.app.design.canvas.model.BrushType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Per-[BrushType] stamp implementations. All are [internal] so
 * [BrushEngine] can dispatch to them, but they don't access any
 * [BrushEngine] state — they take a Canvas + Paint + geometry and draw
 * a single stamp footprint.
 */
internal fun drawStampForType(
    canvas: AndroidCanvas,
    paint: Paint,
    brush: BrushSettings,
    p: Offset,
    scale: Float
) {
    val r = (brush.size * scale) / 2f
    if (r <= 0.5f) return

    when (brush.type) {
        BrushType.PENCIL -> drawSoftCircle(canvas, paint, p, r)
        BrushType.INK -> drawSolidCircle(canvas, paint, p, r)
        BrushType.CALLIGRAPHY -> drawCalligraphyStamp(canvas, paint, p, r)
        BrushType.MARKER -> drawSolidCircle(canvas, paint, p, r)
        BrushType.AIRBRUSH -> drawAirbrushStamp(canvas, paint, p, r)
        BrushType.WATERCOLOR -> drawWatercolorStamp(canvas, paint, p, r)
        BrushType.HENNA -> drawHennaPetal(canvas, paint, p, r)
        BrushType.EMBROIDERY -> drawStitchStamp(canvas, paint, p, r)
        BrushType.PATTERN -> drawPatternStar(canvas, paint, p, r)
        BrushType.CHARCOAL -> drawCharcoalStamp(canvas, paint, p, r)
        BrushType.ERASER_SOFT -> drawSoftCircle(canvas, paint, p, r)
        BrushType.ERASER_HARD -> drawSolidCircle(canvas, paint, p, r)
    }
}

private fun drawSoftCircle(canvas: AndroidCanvas, paint: Paint, p: Offset, r: Float) {
    canvas.drawCircle(p.x, p.y, r, paint)
}

private fun drawSolidCircle(canvas: AndroidCanvas, paint: Paint, p: Offset, r: Float) {
    canvas.drawCircle(p.x, p.y, r, paint)
}

private fun drawCalligraphyStamp(canvas: AndroidCanvas, paint: Paint, p: Offset, r: Float) {
    canvas.save()
    canvas.rotate(-30f, p.x, p.y)
    canvas.drawOval(p.x - r * 1.6f, p.y - r * 0.4f, p.x + r * 1.6f, p.y + r * 0.4f, paint)
    canvas.restore()
}

private fun drawAirbrushStamp(canvas: AndroidCanvas, paint: Paint, p: Offset, r: Float) {
    // Reuse a shared unit-origin RadialGradient via a translating local matrix.
    // Allocating a new RadialGradient per stamp was a major GC source.
    val savedShader = paint.shader
    val centerColor = paint.color
    val transparent = (centerColor and 0x00FFFFFF) or 0
    val radiusBucket = (r * 1.2f).toInt().coerceAtLeast(1)
    val shader = AirbrushShaderCache.get(centerColor, transparent, radiusBucket)
    AirbrushShaderCache.applyTranslation(shader, p.x, p.y)
    paint.shader = shader
    canvas.drawCircle(p.x, p.y, radiusBucket.toFloat(), paint)
    paint.shader = savedShader
}

private fun drawWatercolorStamp(canvas: AndroidCanvas, paint: Paint, p: Offset, r: Float) {
    val savedAlpha = paint.alpha
    for (i in 0..2) {
        paint.alpha = (savedAlpha * (1f - i * 0.25f)).toInt().coerceAtLeast(20)
        canvas.drawCircle(p.x, p.y, r * (1f + i * 0.25f), paint)
    }
    paint.alpha = savedAlpha
}

private fun drawHennaPetal(canvas: AndroidCanvas, paint: Paint, p: Offset, r: Float) {
    val path = Path()
    val s = r * 1.4f
    path.moveTo(p.x, p.y - s)
    path.cubicTo(p.x - s * 0.8f, p.y - s * 0.5f, p.x - s * 0.4f, p.y + s * 0.6f, p.x, p.y + s * 0.4f)
    path.cubicTo(p.x + s * 0.4f, p.y + s * 0.6f, p.x + s * 0.8f, p.y - s * 0.5f, p.x, p.y - s)
    path.close()
    canvas.drawPath(path, paint)
}

private fun drawStitchStamp(canvas: AndroidCanvas, paint: Paint, p: Offset, r: Float) {
    val savedStroke = paint.strokeWidth
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = r * 0.4f
    canvas.drawLine(p.x - r, p.y, p.x + r, p.y, paint)
    paint.style = Paint.Style.FILL
    paint.strokeWidth = savedStroke
}

private fun drawPatternStar(canvas: AndroidCanvas, paint: Paint, p: Offset, r: Float) {
    val path = Path()
    val points = 8
    for (i in 0 until points * 2) {
        val isOuter = i % 2 == 0
        val rr = if (isOuter) r else r * 0.45f
        val angle = (i.toDouble() / (points * 2)) * 2.0 * Math.PI - Math.PI / 2.0
        val x = p.x + (rr * cos(angle)).toFloat()
        val y = p.y + (rr * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    canvas.drawPath(path, paint)
}

private fun drawCharcoalStamp(canvas: AndroidCanvas, paint: Paint, p: Offset, r: Float) {
    val savedAlpha = paint.alpha
    for (i in 0..4) {
        val ox = (Random.nextFloat() - 0.5f) * r
        val oy = (Random.nextFloat() - 0.5f) * r
        paint.alpha = (savedAlpha * 0.55f).toInt()
        canvas.drawCircle(p.x + ox, p.y + oy, r * (0.5f + Random.nextFloat() * 0.5f), paint)
    }
    paint.alpha = savedAlpha
}

/**
 * Cached zero-origin [RadialGradient] shaders keyed by (center color, transparent
 * color, radius bucket). Used by the airbrush brush. The shader is translated to
 * the stamp position via [setLocalMatrix] so a single shader serves many stamps.
 */
private object AirbrushShaderCache {
    private const val MAX_ENTRIES = 32
    private data class Key(val center: Int, val transparent: Int, val radius: Int)
    private val cache = object : LinkedHashMap<Key, RadialGradient>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, RadialGradient>?): Boolean =
            size > MAX_ENTRIES
    }
    private val matrix = Matrix()

    @Synchronized
    fun get(center: Int, transparent: Int, radius: Int): RadialGradient {
        val key = Key(center, transparent, radius)
        cache[key]?.let { return it }
        val shader = RadialGradient(
            0f, 0f, radius.toFloat(),
            intArrayOf(center, transparent),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        cache[key] = shader
        return shader
    }

    @Synchronized
    fun applyTranslation(shader: RadialGradient, x: Float, y: Float) {
        matrix.setTranslate(x, y)
        shader.setLocalMatrix(matrix)
    }
}
