package com.mawaai.love.app.design.domain.model

import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Triple of (hue, saturation, lightness) describing the user-picked garment
 * color on the Customize screen. Stored as floats so the slider UI doesn't
 * jitter when round-tripping through Int↔Float boundaries; the
 * [GarmentColorEngine] consumes these as the HSV substitute targets.
 *
 * Range conventions match the CSS / Android [android.graphics.Color] HSV
 * helpers — hue is degrees, saturation and lightness are normalized.
 *
 *  - [hue]: `[0, 360)` degrees. Wraps at the boundary; 360 is normalized to 0.
 *  - [saturation]: `[0, 1]`. 0 = grayscale, 1 = fully saturated.
 *  - [lightness]: `[0, 1]`. 0 = black, 0.5 = pure hue, 1 = white.
 *
 * Out-of-range inputs are coerced when the color is rendered (toArgb,
 * toHex) so callers (slider drags, hex parses, palette swatches) never
 * silently produce garbage even if the slider state briefly overflows.
 * A `data class` so [androidx.compose.runtime.MutableState] / `StateFlow`
 * can rely on structural equality and partial updates via `copy(...)`.
 */
data class HslColor(
    val hue: Float,
    val saturation: Float,
    val lightness: Float
) {
    init {
        require(!hue.isNaN() && !saturation.isNaN() && !lightness.isNaN()) {
            "HslColor components must be finite"
        }
    }

    /**
     * Returns this color packed as a 32-bit ARGB int with full alpha. The
     * conversion goes HSL → RGB and is the inverse of [fromColor].
     */
    fun toArgb(): Int {
        val h = ((hue % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val l = lightness.coerceIn(0f, 1f)
        val c = (1f - abs(2f * l - 1f)) * s
        val hp = h / 60f
        val x = c * (1f - abs(hp % 2f - 1f))
        val (rp, gp, bp) = when (hp.toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = l - c / 2f
        val r = ((rp + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((gp + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((bp + m) * 255f).roundToInt().coerceIn(0, 255)
        return Color.argb(255, r, g, b)
    }

    /** "#RRGGBB" hex form, suitable for the Customize screen's hex input. */
    fun toHex(): String {
        val argb = toArgb()
        return "#%02X%02X%02X".format(Color.red(argb), Color.green(argb), Color.blue(argb))
    }

    companion object {
        /** Builds an [HslColor] from an ARGB packed int. Alpha is discarded. */
        fun fromColor(argb: Int): HslColor {
            val r = Color.red(argb) / 255f
            val g = Color.green(argb) / 255f
            val b = Color.blue(argb) / 255f
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val delta = maxC - minC
            val l = (maxC + minC) / 2f
            val s = if (delta == 0f) 0f else delta / (1f - abs(2f * l - 1f))
            val h = when {
                delta == 0f -> 0f
                maxC == r -> 60f * (((g - b) / delta) % 6f)
                maxC == g -> 60f * (((b - r) / delta) + 2f)
                else -> 60f * (((r - g) / delta) + 4f)
            }
            val hue = if (h < 0f) h + 360f else h
            return HslColor(hue, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
        }

        /**
         * Parses a hex string of the form `#RGB`, `#RRGGBB`, or the same
         * without the leading `#`. Returns null if the input is malformed —
         * the caller (text input) preserves the user's typing in that case.
         */
        fun fromHex(hex: String): HslColor? {
            val trimmed = hex.trim().removePrefix("#")
            val expanded = when (trimmed.length) {
                3 -> trimmed.map { "$it$it" }.joinToString("")
                6 -> trimmed
                else -> return null
            }
            val intValue = expanded.toLongOrNull(radix = 16) ?: return null
            val r = ((intValue shr 16) and 0xFF).toInt()
            val g = ((intValue shr 8) and 0xFF).toInt()
            val b = (intValue and 0xFF).toInt()
            return fromColor(Color.argb(255, r, g, b))
        }
    }
}
