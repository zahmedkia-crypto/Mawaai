package com.mawaai.love.app.core.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.core.theme.AmiriFamily
import com.mawaai.love.app.core.theme.MawaaiColors

@Composable
fun LoadingHeart() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_heart")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier
            .size(80.dp)
            .scale(scale)) {
            val s = size.minDimension
            val x = size.width / 2
            val y = size.height / 2
            
            val path = Path().apply {
                moveTo(x, y - s * 0.3f)
                cubicTo(x - s * 0.5f, y - s * 0.8f, x - s, y - s * 0.3f, x, y + s * 0.4f)
                cubicTo(x + s, y - s * 0.3f, x + s * 0.5f, y - s * 0.8f, x, y - s * 0.3f)
                close()
            }
            drawPath(
                path = path,
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(MawaaiColors.SoftRose, MawaaiColors.DeepRose)
                )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "لحظة...",
            fontFamily = AmiriFamily,
            color = MawaaiColors.RoseGold,
            fontSize = 20.sp
        )
    }
}
