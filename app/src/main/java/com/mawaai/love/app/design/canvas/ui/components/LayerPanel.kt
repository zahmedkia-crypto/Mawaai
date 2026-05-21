package com.mawaai.love.app.design.canvas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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
import com.mawaai.love.app.design.canvas.model.BlendMode
import com.mawaai.love.app.design.canvas.model.Layer

@Composable
fun LayerPanel(
    layers: List<Layer>,
    activeId: Int,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit,
    onDuplicate: (Int) -> Unit,
    onMergeDown: (Int) -> Unit,
    onToggleVisible: (Int, Boolean) -> Unit,
    onOpacity: (Int, Float) -> Unit,
    onBlend: (Int, BlendMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.canvas_layers),
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MawaaiColors.DesignTextLight,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.canvas_layer_new), tint = MawaaiColors.DesignGold)
            }
        }
        Spacer(Modifier.height(8.dp))
        // Layers shown top-most first (visual stacking) — render reversed
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(layers.reversed(), key = { it.id }) { layer ->
                LayerRow(
                    layer = layer,
                    selected = layer.id == activeId,
                    onSelect = onSelect,
                    onDelete = onDelete,
                    onDuplicate = onDuplicate,
                    onMergeDown = onMergeDown,
                    onToggleVisible = onToggleVisible,
                    onOpacity = onOpacity,
                    onBlend = onBlend
                )
            }
        }
    }
}

@Composable
private fun LayerRow(
    layer: Layer,
    selected: Boolean,
    onSelect: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onDuplicate: (Int) -> Unit,
    onMergeDown: (Int) -> Unit,
    onToggleVisible: (Int, Boolean) -> Unit,
    onOpacity: (Int, Float) -> Unit,
    onBlend: (Int, BlendMode) -> Unit
) {
    var blendMenuOpen by remember { mutableStateOf(false) }
    val borderColor = if (selected) MawaaiColors.DesignGold else MawaaiColors.DesignGold.copy(alpha = 0.25f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MawaaiColors.DesignBgDark)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onSelect(layer.id) }
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onToggleVisible(layer.id, !layer.visible) }) {
                Icon(
                    imageVector = if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = if (layer.visible) MawaaiColors.DesignGold else MawaaiColors.DesignTextLight.copy(alpha = 0.4f)
                )
            }
            Text(
                layer.name,
                fontFamily = CairoFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                color = MawaaiColors.DesignTextLight,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onDuplicate(layer.id) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.canvas_layer_duplicate), tint = MawaaiColors.DesignTextLight)
            }
            IconButton(onClick = { onMergeDown(layer.id) }) {
                Icon(Icons.Default.Merge, contentDescription = stringResource(R.string.canvas_layer_merge), tint = MawaaiColors.DesignTextLight)
            }
            IconButton(onClick = { onDelete(layer.id) }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.canvas_layer_delete), tint = MawaaiColors.DesignTextLight)
            }
        }
        Slider(
            value = layer.opacity,
            onValueChange = { onOpacity(layer.id, it) },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MawaaiColors.DesignGold,
                activeTrackColor = MawaaiColors.DesignGold
            )
        )
        Box {
            TextButton(onClick = { blendMenuOpen = true }) {
                Text(
                    "${stringResource(R.string.canvas_blend)}: ${blendName(layer.blend)}",
                    fontFamily = CairoFamily,
                    color = MawaaiColors.DesignGold
                )
            }
            DropdownMenu(expanded = blendMenuOpen, onDismissRequest = { blendMenuOpen = false }) {
                BlendMode.values().forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(blendName(mode), fontFamily = CairoFamily) },
                        onClick = { onBlend(layer.id, mode); blendMenuOpen = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun blendName(mode: BlendMode): String = stringResource(
    when (mode) {
        BlendMode.NORMAL -> R.string.canvas_blend_normal
        BlendMode.MULTIPLY -> R.string.canvas_blend_multiply
        BlendMode.OVERLAY -> R.string.canvas_blend_overlay
        BlendMode.SCREEN -> R.string.canvas_blend_screen
        BlendMode.SOFT_LIGHT -> R.string.canvas_blend_soft_light
    }
)
