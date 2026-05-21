package com.mawaai.love.app.design.ai

import android.graphics.Bitmap
import androidx.core.graphics.get
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Cheap, offline bitmap statistics for [LocalDrawingAnalyzer]. A single
 * 32×32 sampling grid produces every metric the analyzer needs in a
 * single pass — the heaviest call site is < 50 ms on a 1024×1024
 * canvas.
 */
internal data class DrawingStats(
    val coverage: Float,
    val averageBrightness: Float,
    val uniqueHues: Int,
    val asymmetryScore: Float,
    val focusOffset: Float,
    val edgeJaggedness: Float,
    // Phase 20 — fraction of inked samples that are 1-cell-wide
    // (≤ 1 cardinal neighbour also inked). Empirically values above
    // ~0.55 correlate with prints that lose detail at 12 cm scale.
    val thinStrokeFraction: Float,
    // Phase 20 — fraction of saturated samples covered by the top
    // 3 hue buckets. 1.0 = highly coherent palette; values below
    // ~0.5 are visually busy enough to benefit from rebalancing.
    val paletteCoherence: Float
)

private const val SAMPLE_GRID = 32

// Luma delta above which an adjacent neighbour pair counts as
// a "jagged" transition. 0.30 is tuned for the canvas's
// default brush — empirically separates intentional dark/light
// outlines from accidental pixel-level shake.
private const val JAGGED_LUMA_THRESHOLD = 0.30f

// Hue histogram bucket count. 12 × 30° matches the
// [DrawingActionEngine] palette balancer so the analyzer's
// "you have N dominant buckets" signal aligns with the
// action's "shift toward top 3 of N" effect.
private const val PALETTE_HUE_BUCKETS = 12
private const val PALETTE_BUCKETS = 3

