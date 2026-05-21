package com.mawaai.love.app.core.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mawaai.love.app.core.theme.MawaaiColors

@Composable
fun GoldDivider(
    modifier: Modifier = Modifier,
    targetWidth: Dp = 120.dp
) {
    var startAnimation by remember { mutableStateOf(false) }
    val animatedWidth by animateDpAsState(
        targetValue = if (startAnimation) targetWidth else 0.dp,
        animationSpec = tween(1000),
        label = "width"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width = animatedWidth, height = 20.dp)) {
            val w = size.width
            val h = size.height
            val centerY = h / 2

            // Gold line
            drawLine(
                brush = MawaaiColors.GradGold,
                start = Offset(0f, centerY),
                end = Offset(w, centerY),
                strokeWidth = 1.dp.toPx()
            )

            // Diamond in the middle
            val diamondSize = 6.dp.toPx()
            val diamondPath = Path().apply {
                moveTo(w / 2, centerY - diamondSize)
                lineTo(w / 2 + diamondSize, centerY)
                lineTo(w / 2, centerY + diamondSize)
                lineTo(w / 2 - diamondSize, centerY)
                close()
            }
            drawPath(path = diamondPath, brush = MawaaiColors.GradGold)
        }
    }
}
