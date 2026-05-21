package com.mawaai.love.app.design.canvas.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ColorPickerDialog(
    initial: Color,
    palette: List<Color>,
    recents: List<Color>,
    onConfirm: (Color) -> Unit,
    onAddToPalette: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val hsv = remember {
        val arr = FloatArray(3)
        AndroidColor.colorToHSV(initial.toArgbInt(), arr)
        arr
    }
    // Float-state instead of generic mutableStateOf — avoids autoboxing
    // each value write/read (Phase 6 lint cleanup, AutoboxingStateCreation).
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    var alpha by remember { mutableFloatStateOf(initial.alpha) }
    var hexInput by remember { mutableStateOf(initial.toHexString()) }

    val current = remember(hue, sat, value, alpha) {
        val argb = AndroidColor.HSVToColor((alpha * 255f).toInt().coerceIn(0, 255), floatArrayOf(hue, sat, value))
        Color(argb)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MawaaiColors.DesignSurface,
        title = {
            Text(
                stringResource(R.string.canvas_colors),
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold,
                color = MawaaiColors.DesignTextLight
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Big preview swatch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(current)
                            .border(1.dp, MawaaiColors.DesignGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "HSB(${(hue).toInt()}°, ${(sat * 100).toInt()}%, ${(value * 100).toInt()}%)",
                            fontFamily = CairoFamily, color = MawaaiColors.DesignTextLight
                        )
                        Text(
                            "RGB(${current.redInt()}, ${current.greenInt()}, ${current.blueInt()})",
                            fontFamily = CairoFamily, color = MawaaiColors.DesignHennaLight
                        )
                    }
                }

                // HSB Wheel + Value square
                HSBWheel(
                    hue = hue,
                    saturation = sat,
                    value = value,
                    onChanged = { h, s, v -> hue = h; sat = s; value = v }
                )

                // Sliders for each channel
                LabeledSlider("H", hue / 360f, 0f..1f) { hue = (it * 360f).coerceIn(0f, 360f) }
                LabeledSlider("S", sat, 0f..1f) { sat = it }
                LabeledSlider("V", value, 0f..1f) { value = it }
                LabeledSlider("A", alpha, 0f..1f) { alpha = it }

                // RGB sliders
                val rInt = current.redInt()
                val gInt = current.greenInt()
                val bInt = current.blueInt()
                LabeledSlider("R", rInt / 255f, 0f..1f) { applyRgb((it * 255).toInt(), gInt, bInt) { h, s, v -> hue = h; sat = s; value = v } }
                LabeledSlider("G", gInt / 255f, 0f..1f) { applyRgb(rInt, (it * 255).toInt(), bInt) { h, s, v -> hue = h; sat = s; value = v } }
                LabeledSlider("B", bInt / 255f, 0f..1f) { applyRgb(rInt, gInt, (it * 255).toInt()) { h, s, v -> hue = h; sat = s; value = v } }

                // Hex
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#", fontFamily = CairoFamily, color = MawaaiColors.DesignTextLight)
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { newValue ->
                            hexInput = newValue.uppercase()
                            runCatching {
                                val clean = newValue.removePrefix("#")
                                if (clean.length in 6..8) {
                                    val argbStr = if (clean.length == 6) "FF$clean" else clean
                                    val parsed = AndroidColor.parseColor("#$argbStr")
                                    val arr = FloatArray(3)
                                    AndroidColor.colorToHSV(parsed, arr)
                                    hue = arr[0]; sat = arr[1]; value = arr[2]
                                    alpha = ((parsed ushr 24) and 0xFF) / 255f
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                }

                // Recent + palette
                if (recents.isNotEmpty()) {
                    Text(
                        stringResource(R.string.canvas_color_recents),
                        fontFamily = CairoFamily, color = MawaaiColors.DesignHennaLight
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(recents, key = { it.value.toLong() }) { c -> Swatch(c) { setHsv(c) { h, s, v, a -> hue = h; sat = s; value = v; alpha = a } } }
                    }
                }
                Text(
                    stringResource(R.string.canvas_color_palette),
                    fontFamily = CairoFamily, color = MawaaiColors.DesignHennaLight
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(palette, key = { it.value.toLong() }) { c -> Swatch(c) { setHsv(c) { h, s, v, a -> hue = h; sat = s; value = v; alpha = a } } }
                }
                TextButton(onClick = { onAddToPalette(current) }) {
                    Text("+ ${stringResource(R.string.canvas_color_palette)}", fontFamily = CairoFamily, color = MawaaiColors.DesignGold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current) }) {
                Text(stringResource(R.string.canvas_done), fontFamily = CairoFamily, color = MawaaiColors.DesignGold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), fontFamily = CairoFamily, color = MawaaiColors.DesignTextLight)
            }
        }
    )
}

