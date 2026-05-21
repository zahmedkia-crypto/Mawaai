package com.mawaai.love.app.design.showcase.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.mawaai.love.app.design.showcase.domain.ShowcaseLighting

@Composable
fun LightingOverlay(lighting: ShowcaseLighting, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        when (lighting) {
            ShowcaseLighting.NATURAL -> {
                // Soft top-down vignette
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x55000000)),
                        center = Offset(size.width / 2, size.height * 0.6f),
                        radius = size.width * 0.85f
                    )
                )
            }
            ShowcaseLighting.WARM -> {
                drawRect(color = Color(0xFFFFB47A).copy(alpha = 0.18f))
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x40FFE4B5), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.35f),
                        radius = size.width * 0.55f
                    )
                )
            }
            ShowcaseLighting.COOL -> {
                drawRect(color = Color(0xFF7AA3FF).copy(alpha = 0.15f))
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0x33B0CFFF), Color.Transparent),
                        start = Offset(size.width, 0f),
                        end = Offset(0f, size.height)
                    )
                )
            }
            ShowcaseLighting.DRAMATIC -> {
                // Heavy vignette
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000)),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.width * 0.65f
                    )
                )
                // Dramatic spotlight
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x66FFE4B5), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.4f),
                        radius = size.width * 0.30f
                    )
                )
            }
        }
    }
}
