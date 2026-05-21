package com.mawaai.love.app.design.canvas.engine

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot
import kotlin.math.max

/**
 * Resamples a raw polyline of touch points into a smooth list of densely-spaced
 * samples along quadratic Bézier segments using the classic "midpoint trick":
 * for each triple (p0, p1, p2), the smoothed curve goes from mid(p0,p1) to
 * mid(p1,p2) with p1 as the control point. The result is an evenly spaced
 * polyline that hides input jitter and matches the per-stamp spacing the
 * BrushEngine expects.
 */
internal object PathSmoother {
    /**
     * @param raw incoming touch samples
     * @param sampleStep desired distance (px) between output points (default 1f).
     */
    fun smooth(raw: List<Offset>, sampleStep: Float = 1f): List<Offset> {
        if (raw.size <= 2) return raw
        val step = max(0.5f, sampleStep)
        val out = ArrayList<Offset>(raw.size * 2)
        out.add(raw[0])
        var i = 1
        while (i < raw.size - 1) {
            val p0 = raw[i - 1]
            val p1 = raw[i]
            val p2 = raw[i + 1]
            val m0 = Offset((p0.x + p1.x) * 0.5f, (p0.y + p1.y) * 0.5f)
            val m1 = Offset((p1.x + p2.x) * 0.5f, (p1.y + p2.y) * 0.5f)
            // Adaptive subdivision count based on the segment length.
            val approxLen = hypot((m1.x - m0.x), (m1.y - m0.y)) +
                hypot((p1.x - m0.x), (p1.y - m0.y)) +
                hypot((m1.x - p1.x), (m1.y - p1.y))
            val steps = (approxLen / step).toInt().coerceIn(2, 64)
            var t = 1f / steps
            while (t <= 1f) {
                val inv = 1f - t
                val bx = inv * inv * m0.x + 2f * inv * t * p1.x + t * t * m1.x
                val by = inv * inv * m0.y + 2f * inv * t * p1.y + t * t * m1.y
                out.add(Offset(bx, by))
                t += 1f / steps
            }
            i++
        }
        out.add(raw.last())
        return out
    }
}
