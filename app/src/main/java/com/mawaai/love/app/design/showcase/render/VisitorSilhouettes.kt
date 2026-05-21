package com.mawaai.love.app.design.showcase.render

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Animated visitor silhouettes that walk slowly across the bottom of the scene to
 * sell the gallery feeling. Pure DrawScope, no Lottie.
 */
@Composable
fun VisitorSilhouettes(
    visitorCount: Int = 3,
    modifier: Modifier = Modifier,
    color: Color = Color(0x88000000)
) {
    val transition = rememberInfiniteTransition(label = "visitors")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "walk"
    )

    val seeds = remember(visitorCount) {
        (0 until visitorCount).map { it.toFloat() / visitorCount.coerceAtLeast(1) }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val baseY = size.height * 0.86f
        val visitorH = size.height * 0.16f
        seeds.forEachIndexed { idx, seed ->
            val phase = (progress + seed) % 1f
            val x = phase * (size.width + visitorH) - visitorH
            drawVisitor(
                cx = x,
                topY = baseY - visitorH,
                size = visitorH,
                tone = color,
                hatVariant = idx % 3
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVisitor(
    cx: Float, topY: Float, size: Float, tone: Color, hatVariant: Int
) {
    // Head
    drawCircle(color = tone, radius = size * 0.10f, center = Offset(cx, topY + size * 0.10f))
    // Body
    val bodyPath = Path().apply {
        moveTo(cx - size * 0.13f, topY + size * 0.20f)
        lineTo(cx + size * 0.13f, topY + size * 0.20f)
        lineTo(cx + size * 0.10f, topY + size * 0.65f)
        lineTo(cx - size * 0.10f, topY + size * 0.65f)
        close()
    }
    drawPath(bodyPath, color = tone)
    // Legs
    drawRect(color = tone, topLeft = Offset(cx - size * 0.10f, topY + size * 0.65f), size = androidx.compose.ui.geometry.Size(size * 0.06f, size * 0.30f))
    drawRect(color = tone, topLeft = Offset(cx + size * 0.04f, topY + size * 0.65f), size = androidx.compose.ui.geometry.Size(size * 0.06f, size * 0.30f))
    // Optional Sudanese 'imma turban / kufi
    when (hatVariant) {
        1 -> drawCircle(color = tone, radius = size * 0.13f, center = Offset(cx, topY + size * 0.05f))
        2 -> drawRect(color = tone, topLeft = Offset(cx - size * 0.12f, topY), size = androidx.compose.ui.geometry.Size(size * 0.24f, size * 0.06f))
    }
}
