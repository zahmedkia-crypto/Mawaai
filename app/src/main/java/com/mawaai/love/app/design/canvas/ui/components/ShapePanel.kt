package com.mawaai.love.app.design.canvas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.mawaai.love.app.design.canvas.model.ShapeSettings
import com.mawaai.love.app.design.canvas.model.ShapeType

@Composable
fun ShapePanel(
    settings: ShapeSettings,
    onChange: (ShapeSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.canvas_tool_shape),
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MawaaiColors.DesignTextLight
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShapeType.values().toList().chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { shape ->
                        val sel = settings.shape == shape
                        val border = if (sel) MawaaiColors.DesignGold else MawaaiColors.DesignGold.copy(alpha = 0.25f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MawaaiColors.DesignBgDark)
                                .border(if (sel) 2.dp else 1.dp, border, RoundedCornerShape(12.dp))
                                .clickable { onChange(settings.copy(shape = shape)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                shapeName(shape),
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Stroke", fontFamily = CairoFamily, modifier = Modifier.weight(1f), color = MawaaiColors.DesignTextLight)
            Text("${settings.strokeWidth.toInt()}", fontFamily = CairoFamily, color = MawaaiColors.DesignHennaLight)
        }
        Slider(
            value = settings.strokeWidth,
            onValueChange = { onChange(settings.copy(strokeWidth = it)) },
            valueRange = 1f..40f,
            colors = SliderDefaults.colors(thumbColor = MawaaiColors.DesignGold, activeTrackColor = MawaaiColors.DesignGold)
        )
        if (settings.shape == ShapeType.POLYGON) {
            Text("Sides ${settings.polygonSides}", fontFamily = CairoFamily, color = MawaaiColors.DesignTextLight)
            Slider(
                value = settings.polygonSides.toFloat(),
                onValueChange = { onChange(settings.copy(polygonSides = it.toInt().coerceIn(3, 12))) },
                valueRange = 3f..12f,
                colors = SliderDefaults.colors(thumbColor = MawaaiColors.DesignGold, activeTrackColor = MawaaiColors.DesignGold)
            )
        }
        if (settings.shape == ShapeType.STAR) {
            Text("Points ${settings.starPoints}", fontFamily = CairoFamily, color = MawaaiColors.DesignTextLight)
            Slider(
                value = settings.starPoints.toFloat(),
                onValueChange = { onChange(settings.copy(starPoints = it.toInt().coerceIn(3, 12))) },
                valueRange = 3f..12f,
                colors = SliderDefaults.colors(thumbColor = MawaaiColors.DesignGold, activeTrackColor = MawaaiColors.DesignGold)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Fill", fontFamily = CairoFamily, modifier = Modifier.weight(1f), color = MawaaiColors.DesignTextLight)
            Switch(
                checked = settings.fillColor != null,
                onCheckedChange = { fillOn ->
                    onChange(settings.copy(fillColor = if (fillOn) settings.strokeColor else null))
                },
                colors = SwitchDefaults.colors(checkedThumbColor = MawaaiColors.DesignGold)
            )
        }
    }
}

@Composable
private fun shapeName(shape: ShapeType): String = stringResource(
    when (shape) {
        ShapeType.LINE -> R.string.canvas_shape_line
        ShapeType.RECT -> R.string.canvas_shape_rect
        ShapeType.CIRCLE -> R.string.canvas_shape_circle
        ShapeType.POLYGON -> R.string.canvas_shape_polygon
        ShapeType.STAR -> R.string.canvas_shape_star
    }
)
