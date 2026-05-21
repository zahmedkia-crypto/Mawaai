package com.mawaai.love.app.design.canvas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.design.canvas.model.ToolType

internal enum class Sheet { NONE, BRUSHES, BRUSH_OPTIONS, LAYERS, SYMMETRY, SHAPES }

@Composable
internal fun CanvasToolbar(
    tool: ToolType,
    onTool: (ToolType) -> Unit,
    color: Color,
    onColorClick: () -> Unit,
    onSheet: (Sheet) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MawaaiColors.DesignSurface)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolButton(Icons.Default.Brush, R.string.canvas_tool_brush, tool == ToolType.BRUSH) { onTool(ToolType.BRUSH); onSheet(Sheet.BRUSHES) }
        ToolButton(Icons.Default.AutoFixNormal, R.string.canvas_tool_eraser, tool == ToolType.ERASER) { onTool(ToolType.ERASER); onSheet(Sheet.BRUSH_OPTIONS) }
        ToolButton(Icons.Default.FormatColorFill, R.string.canvas_tool_fill, tool == ToolType.FILL) { onTool(ToolType.FILL) }
        ToolButton(Icons.Default.FormatShapes, R.string.canvas_tool_shape, tool == ToolType.SHAPE) { onTool(ToolType.SHAPE); onSheet(Sheet.SHAPES) }
        ToolButton(Icons.Default.Colorize, R.string.canvas_tool_eyedropper, tool == ToolType.EYEDROPPER) { onTool(ToolType.EYEDROPPER) }
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, MawaaiColors.DesignGold, CircleShape)
                .clickable(onClick = onColorClick)
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = { onSheet(Sheet.BRUSH_OPTIONS) }) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.canvas_brushes), tint = MawaaiColors.DesignTextLight)
        }
        IconButton(onClick = { onSheet(Sheet.LAYERS) }) {
            Icon(Icons.Default.Layers, contentDescription = stringResource(R.string.canvas_layers), tint = MawaaiColors.DesignTextLight)
        }
        IconButton(onClick = { onSheet(Sheet.SYMMETRY) }) {
            Icon(Icons.Default.Palette, contentDescription = stringResource(R.string.canvas_symmetry), tint = MawaaiColors.DesignTextLight)
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, labelRes: Int, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) MawaaiColors.DesignGold else MawaaiColors.DesignTextLight
    val bg = if (selected) MawaaiColors.DesignGold.copy(alpha = 0.18f) else Color.Transparent
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(stringResource(labelRes), fontFamily = CairoFamily, fontSize = 10.sp, color = tint)
    }
}
