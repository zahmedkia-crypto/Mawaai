package com.mawaai.love.app.design.canvas.engine

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import com.mawaai.love.app.design.canvas.model.BrushSettings
import com.mawaai.love.app.design.canvas.model.SymmetryMode
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Renders brush strokes to an Android [Bitmap] using stamp-based painting.
 *
 * The stateful API ([beginStroke] / [extendStroke] / [endStroke]) maintains
 * a spacing accumulator across gesture events so stamps continue evenly
 * along a quadratic-Bezier smoothed path (no banding, missed stamps, or
 * kinks at event boundaries).
 *
 * Per-[BrushType][com.mawaai.love.app.design.canvas.model.BrushType] stamp
 * rendering lives in [BrushStamps] — pure Canvas/Paint draws with no class
 * state.
 */
class BrushEngine {

    private class StrokeState(
        val brush: BrushSettings,
        val eraseMode: Boolean,
        val symmetry: SymmetryMode,
        val canvasSize: IntSize,
        val paint: Paint,
        val random: Random
    ) {
        val points: ArrayList<Offset> = ArrayList()
        var lastAnchor: Offset? = null
        var carry: Float = 0f
    }

    private var stroke: StrokeState? = null

    fun beginStroke(
        bitmap: Bitmap,
        brush: BrushSettings,
        initialPoint: Offset,
        symmetry: SymmetryMode,
        canvasSize: IntSize,
        eraseMode: Boolean = false,
        seed: Long = 0L
    ) {
        val paint = makePaint(brush, eraseMode)
        val state = StrokeState(brush, eraseMode, symmetry, canvasSize, paint, Random(seed))
        stroke = state
        state.points.add(initialPoint)
        state.lastAnchor = initialPoint
        val canvas = AndroidCanvas(bitmap)
        stampWithSymmetry(canvas, state, initialPoint)
    }

    /**
     * Add one source-arm point to the active stroke and stamp the new
     * finalized sub-segment using quadratic-Bezier midpoint smoothing.
     *
     * Protocol:
     *  - n=1: [beginStroke] has already placed the initial stamp at p0 and
     *    seeded [StrokeState.lastAnchor] to p0.
     *  - n=2: defer drawing. A single Bezier needs three points; emitting a
     *    straight head segment here would leave a visible curvature jump at
     *    mid(p0, p1). [endStroke] handles the tap-drag (only-two-points) case
     *    by stamping a straight p0->p1 line.
     *  - n>=3: quadratic Bezier from [StrokeState.lastAnchor] through
     *    p_{n-2} (control) to midpoint(p_{n-2}, p_{n-1}). On the first call
     *    that draws (n=3) [StrokeState.lastAnchor] is still p0, so the first
     *    Bezier sweeps the entire p0..mid(p1,p2) span as a curve rather than
     *    a half-line followed by a curve.
     */
    fun extendStroke(bitmap: Bitmap, newPoint: Offset) {
        val state = stroke ?: return
        val pts = state.points
        if (pts.isNotEmpty() && pts.last() == newPoint) return
        pts.add(newPoint)
        val n = pts.size
        if (n < 3) return
        val pPrev1 = pts[n - 2]
        val pCur = pts[n - 1]
        val from = state.lastAnchor ?: pts[0]
        val to = midpoint(pPrev1, pCur)
        stampAlongQuadratic(AndroidCanvas(bitmap), state, from = from, control = pPrev1, to = to)
        state.lastAnchor = to
    }

    /**
     * Finalize the stroke: stamps the trailing line from the last anchor to
     * the last raw point so the user-visible stroke matches the finger's last
     * position. Called at the gesture's pointer-up.
     */
    fun endStroke(bitmap: Bitmap?) {
        val state = stroke ?: return
        val pts = state.points
        if (bitmap != null && pts.size >= 2) {
            val from = state.lastAnchor ?: pts[0]
            val to = pts.last()
            if (from != to) {
                stampAlongLine(AndroidCanvas(bitmap), state, from, to)
            }
        }
        stroke = null
    }

