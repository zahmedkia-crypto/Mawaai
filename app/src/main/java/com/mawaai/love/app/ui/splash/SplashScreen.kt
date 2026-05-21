package com.mawaai.love.app.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mawaai.love.app.core.components.ParticleHeartSystem
import com.mawaai.love.app.core.theme.*
import kotlinx.coroutines.delay
import androidx.compose.ui.tooling.preview.Preview

/**
 * Brief brand splash with the heart-and-ring logo, then always hands off to
 * the intro video (which in turn routes to onboarding on first launch or
 * straight to home afterwards).
 */
@Composable
fun SplashScreen(
    onNavigateToIntro: () -> Unit
) {
    val scaleAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = HeartSpring
        )
        delay(1200) // Brief brand moment before the intro video.
        onNavigateToIntro()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MawaaiColors.DeepNight),
        contentAlignment = Alignment.Center
    ) {
        ParticleHeartSystem(particleCount = 5, modifier = Modifier.alpha(0.3f))

        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scaleAnim.value)
                .goldGlow(radius = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val s = size.minDimension * 0.8f
                val cx = w / 2
                val cy = h / 2

                val heartPath = Path().apply {
                    moveTo(cx, cy - s * 0.3f)
                    cubicTo(cx - s * 0.5f, cy - s * 0.8f, cx - s, cy - s * 0.3f, cx, cy + s * 0.4f)
                    cubicTo(cx + s, cy - s * 0.3f, cx + s * 0.5f, cy - s * 0.8f, cx, cy - s * 0.3f)
                    close()
                }
                drawPath(path = heartPath, color = MawaaiColors.DeepRose)

                val ringRadius = s * 0.2f
                val ringCx = cx + s * 0.35f
                val ringCy = cy - s * 0.1f
                drawCircle(
                    color = MawaaiColors.ChampagneGold,
                    radius = ringRadius,
                    center = androidx.compose.ui.geometry.Offset(ringCx, ringCy),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun SplashScreenPreview() {
    SplashScreen(onNavigateToIntro = {})
}
