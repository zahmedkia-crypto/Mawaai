package com.mawaai.love.app.design.canvas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.mawaai.love.app.design.canvas.model.SymmetryMode

@Composable
fun SymmetryPanel(
    selected: SymmetryMode,
    onSelect: (SymmetryMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            stringResource(R.string.canvas_symmetry),
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MawaaiColors.DesignTextLight
        )
        Spacer(Modifier.height(12.dp))
        FlowGrid(
            items = SymmetryMode.values().toList(),
            isSelected = { it == selected },
            label = { name(it) },
            onClick = onSelect
        )
    }
}

@Composable
private fun name(mode: SymmetryMode): String = stringResource(
    when (mode) {
        SymmetryMode.OFF -> R.string.canvas_symmetry_off
        SymmetryMode.VERTICAL -> R.string.canvas_symmetry_v
        SymmetryMode.HORIZONTAL -> R.string.canvas_symmetry_h
        SymmetryMode.RADIAL_2 -> R.string.canvas_symmetry_2fold
        SymmetryMode.RADIAL_4 -> R.string.canvas_symmetry_4fold
        SymmetryMode.RADIAL_6 -> R.string.canvas_symmetry_6fold
        SymmetryMode.RADIAL_8 -> R.string.canvas_symmetry_8fold
    }
)

@Composable
private fun <T> FlowGrid(
    items: List<T>,
    isSelected: (T) -> Boolean,
    label: @Composable (T) -> String,
    onClick: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    val sel = isSelected(item)
                    val border = if (sel) MawaaiColors.DesignGold else MawaaiColors.DesignGold.copy(alpha = 0.25f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MawaaiColors.DesignBgDark)
                            .border(if (sel) 2.dp else 1.dp, border, RoundedCornerShape(12.dp))
                            .clickable { onClick(item) }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label(item),
                            fontFamily = CairoFamily,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = MawaaiColors.DesignTextLight
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
