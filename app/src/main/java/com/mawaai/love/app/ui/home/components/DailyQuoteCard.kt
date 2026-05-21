package com.mawaai.love.app.ui.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.core.components.RoseGlassCard
import com.mawaai.love.app.core.theme.AmiriFamily
import com.mawaai.love.app.core.theme.MawaaiColors

@Composable
fun DailyQuoteCard(quote: String, onShare: (String) -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val offsetX by animateDpAsState(
        targetValue = if (startAnimation) 0.dp else 100.dp,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "offset"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1000),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    RoseGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .graphicsLayer(translationX = offsetX.value.toFloat())
            .alpha(alpha)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FormatQuote,
                contentDescription = null,
                tint = MawaaiColors.ChampagneGold,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = quote,
                fontFamily = AmiriFamily,
                fontSize = 18.sp,
                color = MawaaiColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            IconButton(onClick = { onShare(quote) }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Quote",
                    tint = MawaaiColors.RoseGold
                )
            }
        }
    }
}