    private fun stampAlongLine(
        canvas: AndroidCanvas,
        state: StrokeState,
        from: Offset,
        to: Offset
    ) {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val dist = hypot(dx, dy)
        if (dist <= 0f) return
        val spacingPx = (state.brush.size * state.brush.spacing).coerceAtLeast(0.5f)
        val nx = dx / dist
        val ny = dy / dist
        var travelled = -state.carry
        while (travelled + spacingPx <= dist) {
            travelled += spacingPx
            val sample = Offset(from.x + nx * travelled, from.y + ny * travelled)
            stampWithSymmetry(canvas, state, sample)
        }
        state.carry = dist - travelled
    }

    private fun stampAlongQuadratic(
        canvas: AndroidCanvas,
        state: StrokeState,
        from: Offset,
        control: Offset,
        to: Offset
    ) {
        val brush = state.brush
        val spacingPx = (brush.size * brush.spacing).coerceAtLeast(0.5f)
        val steps = 16
        // Sample the Bezier into a polyline, then walk it accumulating distance.
        val first = bezierPoint(from, control, to, 0f)
        var prev = first
        var carry = state.carry
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val cur = bezierPoint(from, control, to, t)
            val segDx = cur.x - prev.x
            val segDy = cur.y - prev.y
            val segLen = hypot(segDx, segDy)
            if (segLen > 0f) {
                val nx = segDx / segLen
                val ny = segDy / segLen
                var travelled = -carry
                while (travelled + spacingPx <= segLen) {
                    travelled += spacingPx
                    val sample = Offset(prev.x + nx * travelled, prev.y + ny * travelled)
                    stampWithSymmetry(canvas, state, sample)
                }
                carry = segLen - travelled
            }
            prev = cur
        }
        state.carry = carry
    }

    private fun stampWithSymmetry(canvas: AndroidCanvas, state: StrokeState, sourcePoint: Offset) {
        val arms = SymmetryEngine.mirror(listOf(sourcePoint), state.symmetry, state.canvasSize)
        val brush = state.brush
        for (armPoints in arms) {
            val p = armPoints.first()
            drawStampForType(
                canvas,
                state.paint,
                brush,
                applyScatter(p, brush, state.random),
                scatteredScale(1f, brush, state.random)
            )
        }
    }

    private fun bezierPoint(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
        val u = 1f - t
        val x = u * u * p0.x + 2f * u * t * p1.x + t * t * p2.x
        val y = u * u * p0.y + 2f * u * t * p1.y + t * t * p2.y
        return Offset(x, y)
    }

    private fun midpoint(a: Offset, b: Offset): Offset =
        Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)

    private fun applyScatter(p: Offset, brush: BrushSettings, random: Random): Offset {
        if (brush.scatter <= 0f) return p
        val r = brush.size * brush.scatter
        val angle = random.nextFloat() * 2f * Math.PI.toFloat()
        val rr = random.nextFloat() * r
        return Offset(p.x + cos(angle) * rr, p.y + sin(angle) * rr)
    }

    private fun scatteredScale(base: Float, brush: BrushSettings, random: Random): Float {
        if (brush.jitter <= 0f) return base
        val variance = brush.jitter
        return base * (1f - variance + random.nextFloat() * variance * 2f)
    }

    private fun makePaint(brush: BrushSettings, eraseMode: Boolean): Paint {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.color = brush.color.toArgb()
        p.alpha = (brush.opacity * brush.flow * 255f).toInt().coerceIn(0, 255)
        if (eraseMode) {
            p.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        if (brush.hardness < 1f) {
            val blurR = brush.size * (1f - brush.hardness) / 2f
            if (blurR > 0.5f) {
                p.maskFilter = BlurMaskFilter(blurR, BlurMaskFilter.Blur.NORMAL)
            }
        }
        return p
    }
}
