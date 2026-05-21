package com.mawaai.love.app.ui.home.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors

sealed class NavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : NavItem("home", Icons.Default.Home, "الرئيسية")
    object Memories : NavItem("memories", Icons.Default.PhotoLibrary, "ذكريات")
    object Letters : NavItem("letters", Icons.Default.Email, "رسائل")
    object Mood : NavItem("mood", Icons.Default.Mood, "مزاجنا")
}

@Composable
fun MawaaiBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        NavItem.Home,
        NavItem.Memories,
        NavItem.Letters,
        NavItem.Mood
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, MawaaiColors.SurfaceDark)
                )
            )
            // Reserve room for the gesture / 3-button system navigation bar
            // INSIDE the gradient so the buttons sit above the OS bar but the
            // gradient still bleeds underneath it. Without this the icons end
            // up half-clipped by the nav bar in edge-to-edge mode.
            .navigationBarsPadding()
            .height(80.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(item.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) MawaaiColors.RoseGold else MawaaiColors.TextHint,
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = item.label,
                        fontFamily = CairoFamily,
                        fontSize = 10.sp,
                        color = if (isSelected) MawaaiColors.RoseGold else MawaaiColors.TextHint,
                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                    )

                    if (isSelected) {
                        val dotSize by animateDpAsState(targetValue = 4.dp, label = "dot")
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(dotSize)
                                .clip(CircleShape)
                                .background(MawaaiColors.ChampagneGold)
                        )
                    }
                }
            }
        }
    }
}
