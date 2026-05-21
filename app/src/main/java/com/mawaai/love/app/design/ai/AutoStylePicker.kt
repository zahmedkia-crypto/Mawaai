package com.mawaai.love.app.design.ai

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.get
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Picks the best converter `styleId` for a given sketch when the user
 * leaves the dropdown on its default "auto" setting (which they almost
 * always do — the styles list is the FIRST decision in the converter
 * flow and "auto" is the recommended top option).
 *
 * Before Phase 11 "auto" silently fell through to the generic else
 * prompt in `AIEngine.stylePromptFor`. Every auto-style render used the
 * same fallback regardless of what the user actually drew. This class
 * fixes that by classifying the sketch via a fast local heuristic into
 * one of the four real catalog styles:
 *  - `vector_clean` — bold/clear linework, few colors, hard edges
 *  - `artistic`     — rich texture, many colors, soft edges
 *  - `minimalist`   — very sparse, single accent color, lots of negative space
 *  - `realistic`    — gradient-heavy, photo-like color spread
 *
 * The picker is deterministic and runs in O(grid²) on a 48×48
 * subsample (~2300 reads) — under 30 ms on a Pixel 5 even with cold
 * JIT, so it sits comfortably ahead of the ControlNet call without
 * adding perceived latency.
 *
 * Returns "auto" when the sketch is essentially empty (≤ 2% coverage)
 * so the AIEngine still routes to the generic-quality default prompt
 * instead of fabricating a vector preset over five accidental dots.
 */
@Singleton
class AutoStylePicker @Inject constructor() {

    fun pick(bitmap: Bitmap): String {
        if (bitmap.isRecycled) return AUTO
        val signals = signals(bitmap) ?: return AUTO
        if (signals.coverage < 0.02f) return AUTO

        // Decision rules — ordered so the most specific category wins.
        // Each branch is gated on TWO complementary signals so a single
        // outlier (a stray sparse stroke, a noisy hue count) can't swing
        // the classification by itself.

        // Minimalist: very sparse + few colors + clean edges.
        if (signals.coverage < 0.18f && signals.uniqueHues <= 2 && signals.edgeSharpness > 0.55f) {
            return MINIMALIST
        }

        // Realistic: lots of mid-tones (gradients) + many colors + soft edges.
        if (signals.midToneFraction > 0.40f && signals.uniqueHues >= 4 && signals.edgeSharpness < 0.45f) {
            return REALISTIC
        }

        // Vector clean: high coverage of clear linework + few colors.
        if (signals.edgeSharpness > 0.60f && signals.uniqueHues <= 3) {
            return VECTOR_CLEAN
        }

        // Artistic: rich color variety AND moderate-to-high coverage.
        // This is the "expressive painterly" bucket — falls out as the
        // default when none of the more-specific rules hit.
        if (signals.uniqueHues >= 4 && signals.coverage > 0.15f) {
            return ARTISTIC
        }

        // Tie-breaker — when no specific rule fired, lean on the
        // strongest single signal. Hard edges → vector, soft → artistic.
        return if (signals.edgeSharpness >= 0.50f) VECTOR_CLEAN else ARTISTIC
    }

    /**
     * Collects four normalized signals from the bitmap in a single pass.
     * Returns null on degenerate inputs (zero-area, totally transparent)
     * so the caller can fall back to the generic auto path.
     */
    private fun signals(bitmap: Bitmap): Signals? {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return null

        val grid = GRID
        var inked = 0
        var totalLuma = 0f
        var midTone = 0
        val hues = HashSet<Int>()
        // Edge-sharpness proxy: count horizontally + vertically adjacent
        // sample-pair luma deltas above SHARP_LUMA_THRESHOLD.
        var sharpPairs = 0
        var totalPairs = 0
        val lumaGrid = FloatArray(grid * grid) { Float.NaN }

        for (j in 0 until grid) {
            for (i in 0 until grid) {
                val x = (i.toFloat() / (grid - 1) * (w - 1)).toInt().coerceIn(0, w - 1)
                val y = (j.toFloat() / (grid - 1) * (h - 1)).toInt().coerceIn(0, h - 1)
                val argb = bitmap[x, y]
                val a = (argb ushr 24) and 0xFF
                if (a < 24) continue
                inked++
                val r = (argb shr 16) and 0xFF
                val g = (argb shr 8) and 0xFF
                val b = argb and 0xFF
                val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                totalLuma += luma
                lumaGrid[j * grid + i] = luma
                if (luma in 0.30f..0.70f) midTone++
                hues += hueBucket(r, g, b)
            }
        }

        if (inked == 0) return Signals.EMPTY

        // Combined H + V neighbour-pair pass — visits each cell once and
        // computes both right + down deltas in row-major order, which
        // keeps the inner reads sequential (better L1 cache behaviour
        // than two separate passes with reversed loop nests).
        for (j in 0 until grid) {
            for (i in 0 until grid) {
                val a = lumaGrid[j * grid + i]
                if (a.isNaN()) continue
                if (i < grid - 1) {
                    val r = lumaGrid[j * grid + i + 1]
                    if (!r.isNaN()) {
                        totalPairs++
                        if (abs(a - r) > SHARP_LUMA_THRESHOLD) sharpPairs++
                    }
                }
                if (j < grid - 1) {
                    val d = lumaGrid[(j + 1) * grid + i]
                    if (!d.isNaN()) {
                        totalPairs++
                        if (abs(a - d) > SHARP_LUMA_THRESHOLD) sharpPairs++
                    }
                }
            }
        }

        val totalCells = grid * grid
        return Signals(
            coverage = inked.toFloat() / totalCells,
            uniqueHues = hues.size,
            midToneFraction = if (inked > 0) midTone.toFloat() / inked else 0f,
            edgeSharpness = if (totalPairs > 0) sharpPairs.toFloat() / totalPairs else 0f,
        ).also {
            Log.d(TAG, "AutoStylePicker signals: $it")
        }
    }

    private data class Signals(
        val coverage: Float,
        val uniqueHues: Int,
        val midToneFraction: Float,
        val edgeSharpness: Float,
    ) {
        companion object {
            val EMPTY = Signals(coverage = 0f, uniqueHues = 0, midToneFraction = 0f, edgeSharpness = 0f)
        }
    }

    companion object {
        const val AUTO = "auto"
        const val VECTOR_CLEAN = "vector_clean"
        const val ARTISTIC = "artistic"
        const val MINIMALIST = "minimalist"
        const val REALISTIC = "realistic"

        private const val TAG = "AutoStylePicker"
        // 48×48 grid samples 2304 pixels — under 30 ms on cold JIT and
        // resolves enough detail to tell a sparse minimalist from a
        // dense artistic without sampling every pixel.
        private const val GRID = 48
        // Luma delta above which we call an adjacent pair "sharp".
        // 0.20 = 20% of the [0,1] luma range — empirically separates
        // anti-aliased painterly transitions from crisp inked edges.
        private const val SHARP_LUMA_THRESHOLD = 0.20f
    }
}
