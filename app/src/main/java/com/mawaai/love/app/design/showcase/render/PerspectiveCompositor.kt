package com.mawaai.love.app.design.showcase.render

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.geometry.Size

/**
 * Renders an artwork [bitmap] onto a target Canvas with the given quad-warp.
 * The four destination corners are TL, TR, BR, BL in pixel coordinates.
 *
 * Uses Android's polyToPoly() — a 4-point perspective transform implemented in
 * the framework. No OpenCV, no external libraries.
 */
object PerspectiveCompositor {
    fun compose(
        backdrop: Bitmap,
        artwork: Bitmap,
        tl: Pair<Float, Float>,
        tr: Pair<Float, Float>,
        br: Pair<Float, Float>,
        bl: Pair<Float, Float>,
        artworkAlpha: Float = 1f
    ): Bitmap {
        val output = backdrop.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = AndroidCanvas(output)

        val src = floatArrayOf(
            0f, 0f,
            artwork.width.toFloat(), 0f,
            artwork.width.toFloat(), artwork.height.toFloat(),
            0f, artwork.height.toFloat()
        )
        val dst = floatArrayOf(
            tl.first, tl.second,
            tr.first, tr.second,
            br.first, br.second,
            bl.first, bl.second
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(src, 0, dst, 0, 4)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            alpha = (artworkAlpha * 255f).toInt().coerceIn(0, 255)
        }
        canvas.drawBitmap(artwork, matrix, paint)
        return output
    }

    /**
     * Computes destination quad in pixel coords given a normalized [zone] and
     * the canvas [size].
     */
    fun pixelQuad(
        zone: com.mawaai.love.app.design.showcase.domain.FrameZone,
        size: Size
    ): Quad {
        val w = size.width
        val h = size.height
        return Quad(
            tl = zone.tlX * w to zone.tlY * h,
            tr = zone.trX * w to zone.trY * h,
            br = zone.brX * w to zone.brY * h,
            bl = zone.blX * w to zone.blY * h
        )
    }

    data class Quad(
        val tl: Pair<Float, Float>,
        val tr: Pair<Float, Float>,
        val br: Pair<Float, Float>,
        val bl: Pair<Float, Float>
    )
}
