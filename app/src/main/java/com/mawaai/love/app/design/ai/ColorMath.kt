package com.mawaai.love.app.design.ai

import kotlin.math.max
import kotlin.math.min

/**
 * Shared colour-math helpers used by `AutoStylePicker` and
 * `LocalDrawingAnalyzer`. Promoted to a package-level file in Phase 14
 * so the previously-duplicated `hueBucket` body lives in exactly one
 * place. The function is a pure RGB → 8-bucket hue partition; both
 * analyzers consume it identically and a tweak to one is correctly a
 * tweak to both.
 */

/**
 * Coarse 8-bucket hue partition of an `(r, g, b)` triple in `[0, 255]`.
 * Returns the 0..7 bucket index for chromatic colours, or `-1` for
 * effectively-greyscale samples (delta < 24, ~9 % of the channel range).
 *
 * The 8 buckets are 45-degree slices on the standard RGB hue ring, so
 * (red, orange, yellow, lime, green, cyan, blue, purple/magenta) end
 * up on adjacent ints. Two near-identical reds always land in the same
 * bucket — this is what each analyzer wants when counting "how many
 * distinct hues did the user use" without inflating the count from
 * tiny anti-alias variations.
 */
internal fun hueBucket(r: Int, g: Int, b: Int): Int {
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    if (delta < HUE_GREYSCALE_THRESHOLD) return GREYSCALE_BUCKET
    val hue = when (maxC) {
        r -> 60 * (((g - b).toFloat() / delta) % 6)
        g -> 60 * (((b - r).toFloat() / delta) + 2)
        else -> 60 * (((r - g).toFloat() / delta) + 4)
    }
    val positive = (hue + 360) % 360
    return (positive / 45f).toInt() // 0..7
}

private const val HUE_GREYSCALE_THRESHOLD = 24
private const val GREYSCALE_BUCKET = -1
