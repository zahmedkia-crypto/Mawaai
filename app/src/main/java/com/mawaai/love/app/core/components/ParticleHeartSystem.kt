package com.mawaai.love.app.core.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.mawaai.love.app.core.theme.MawaaiColors
import kotlin.random.Random

data class HeartParticle(
    val id: Int,
    val startX: Float,      // 0f..1f relative
    val speed: Int,         // duration in ms
    val size: Float,        // size in dp
    val alpha: Float,       // 0.05f..0.20f
    val drift: Float,       // horizontal drift
    val delay: Int          // start delay in ms
)

@Composable
fun ParticleHeartSystem(
    // `modifier` precedes `particleCount` to follow the Compose
    // Modifier-first convention (lint: ModifierParameter). All six
    // existing call sites use named arguments, so the reorder is
    // source-compatible.
    modifier: Modifier = Modifier,
    particleCount: Int = 8
) {
    val particles = remember {
        List(particleCount) { i ->
            HeartParticle(
                id = i,
                startX = Random.nextFloat(),
                speed = Random.nextInt(3000, 6000),
                size = Random.nextInt(15, 40).toFloat(),
                alpha = Random.nextFloat() * 0.15f + 0.05f,
                drift = (Random.nextFloat() - 0.5f) * 200f,
                delay = Random.nextInt(0, 3000)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val progresses = particles.map { particle ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(particle.speed, delayMillis = particle.delay, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progress_${particle.id}"
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEachIndexed { index, particle ->
            val progress = progresses[index].value
            val x = (particle.startX * width) + (particle.drift * progress)
            val y = height - (progress * (height + 200f))
            val currentSize = particle.size.dp.toPx()
            val rotation = progress * 45f

            rotate(rotation, pivot = androidx.compose.ui.geometry.Offset(x, y)) {
                val path = Path().apply {
                    val s = currentSize
                    moveTo(x, y - s * 0.3f)
                    cubicTo(x - s * 0.5f, y - s * 0.8f, x - s, y - s * 0.3f, x, y + s * 0.4f)
                    cubicTo(x + s, y - s * 0.3f, x + s * 0.5f, y - s * 0.8f, x, y - s * 0.3f)
                    close()
                }
                drawPath(
                    path = path,
                    color = MawaaiColors.RoseGold,
                    alpha = particle.alpha * (1f - progress)
                )
            }
        }
    }
}
