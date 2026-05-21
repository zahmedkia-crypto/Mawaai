package com.mawaai.love.app.design.canvas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.design.canvas.model.BrushSettings

@Composable
fun BrushOptionsPanel(
    brush: BrushSettings,
    onChange: (BrushSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Header(stringResource(R.string.canvas_brushes))
        Slider(
            label = stringResource(R.string.canvas_size),
            display = "${brush.size.toInt()}",
            value = brush.size,
            range = 1f..200f,
            onValue = { onChange(brush.copy(size = it)) }
        )
        Slider(
            label = stringResource(R.string.canvas_opacity),
            display = "${(brush.opacity * 100).toInt()}%",
            value = brush.opacity,
            range = 0f..1f,
            onValue = { onChange(brush.copy(opacity = it)) }
        )
        Slider(
            label = stringResource(R.string.canvas_hardness),
            display = "${(brush.hardness * 100).toInt()}%",
            value = brush.hardness,
            range = 0f..1f,
            onValue = { onChange(brush.copy(hardness = it)) }
        )
        Slider(
            label = stringResource(R.string.canvas_spacing),
            display = "${(brush.spacing * 100).toInt()}%",
            value = brush.spacing,
            range = 0.01f..2f,
            onValue = { onChange(brush.copy(spacing = it)) }
        )
        Slider(
            label = stringResource(R.string.canvas_scatter),
            display = "${(brush.scatter * 100).toInt()}%",
            value = brush.scatter,
            range = 0f..1f,
            onValue = { onChange(brush.copy(scatter = it)) }
        )
        Slider(
            label = stringResource(R.string.canvas_jitter),
            display = "${(brush.jitter * 100).toInt()}%",
            value = brush.jitter,
            range = 0f..1f,
            onValue = { onChange(brush.copy(jitter = it)) }
        )
        Slider(
            label = stringResource(R.string.canvas_flow),
            display = "${(brush.flow * 100).toInt()}%",
            value = brush.flow,
            range = 0f..1f,
            onValue = { onChange(brush.copy(flow = it)) }
        )
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        fontFamily = CairoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = MawaaiColors.DesignTextLight
    )
}

@Composable
private fun Slider(
    label: String,
    display: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValue: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                fontFamily = CairoFamily,
                fontSize = 13.sp,
                color = MawaaiColors.DesignTextLight,
                modifier = Modifier.weight(1f)
            )
            Text(
                display,
                fontFamily = CairoFamily,
                fontSize = 13.sp,
                color = MawaaiColors.DesignHennaLight
            )
        }
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MawaaiColors.DesignGold,
                activeTrackColor = MawaaiColors.DesignGold
            )
        )
    }
}