internal fun sampleDrawingStats(bitmap: Bitmap): DrawingStats {
    val grid = SAMPLE_GRID
    val w = bitmap.width
    val h = bitmap.height
    var inked = 0
    var totalLuma = 0f
    var sumX = 0f
    var sumY = 0f
    val hues = HashSet<Int>()
    val leftSamples = ArrayList<Int>(grid * grid / 2)
    val rightSamples = ArrayList<Int>(grid * grid / 2)
    // Luma grid for the edge-jaggedness pass — NaN means
    // un-inked. Reused after the main pass to compute
    // neighbour deltas without re-reading the bitmap.
    val lumaGrid = FloatArray(grid * grid) { Float.NaN }
    // Phase 20 — hue histogram for palette coherence.
    // 12 buckets × 30°. Only saturated samples (S > 0.18) contribute
    // so outline / grayscale strokes don't drown out the actual
    // palette. Mirrors the BalancePalette action's threshold.
    val hueHistogram = IntArray(PALETTE_HUE_BUCKETS)
    var saturatedCount = 0
    // 0..255 alpha grid — non-zero where inked. Reused after the
    // main pass to count "thin" samples (inked + ≤ 1 inked
    // cardinal neighbour). Cheaper than re-reading the bitmap.
    val alphaGrid = IntArray(grid * grid)

    for (j in 0 until grid) {
        for (i in 0 until grid) {
            // `coerceIn` is defensive — float-rounding on the right
            // edge can produce `w` (one past the last valid x) on
            // some inputs, and `bitmap[w, ...]` throws.
            val x = (i.toFloat() / (grid - 1) * (w - 1)).toInt().coerceIn(0, w - 1)
            val y = (j.toFloat() / (grid - 1) * (h - 1)).toInt().coerceIn(0, h - 1)
            val argb = bitmap[x, y]
            val a = (argb ushr 24) and 0xFF
            if (a < 24) continue
            inked++
            alphaGrid[j * grid + i] = a
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            totalLuma += luma
            lumaGrid[j * grid + i] = luma
            // Normalize sample coords to [0, 1] for centroid math.
            sumX += i.toFloat() / (grid - 1)
            sumY += j.toFloat() / (grid - 1)
            hues += hueBucket(r, g, b)
            if (i < grid / 2) leftSamples += argb else rightSamples += argb
            // Phase 20 — hue histogram. Re-derive H + S from RGB
            // here rather than calling Color.colorToHSV per sample
            // (avoids the FloatArray allocation per pixel).
            val maxC = maxOf(r, maxOf(g, b))
            val minC = minOf(r, minOf(g, b))
            val delta = maxC - minC
            if (maxC > 0 && delta > 0) {
                val saturation = delta.toFloat() / maxC
                if (saturation >= 0.18f) {
                    var hue = when (maxC) {
                        r -> 60f * ((g - b).toFloat() / delta)
                        g -> 60f * ((b - r).toFloat() / delta) + 120f
                        else -> 60f * ((r - g).toFloat() / delta) + 240f
                    }
                    if (hue < 0f) hue += 360f
                    val bucket = ((hue / 360f) * PALETTE_HUE_BUCKETS).toInt()
                        .coerceIn(0, PALETTE_HUE_BUCKETS - 1)
                    hueHistogram[bucket]++
                    saturatedCount++
                }
            }
        }
    }

    val totalCells = grid * grid
    val coverage = inked.toFloat() / totalCells
    val avgBrightness = if (inked > 0) totalLuma / inked else 0f

    // Asymmetry: compare average luma + colour of left and right halves.
    val asymmetry = run {
        if (leftSamples.isEmpty() || rightSamples.isEmpty()) return@run 0f
        val l = averageColor(leftSamples)
        val r = averageColor(rightSamples)
        val dr = abs(l.first - r.first) / 255f
        val dg = abs(l.second - r.second) / 255f
        val db = abs(l.third - r.third) / 255f
        (dr + dg + db) / 3f
    }

    // Centroid of inked pixels in normalized [0, 1] coords. Compared
    // against the four rule-of-thirds intersection points to compute
    // the focal offset — the distance to the NEAREST power point.
    val focusOffset = if (inked == 0) 0f else {
        val cx = sumX / inked
        val cy = sumY / inked
        val thirds = floatArrayOf(1f / 3f, 2f / 3f)
        var best = Float.MAX_VALUE
        for (tx in thirds) for (ty in thirds) {
            val d = hypot((cx - tx).toDouble(), (cy - ty).toDouble()).toFloat()
            if (d < best) best = d
        }
        best
    }

    // Edge jaggedness — fraction of inked neighbour pairs whose luma
    // delta exceeds JAGGED_LUMA_THRESHOLD. High = strokes flip
    // dark/light pixel-by-pixel (shaky / aliased). Low = smooth.
    // Single combined H + V pass in row-major order — keeps reads
    // sequential and reuses the `a` cell load for both deltas.
    var jagged = 0
    var totalPairs = 0
    for (j in 0 until grid) {
        for (i in 0 until grid) {
            val a = lumaGrid[j * grid + i]
            if (a.isNaN()) continue
            if (i < grid - 1) {
                val rightVal = lumaGrid[j * grid + i + 1]
                if (!rightVal.isNaN()) {
                    totalPairs++
                    if (abs(a - rightVal) > JAGGED_LUMA_THRESHOLD) jagged++
                }
            }
            if (j < grid - 1) {
                val down = lumaGrid[(j + 1) * grid + i]
                if (!down.isNaN()) {
                    totalPairs++
                    if (abs(a - down) > JAGGED_LUMA_THRESHOLD) jagged++
                }
            }
        }
    }
    val edgeJaggedness = if (totalPairs > 0) jagged.toFloat() / totalPairs else 0f

    // Phase 20 — thin-stroke fraction. A sample is "thin" if it's
    // inked but ≤ 1 of its 4 cardinal neighbours are also inked
    // (i.e. it sits on a 1-cell-wide line). High fraction → fabric
    // print risk → suggest ThickenThinStrokes.
    var thinSamples = 0
    var inkedSamples = 0
    for (j in 0 until grid) {
        for (i in 0 until grid) {
            val idx = j * grid + i
            if (alphaGrid[idx] == 0) continue
            inkedSamples++
            var inkedNeighbours = 0
            if (i > 0 && alphaGrid[idx - 1] != 0) inkedNeighbours++
            if (i < grid - 1 && alphaGrid[idx + 1] != 0) inkedNeighbours++
            if (j > 0 && alphaGrid[idx - grid] != 0) inkedNeighbours++
            if (j < grid - 1 && alphaGrid[idx + grid] != 0) inkedNeighbours++
            if (inkedNeighbours <= 1) thinSamples++
        }
    }
    val thinStrokeFraction = if (inkedSamples > 0) thinSamples.toFloat() / inkedSamples else 0f

    // Phase 20 — palette coherence. The fraction of saturated
    // samples covered by the top 3 hue buckets. 1.0 = all
    // saturated pixels live in 3 buckets (highly coherent);
    // approaching 0.3 = saturated pixels are spread across the
    // entire 12-bucket histogram (chaotic).
    val paletteCoherence = if (saturatedCount == 0) 1f else {
        val top3Sum = hueHistogram.sortedDescending().take(PALETTE_BUCKETS).sum()
        top3Sum.toFloat() / saturatedCount
    }

    return DrawingStats(
        coverage = coverage,
        averageBrightness = avgBrightness,
        uniqueHues = hues.size,
        asymmetryScore = asymmetry,
        focusOffset = focusOffset,
        edgeJaggedness = edgeJaggedness,
        thinStrokeFraction = thinStrokeFraction,
        paletteCoherence = paletteCoherence
    )
}

private fun averageColor(samples: List<Int>): Triple<Int, Int, Int> {
    var r = 0L; var g = 0L; var b = 0L
    for (argb in samples) {
        r += (argb shr 16) and 0xFF
        g += (argb shr 8) and 0xFF
        b += argb and 0xFF
    }
    val n = samples.size.coerceAtLeast(1)
    return Triple((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
}
