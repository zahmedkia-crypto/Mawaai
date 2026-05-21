package com.mawaai.love.app.ui.memories.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.core.utils.DateUtils
import com.mawaai.love.app.data.model.Memory

@Composable
fun MemoryCard(
    memory: Memory,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500),
        label = "alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MawaaiColors.CardDark)
    ) {
        Box {
            AsyncImage(
                model = memory.imagePath,
                contentDescription = memory.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentScale = ContentScale.FillWidth
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MawaaiColors.SurfaceDark.copy(alpha = 0.7f)),
                            startY = 100f
                        )
                    )
            )

            // Favorite Icon
            Icon(
                imageVector = if (memory.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (memory.isFavorite) MawaaiColors.DeepRose else MawaaiColors.PearlWhite,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(20.dp)
            )

            // Info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = memory.title,
                    fontFamily = CairoFamily,
                    fontSize = 14.sp,
                    color = MawaaiColors.PearlWhite,
                    maxLines = 1
                )
                Text(
                    text = DateUtils.formatArabicDate(memory.date),
                    fontFamily = CairoFamily,
                    fontSize = 10.sp,
                    color = MawaaiColors.TextSecondary
                )
            }
        }
    }
}