private fun applyRgb(r: Int, g: Int, b: Int, set: (Float, Float, Float) -> Unit) {
    val arr = FloatArray(3)
    AndroidColor.RGBToHSV(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255), arr)
    set(arr[0], arr[1], arr[2])
}

private fun setHsv(color: Color, set: (Float, Float, Float, Float) -> Unit) {
    val arr = FloatArray(3)
    AndroidColor.colorToHSV(color.toArgbInt(), arr)
    set(arr[0], arr[1], arr[2], color.alpha)
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontFamily = CairoFamily, modifier = Modifier.width(20.dp), color = MawaaiColors.DesignTextLight)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MawaaiColors.DesignGold,
                activeTrackColor = MawaaiColors.DesignGold
            )
        )
    }
}

@Composable
private fun Swatch(c: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(c)
            .border(1.dp, MawaaiColors.DesignGold.copy(alpha = 0.4f), CircleShape)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun HSBWheel(
    hue: Float,
    saturation: Float,
    value: Float,
    onChanged: (Float, Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val sizeDp = 200.dp
    val sizePx = with(density) { sizeDp.toPx() }
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val p = change.position
                        val cx = sizePx / 2f
                        val cy = sizePx / 2f
                        val dx = p.x - cx
                        val dy = p.y - cy
                        val r = hypot(dx, dy)
                        val maxR = sizePx / 2f
                        if (r <= maxR) {
                            val ang = (atan2(dy, dx) * (180f / Math.PI.toFloat()) + 360f) % 360f
                            val sat = (r / maxR).coerceIn(0f, 1f)
                            onChanged(ang, sat, value)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { p ->
                        val cx = sizePx / 2f
                        val cy = sizePx / 2f
                        val dx = p.x - cx
                        val dy = p.y - cy
                        val r = hypot(dx, dy)
                        val maxR = sizePx / 2f
                        if (r <= maxR) {
                            val ang = (atan2(dy, dx) * (180f / Math.PI.toFloat()) + 360f) % 360f
                            val sat = (r / maxR).coerceIn(0f, 1f)
                            onChanged(ang, sat, value)
                        }
                    }
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = size.width / 2f
            val steps = 60
            for (a in 0 until 360 step 360 / steps) {
                val angRad = (a.toDouble() * Math.PI / 180.0)
                val nextAngRad = ((a + 360 / steps).toDouble() * Math.PI / 180.0)
                val color = Color(AndroidColor.HSVToColor(floatArrayOf(a.toFloat(), 1f, value)))
                drawArc(
                    brush = SolidColor(color),
                    startAngle = a.toFloat(),
                    sweepAngle = (360f / steps),
                    useCenter = true,
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height)
                )
            }
            // White center fade for saturation
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = Offset(cx, cy),
                    radius = maxR
                ),
                radius = maxR,
                center = Offset(cx, cy)
            )
            // Cursor
            val cursorR = saturation * maxR
            val ang = hue * (Math.PI / 180.0)
            val px = cx + (cursorR * cos(ang)).toFloat()
            val py = cy + (cursorR * sin(ang)).toFloat()
            drawCircle(color = Color.White, radius = 8f, center = Offset(px, py))
            drawCircle(color = Color.Black, radius = 8f, center = Offset(px, py), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
        }
    }
}

private fun Color.toArgbInt(): Int {
    val a = (alpha * 255f).toInt() and 0xFF
    val r = (red * 255f).toInt() and 0xFF
    val g = (green * 255f).toInt() and 0xFF
    val b = (blue * 255f).toInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

private fun Color.redInt(): Int = (red * 255f).toInt().coerceIn(0, 255)
private fun Color.greenInt(): Int = (green * 255f).toInt().coerceIn(0, 255)
private fun Color.blueInt(): Int = (blue * 255f).toInt().coerceIn(0, 255)

private fun Color.toHexString(): String {
    val a = (alpha * 255f).toInt().coerceIn(0, 255)
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X%02X".format(a, r, g, b)
}
