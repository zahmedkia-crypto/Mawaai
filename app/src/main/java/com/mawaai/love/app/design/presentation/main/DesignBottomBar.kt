package com.mawaai.love.app.design.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors

enum class DesignTab(val route: String) {
    SPECIALIZED(DesignRoute.SpecializedHome.route),
    CONVERTER(DesignRoute.ConverterHome.route)
}

@Composable
fun DesignBottomBar(
    current: DesignTab,
    onSelect: (DesignTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MawaaiColors.DesignSurface)
            // Keep the tab row above the gesture / 3-button system nav bar
            // while letting the DesignSurface background extend under it.
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DesignTabButton(
            icon = Icons.Default.GridView,
            label = stringResource(R.string.design_tab_specialized),
            selected = current == DesignTab.SPECIALIZED,
            onClick = { onSelect(DesignTab.SPECIALIZED) }
        )
        DesignTabButton(
            icon = Icons.Default.AutoAwesome,
            label = stringResource(R.string.design_tab_converter),
            selected = current == DesignTab.CONVERTER,
            onClick = { onSelect(DesignTab.CONVERTER) }
        )
    }
}

@Composable
private fun DesignTabButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) MawaaiColors.DesignGold.copy(alpha = 0.18f) else MawaaiColors.DesignSurface
    val tint = if (selected) MawaaiColors.DesignGold else MawaaiColors.DesignTextLight.copy(alpha = 0.6f)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontFamily = CairoFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
            color = tint
        )
    }
}
