package com.mawaai.love.app.design.canvas.engine

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import com.mawaai.love.app.design.canvas.model.ShapeSettings
import com.mawaai.love.app.design.canvas.model.ShapeType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object ShapeEngine {
    fun render(bitmap: Bitmap, settings: ShapeSettings, start: Offset, end: Offset) {
        val canvas = AndroidCanvas(bitmap)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = settings.strokeWidth
            color = settings.strokeColor.toArgb()
        }
        val fillPaint = settings.fillColor?.let {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = it.toArgb()
            }
        }

        when (settings.shape) {
            ShapeType.LINE -> {
                canvas.drawLine(start.x, start.y, end.x, end.y, strokePaint)
            }
            ShapeType.RECT -> {
                val left = minOf(start.x, end.x)
                val top = minOf(start.y, end.y)
                val right = maxOf(start.x, end.x)
                val bottom = maxOf(start.y, end.y)
                fillPaint?.let { canvas.drawRect(left, top, right, bottom, it) }
                canvas.drawRect(left, top, right, bottom, strokePaint)
            }
            ShapeType.CIRCLE -> {
                val cx = (start.x + end.x) / 2f
                val cy = (start.y + end.y) / 2f
                val rx = kotlin.math.abs(end.x - start.x) / 2f
                val ry = kotlin.math.abs(end.y - start.y) / 2f
                fillPaint?.let { canvas.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, it) }
                canvas.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, strokePaint)
            }
            ShapeType.POLYGON -> {
                val cx = start.x
                val cy = start.y
                val r = hypot(end.x - start.x, end.y - start.y)
                val sides = settings.polygonSides.coerceIn(3, 12)
                val baseAngle = atan2(end.y - start.y, end.x - start.x)
                val path = Path()
                for (i in 0 until sides) {
                    val a = baseAngle + (i.toDouble() / sides) * 2.0 * Math.PI
                    val x = cx + (r * cos(a)).toFloat()
                    val y = cy + (r * sin(a)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                fillPaint?.let { canvas.drawPath(path, it) }
                canvas.drawPath(path, strokePaint)
            }
            ShapeType.STAR -> {
                val cx = start.x
                val cy = start.y
                val r = hypot(end.x - start.x, end.y - start.y)
                val rInner = r * 0.45f
                val points = settings.starPoints.coerceIn(3, 12)
                val baseAngle = atan2(end.y - start.y, end.x - start.x)
                val path = Path()
                for (i in 0 until points * 2) {
                    val rr = if (i % 2 == 0) r else rInner
                    val a = baseAngle + (i.toDouble() / (points * 2)) * 2.0 * Math.PI
                    val x = cx + (rr * cos(a)).toFloat()
                    val y = cy + (rr * sin(a)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                fillPaint?.let { canvas.drawPath(path, it) }
                canvas.drawPath(path, strokePaint)
            }
        }
    }
}
