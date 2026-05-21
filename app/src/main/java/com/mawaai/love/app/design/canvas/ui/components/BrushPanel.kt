package com.mawaai.love.app.design.canvas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.design.canvas.model.BrushPresetCatalog
import com.mawaai.love.app.design.canvas.model.BrushType

@Composable
fun BrushPanel(
    selected: BrushType,
    onSelect: (BrushType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            stringResource(R.string.canvas_brushes),
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MawaaiColors.DesignTextLight
        )
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(BrushPresetCatalog.all, key = { it.type.name }) { preset ->
                BrushTile(
                    name = stringResource(preset.nameRes),
                    selected = preset.type == selected,
                    onClick = { onSelect(preset.type) }
                )
            }
        }
    }
}

@Composable
private fun BrushTile(name: String, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) MawaaiColors.DesignGold else MawaaiColors.DesignGold.copy(alpha = 0.25f)
    Box(
        modifier = Modifier
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MawaaiColors.DesignBgDark)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            fontFamily = CairoFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
            color = MawaaiColors.DesignTextLight
        )
    }
}
