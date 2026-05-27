package com.mawaai.love.app.design.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.design.ai.suggestions.Suggestion

@Composable
fun CreativeSuggestionCard(
    suggestion: Suggestion,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    val borderColor = if (isSelected) MawaaiColors.DesignGold else MawaaiColors.DesignGold.copy(alpha = 0.2f)
    val bgColor = if (isSelected) MawaaiColors.DesignSurface.copy(alpha = 0.9f) else MawaaiColors.DesignSurface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MawaaiColors.DesignGold.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(suggestion.category),
                        contentDescription = null,
                        tint = MawaaiColors.DesignGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suggestion.title,
                        fontFamily = CairoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MawaaiColors.DesignTextLight
                    )
                    Text(
                        text = suggestion.principle,
                        fontFamily = CairoFamily,
                        fontSize = 12.sp,
                        color = MawaaiColors.DesignHennaLight
                    )
                }

                // Selection Toggle
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
                        contentDescription = "Accept Suggestion",
                        tint = if (isSelected) MawaaiColors.DesignGold else MawaaiColors.DesignTextLight.copy(alpha = 0.3f)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MawaaiColors.DesignGold.copy(alpha = 0.1f))
                    Spacer(Modifier.height(12.dp))
                    
                    Text(
                        text = suggestion.explanation,
                        fontFamily = CairoFamily,
                        fontSize = 14.sp,
                        color = MawaaiColors.DesignTextLight.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )

                    if (suggestion.culturalContext.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MawaaiColors.DesignGold.copy(alpha = 0.05f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MawaaiColors.DesignGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = suggestion.culturalContext,
                                fontFamily = CairoFamily,
                                fontSize = 11.sp,
                                color = MawaaiColors.DesignHennaLight
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ImpactBadge(impact = suggestion.impact)
                        if (suggestion.autoFixable) {
                            Spacer(Modifier.width(8.dp))
                            AutoFixBadge()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImpactBadge(impact: Int) {
    Surface(
        color = MawaaiColors.DesignGold.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "Impact: $impact%",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MawaaiColors.DesignGold,
            fontFamily = CairoFamily
        )
    }
}

@Composable
private fun AutoFixBadge() {
    Surface(
        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "AI Auto-Fix",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50),
            fontFamily = CairoFamily
        )
    }
}

private fun getCategoryIcon(category: Suggestion.Category): ImageVector = when (category) {
    Suggestion.Category.LINE -> Icons.Default.Lightbulb
    Suggestion.Category.SYMMETRY -> Icons.Default.AutoAwesome
    Suggestion.Category.TEMPLATE -> Icons.Default.Info
    Suggestion.Category.CULTURAL -> Icons.Default.AutoAwesome
    Suggestion.Category.PRINT -> Icons.Default.Info
    Suggestion.Category.COLOR -> Icons.Default.Lightbulb
}
