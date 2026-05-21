package com.mawaai.love.app.design.canvas.engine

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs

object FillEngine {
    /**
     * Performs a 4-way flood-fill on [bitmap] starting from ([x],[y]).
     * Pixels within [tolerance] of the seed color are replaced with [target].
     */
    fun fill(bitmap: Bitmap, x: Int, y: Int, target: Color, tolerance: Int = 32) {
        val width = bitmap.width
        val height = bitmap.height
        if (x !in 0 until width || y !in 0 until height) return

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val seed = pixels[y * width + x]
        val replacement = target.toArgb()
        if (seed == replacement) return

        val stack = ArrayDeque<Int>()
        stack.addLast(y * width + x)

        while (stack.isNotEmpty()) {
            val idx = stack.removeLast()
            if (idx < 0 || idx >= pixels.size) continue
            if (!matches(pixels[idx], seed, tolerance)) continue
            pixels[idx] = replacement
            val px = idx % width
            val py = idx / width
            if (px > 0) stack.addLast(idx - 1)
            if (px < width - 1) stack.addLast(idx + 1)
            if (py > 0) stack.addLast(idx - width)
            if (py < height - 1) stack.addLast(idx + width)
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun matches(pixel: Int, seed: Int, tolerance: Int): Boolean {
        if (tolerance == 0) return pixel == seed
        val a1 = (pixel ushr 24) and 0xFF
        val r1 = (pixel ushr 16) and 0xFF
        val g1 = (pixel ushr 8) and 0xFF
        val b1 = pixel and 0xFF
        val a2 = (seed ushr 24) and 0xFF
        val r2 = (seed ushr 16) and 0xFF
        val g2 = (seed ushr 8) and 0xFF
        val b2 = seed and 0xFF
        return abs(a1 - a2) <= tolerance &&
            abs(r1 - r2) <= tolerance &&
            abs(g1 - g2) <= tolerance &&
            abs(b1 - b2) <= tolerance
    }
}
