package com.mawaai.love.app.design.showcase.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mawaai.love.app.design.showcase.domain.SceneBackdrop

/**
 * Programmatically draws a stylized backdrop for each scene. No bitmap assets needed.
 * Each backdrop is composed of simple geometry (walls, floor, window light) using
 * the design palette colors so it always feels on-brand.
 */
@Composable
fun SceneBackdropView(
    backdrop: SceneBackdrop,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        when (backdrop) {
            SceneBackdrop.GALLERY -> drawGallery()
            SceneBackdrop.LIVING_ROOM -> drawLivingRoom()
            SceneBackdrop.MUSEUM -> drawMuseum()
            SceneBackdrop.OUTDOOR -> drawOutdoor()
            SceneBackdrop.MODERN_HALL -> drawModernHall()
            SceneBackdrop.MAJLIS -> drawMajlis()
        }
    }
}

private fun DrawScope.drawGallery() {
    // Cream wall + warm wooden floor
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFF7EFE0), Color(0xFFE8D8B5)),
            startY = 0f, endY = size.height * 0.85f
        ),
        size = Size(size.width, size.height * 0.85f)
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF6B4925), Color(0xFF3F2A14)),
            startY = size.height * 0.85f, endY = size.height
        ),
        topLeft = Offset(0f, size.height * 0.85f),
        size = Size(size.width, size.height * 0.15f)
    )
    // Spotlights
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x40FFE4B5), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.45f),
            radius = size.width * 0.4f
        ),
        radius = size.width * 0.4f,
        center = Offset(size.width * 0.5f, size.height * 0.45f)
    )
}

private fun DrawScope.drawLivingRoom() {
    // Warm beige walls, two-tone (wainscot)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFEED6B0), Color(0xFFE0BE85))
        ),
        size = Size(size.width, size.height * 0.55f)
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFB58A52), Color(0xFF7E5630))
        ),
        topLeft = Offset(0f, size.height * 0.55f),
        size = Size(size.width, size.height * 0.30f)
    )
    drawRect(
        color = Color(0xFF3F2A14),
        topLeft = Offset(0f, size.height * 0.85f),
        size = Size(size.width, size.height * 0.15f)
    )
    // Sofa silhouette
    drawRect(
        color = Color(0xFF402418).copy(alpha = 0.85f),
        topLeft = Offset(size.width * 0.05f, size.height * 0.72f),
        size = Size(size.width * 0.35f, size.height * 0.18f)
    )
    drawRect(
        color = Color(0xFF402418).copy(alpha = 0.85f),
        topLeft = Offset(size.width * 0.60f, size.height * 0.72f),
        size = Size(size.width * 0.35f, size.height * 0.18f)
    )
}

private fun DrawScope.drawMuseum() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFE7EEF5), Color(0xFFCBD7E2))
        ),
        size = Size(size.width, size.height * 0.85f)
    )
    drawRect(
        color = Color(0xFF7E8694),
        topLeft = Offset(0f, size.height * 0.85f),
        size = Size(size.width, size.height * 0.15f)
    )
    // Architectural arch above
    val path = Path().apply {
        moveTo(size.width * 0.15f, size.height * 0.85f)
        lineTo(size.width * 0.15f, size.height * 0.15f)
        cubicTo(
            size.width * 0.15f, size.height * 0.05f,
            size.width * 0.85f, size.height * 0.05f,
            size.width * 0.85f, size.height * 0.15f
        )
        lineTo(size.width * 0.85f, size.height * 0.85f)
    }
    drawPath(path, color = Color(0xFFA9B4C2).copy(alpha = 0.4f), style = Stroke(width = 6f))
}

private fun DrawScope.drawOutdoor() {
    // Sky
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFE5C99F), Color(0xFFC09866))
        ),
        size = Size(size.width, size.height * 0.45f)
    )
    // Brick wall texture
    drawRect(
        color = Color(0xFFB68561),
        topLeft = Offset(0f, size.height * 0.45f),
        size = Size(size.width, size.height * 0.55f)
    )
    val brickH = size.height * 0.025f
    val brickW = size.width * 0.10f
    var y = size.height * 0.45f
    var rowIdx = 0
    while (y < size.height) {
        val offsetX = if (rowIdx % 2 == 0) 0f else brickW * 0.5f
        var x = -offsetX
        while (x < size.width) {
            drawRect(
                color = Color(0xFF8B5A30).copy(alpha = 0.5f),
                topLeft = Offset(x, y),
                size = Size(brickW, brickH),
                style = Stroke(width = 1f)
            )
            x += brickW
        }
        y += brickH
        rowIdx++
    }
}

private fun DrawScope.drawModernHall() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFEFEFEF), Color(0xFFC2C2C2))
        ),
        size = Size(size.width, size.height * 0.85f)
    )
    drawRect(
        color = Color(0xFF1F1F1F),
        topLeft = Offset(0f, size.height * 0.85f),
        size = Size(size.width, size.height * 0.15f)
    )
    // Vertical accent line
    drawLine(
        color = Color(0xFFC8860A),
        start = Offset(size.width * 0.5f, 0f),
        end = Offset(size.width * 0.5f, size.height * 0.85f),
        strokeWidth = 3f
    )
}

private fun DrawScope.drawMajlis() {
    // Warm ochre wall + carpet floor
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFE8B469), Color(0xFFC8860A))
        ),
        size = Size(size.width, size.height * 0.65f)
    )
    drawRect(
        color = Color(0xFF8B2F0F),
        topLeft = Offset(0f, size.height * 0.65f),
        size = Size(size.width, size.height * 0.35f)
    )
    // Cushion shapes at base (silhouettes of Sudanese sitting cushions)
    for (i in 0..3) {
        val cx = size.width * (0.15f + i * 0.25f)
        val cy = size.height * 0.83f
        drawOval(
            color = Color(0xFF6B2008).copy(alpha = 0.8f),
            topLeft = Offset(cx - size.width * 0.07f, cy - size.height * 0.04f),
            size = Size(size.width * 0.14f, size.height * 0.08f)
        )
    }
    // Geometric pattern band
    val bandY = size.height * 0.65f
    val triW = size.width * 0.05f
    var x = 0f
    while (x < size.width) {
        val path = Path().apply {
            moveTo(x, bandY)
            lineTo(x + triW / 2, bandY - size.height * 0.025f)
            lineTo(x + triW, bandY)
        }
        drawPath(path, color = Color(0xFFFFE4A1), style = Stroke(width = 2f))
        x += triW
    }
}
