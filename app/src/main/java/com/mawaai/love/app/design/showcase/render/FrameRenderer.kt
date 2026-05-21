package com.mawaai.love.app.design.showcase.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mawaai.love.app.design.showcase.domain.ShowcaseFrame
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Draws a frame around the warped artwork zone. Frames are drawn in DrawScope so
 * they cleanly overlay the perspective-warped artwork.
 *
 * Quad vertices are TL, TR, BR, BL (clockwise from top-left).
 */
object FrameRenderer {
    fun draw(
        scope: DrawScope,
        frame: ShowcaseFrame,
        tl: Offset, tr: Offset, br: Offset, bl: Offset
    ) {
        if (frame == ShowcaseFrame.NONE) return
        val avgEdge = (
            hypot(tr.x - tl.x, tr.y - tl.y) +
            hypot(bl.x - tl.x, bl.y - tl.y)
        ) / 2f
        val thickness = avgEdge * 0.04f

        val path = Path().apply {
            moveTo(tl.x, tl.y); lineTo(tr.x, tr.y); lineTo(br.x, br.y); lineTo(bl.x, bl.y); close()
        }

        when (frame) {
            ShowcaseFrame.GOLD -> {
                scope.drawPath(
                    path,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFD4AF37), Color(0xFFFCEAA8), Color(0xFFC8860A)),
                        start = tl, end = br
                    ),
                    style = Stroke(width = thickness * 2f)
                )
                // Inner highlight
                scope.drawPath(
                    path,
                    color = Color(0xFFFFE9A6).copy(alpha = 0.6f),
                    style = Stroke(width = thickness * 0.4f)
                )
                // Ornate corners
                drawOrnateCorner(scope, tl, thickness * 1.5f, Color(0xFFFCEAA8))
                drawOrnateCorner(scope, tr, thickness * 1.5f, Color(0xFFFCEAA8))
                drawOrnateCorner(scope, br, thickness * 1.5f, Color(0xFFFCEAA8))
                drawOrnateCorner(scope, bl, thickness * 1.5f, Color(0xFFFCEAA8))
            }
            ShowcaseFrame.MODERN_BLACK -> {
                scope.drawPath(
                    path,
                    color = Color(0xFF1F1F1F),
                    style = Stroke(width = thickness * 1.5f)
                )
            }
            ShowcaseFrame.ARABIC_CARVED -> {
                scope.drawPath(
                    path,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF8B5A30), Color(0xFFD2A86F), Color(0xFF8B5A30))
                    ),
                    style = Stroke(width = thickness * 2.4f)
                )
                // Geometric pattern: dotted diamonds along edges
                drawDiamondsAlong(scope, tl, tr, thickness)
                drawDiamondsAlong(scope, tr, br, thickness)
                drawDiamondsAlong(scope, br, bl, thickness)
                drawDiamondsAlong(scope, bl, tl, thickness)
            }
            ShowcaseFrame.NONE -> Unit
        }
    }

    private fun drawOrnateCorner(scope: DrawScope, p: Offset, size: Float, color: Color) {
        scope.drawCircle(color = color, radius = size, center = p)
    }

    private fun drawDiamondsAlong(scope: DrawScope, a: Offset, b: Offset, thickness: Float) {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = hypot(dx, dy)
        if (len <= 0f) return
        val nx = dx / len
        val ny = dy / len
        val perpX = -ny
        val perpY = nx
        val spacing = thickness * 4f
        var t = spacing / 2f
        while (t < len) {
            val cx = a.x + nx * t
            val cy = a.y + ny * t
            val s = thickness * 0.5f
            val path = Path().apply {
                moveTo(cx + perpX * s, cy + perpY * s)
                lineTo(cx + nx * s, cy + ny * s)
                lineTo(cx - perpX * s, cy - perpY * s)
                lineTo(cx - nx * s, cy - ny * s)
                close()
            }
            scope.drawPath(path, color = Color(0xFFFCEAA8))
            t += spacing
        }
    }
}
