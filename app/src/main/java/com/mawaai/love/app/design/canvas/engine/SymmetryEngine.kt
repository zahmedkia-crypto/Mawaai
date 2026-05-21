package com.mawaai.love.app.design.canvas.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.mawaai.love.app.design.canvas.model.SymmetryMode
import kotlin.math.cos
import kotlin.math.sin

object SymmetryEngine {
    /**
     * Returns one or more reflected copies of [points] given the [mode] and the [canvasSize].
     * The original list is always included as the first element.
     */
    fun mirror(points: List<Offset>, mode: SymmetryMode, canvasSize: IntSize): List<List<Offset>> {
        if (mode == SymmetryMode.OFF || points.isEmpty()) return listOf(points)
        val cx = canvasSize.width / 2f
        val cy = canvasSize.height / 2f

        return when (mode) {
            SymmetryMode.OFF -> listOf(points)
            SymmetryMode.VERTICAL -> listOf(points, points.map { Offset(2f * cx - it.x, it.y) })
            SymmetryMode.HORIZONTAL -> listOf(points, points.map { Offset(it.x, 2f * cy - it.y) })
            SymmetryMode.RADIAL_2 -> radial(points, cx, cy, 2)
            SymmetryMode.RADIAL_4 -> radial(points, cx, cy, 4)
            SymmetryMode.RADIAL_6 -> radial(points, cx, cy, 6)
            SymmetryMode.RADIAL_8 -> radial(points, cx, cy, 8)
        }
    }

    private fun radial(points: List<Offset>, cx: Float, cy: Float, fold: Int): List<List<Offset>> {
        val result = ArrayList<List<Offset>>(fold)
        for (i in 0 until fold) {
            val angle = (i.toDouble() / fold) * 2.0 * Math.PI
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()
            val rotated = points.map { p ->
                val dx = p.x - cx
                val dy = p.y - cy
                Offset(cx + dx * cosA - dy * sinA, cy + dx * sinA + dy * cosA)
            }
            result.add(rotated)
        }
        return result
    }
}
