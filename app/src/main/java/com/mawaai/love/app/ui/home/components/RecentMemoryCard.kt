package com.mawaai.love.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mawaai.love.app.core.components.RoseGlassCard
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.core.utils.DateUtils
import com.mawaai.love.app.data.model.Memory

@Composable
fun RecentMemoryCard(memory: Memory?, onClick: () -> Unit) {
    if (memory == null) return

    RoseGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        onClick = onClick
    ) {
        Column {
            Text(
                text = "آخر ذكرياتنا",
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MawaaiColors.RoseGold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = memory.imagePath,
                    contentDescription = memory.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MawaaiColors.DeepNight.copy(alpha = 0.8f))
                            )
                        )
                )
                
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = memory.title,
                        fontFamily = CairoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = DateUtils.formatArabicDate(memory.date),
                        fontFamily = CairoFamily,
                        fontSize = 12.sp,
                        color = MawaaiColors.TextSecondary
                    )
                }
            }
        }
    }
}
