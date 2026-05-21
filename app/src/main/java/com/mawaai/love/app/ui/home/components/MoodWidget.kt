package com.mawaai.love.app.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.core.theme.HeartSpring
import com.mawaai.love.app.data.model.MoodType

@Composable
fun MoodWidget(selectedMood: MoodType?, onMoodSelected: (MoodType) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "كيف حالكِ اليوم؟",
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MawaaiColors.RoseGold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MoodType.entries.forEach { mood ->
                val isSelected = mood == selectedMood
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.4f else 1f,
                    animationSpec = HeartSpring,
                    label = "scale"
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(if (isSelected) MawaaiColors.CardElevated else Color.Transparent)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) MawaaiColors.ChampagneGold else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onMoodSelected(mood) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = mood.emoji, fontSize = 24.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = mood.label,
                        fontFamily = CairoFamily,
                        fontSize = 10.sp,
                        color = if (isSelected) MawaaiColors.TextPrimary else MawaaiColors.TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
