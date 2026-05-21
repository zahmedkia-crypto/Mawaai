package com.mawaai.love.app.design.presentation.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mawaai.love.app.R
import com.mawaai.love.app.core.responsive.isWideWidth
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.design.domain.model.FabricTone
import com.mawaai.love.app.design.domain.model.HslColor
import com.mawaai.love.app.design.presentation.main.DesignRoute
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import android.graphics.Color as AndroidColor

/**
 * Slider units used by [PickerSlider] to format the value label. Both axes
 * round to whole numbers; only the suffix and divisor differ.
 */
private enum class SliderUnit { DEGREES, PERCENT, RAW_BYTE }

/**
 * Two-tab color model selector. HSL is the legacy default — perceptually
 * intuitive for fabric (lightness preserves folds). RGB is the user's
 * Phase 19 request — direct 0-255 sliders for designers who think in
 * RGB. Both feed the same underlying [HslColor] state so the engine
 * call signature is unchanged.
 */
private enum class ColorPickerMode { HSL, RGB }

@Composable
fun CustomizeScreen(
    nav: NavController,
    sessionId: String,
    viewModel: CustomizeViewModel = hiltViewModel()
) {
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.nav.collect {
            nav.navigate(DesignRoute.Result.create(sessionId))
        }
    }

    // Phase 24: tablet / foldable / phone-landscape layouts get the
    // preview on one side and controls on the other. Compact (portrait
    // phone) keeps the original stacked column. Same Composables on
    // both axes — the split is purely a layout decision.
    val wide = isWideWidth
    if (wide) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Left: locked preview that doesn't scroll with the controls.
            Column(
                modifier = Modifier.weight(1f).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.customize_title),
                    fontFamily = CairoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MawaaiColors.DesignTextLight
                )
                PreviewBox(state = state)
                state.errorMessage
                    ?.takeIf { state.previewUri == null }
                    ?.let { ErrorRow(message = it) }
            }
            // Right: scrollable controls.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CustomizeControls(state = state, viewModel = viewModel)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.customize_title),
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MawaaiColors.DesignTextLight
            )
            PreviewBox(state = state)
            state.errorMessage
                ?.takeIf { state.previewUri == null }
                ?.let { ErrorRow(message = it) }
            CustomizeControls(state = state, viewModel = viewModel)
        }
    }
}

/**
 * Phase 24 — extracted controls block. Identical content on phone +
 * tablet; the only thing that varies is the parent container (single
 * scrollable column on phone, right-pane scrollable column on tablet).
 * Lifted into its own Composable to avoid duplicating the preset row,
 * hex swatch, slider tabs, and Save button across both branches.
 */
@Composable
private fun CustomizeControls(
    state: CustomizeUiState,
    viewModel: CustomizeViewModel
) {
    SectionLabel(stringResource(R.string.customize_presets_title))
    FabricPresetRow(
        selected = state.lastPickedPreset,
        onPick = { tone -> viewModel.setPreset(tone) }
    )
    HexSwatchRow(
        color = state.color,
        onColorChange = { viewModel.setColor(it) }
    )

    // Phase 19: HSL ↔ RGB tab toggle. Both views are bound to the same
    // [state.color], so flipping tabs after editing in one mode shows
    // the equivalent values in the other mode. The underlying engine
    // call shape is unchanged — RGB sliders convert via
    // `HslColor.fromColor(argb)` before pushing to the ViewModel.
    var pickerMode by remember { mutableStateOf(ColorPickerMode.HSL) }
    ColorModeTabs(selected = pickerMode, onSelect = { pickerMode = it })
    when (pickerMode) {
        ColorPickerMode.HSL -> HslSliders(
            color = state.color,
            onColorChange = { viewModel.setColor(it) }
        )
        ColorPickerMode.RGB -> RgbSliders(
            color = state.color,
            onColorChange = { viewModel.setColor(it) }
        )
    }
    Spacer(Modifier.height(4.dp))
    Button(
        onClick = { viewModel.save() },
        enabled = !state.isSaving && state.previewUri != null,
        colors = ButtonDefaults.buttonColors(
            containerColor = MawaaiColors.DesignGold,
            contentColor = MawaaiColors.DesignBgDark
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (state.isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.height(18.dp).width(18.dp),
                color = MawaaiColors.DesignBgDark,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
        } else {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = stringResource(R.string.customize_save),
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PreviewBox(state: CustomizeUiState) {
    val recoloringLabel = stringResource(R.string.customize_recoloring)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(MawaaiColors.DesignSurface)
            .border(1.dp, MawaaiColors.DesignGold.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        val previewUri = state.previewUri
        when {
            state.isLoading -> {
                CircularProgressIndicator(color = MawaaiColors.DesignGold)
            }
            previewUri != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(previewUri)
                        .crossfade(false)
                        .build(),
                    contentDescription = stringResource(R.string.customize_title),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MawaaiColors.DesignGold
                )
            }
        }

        if (state.isRecoloring && previewUri != null) {
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MawaaiColors.DesignBgDark.copy(alpha = 0.7f))
                    .semantics { contentDescription = recoloringLabel },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MawaaiColors.DesignGold,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorRow(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MawaaiColors.DesignHennaLight
        )
        Text(
            text = message,
            fontFamily = CairoFamily,
            color = MawaaiColors.DesignHennaLight,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontFamily = CairoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = MawaaiColors.DesignTextLight.copy(alpha = 0.85f)
    )
}

@Composable
private fun FabricPresetRow(
    selected: FabricTone?,
    onPick: (FabricTone) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FabricTone.entries.forEach { tone ->
            val isSelected = selected == tone
            val borderColor = if (isSelected) MawaaiColors.DesignGold else MawaaiColors.DesignGold.copy(alpha = 0.3f)
            val borderWidth = if (isSelected) 2.dp else 1.dp
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(tone.argb))
                    .border(borderWidth, borderColor, CircleShape)
                    .semantics {
                        role = Role.Button
                        contentDescription = tone.nameAr
                    }
                    .clickable { onPick(tone) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HexSwatchRow(
    color: HslColor,
    onColorChange: (HslColor) -> Unit
) {
    var hexText by remember { mutableStateOf(color.toHex()) }
    LaunchedEffect(color) {
        val canonical = color.toHex()
        if (!hexText.equals(canonical, ignoreCase = true)) {
            hexText = canonical
        }
    }
    val swatchA11y = stringResource(R.string.customize_hex_label) + " " + color.toHex()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(color.toArgb()))
                .border(1.dp, MawaaiColors.DesignGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .semantics { contentDescription = swatchA11y }
        )
        OutlinedTextField(
            value = hexText,
            onValueChange = { raw ->
                hexText = raw
                HslColor.fromHex(raw)?.let(onColorChange)
            },
            label = {
                Text(
                    text = stringResource(R.string.customize_hex_label),
                    fontFamily = CairoFamily,
                    fontSize = 12.sp
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MawaaiColors.DesignGold,
                unfocusedBorderColor = MawaaiColors.DesignGold.copy(alpha = 0.4f),
                focusedLabelColor = MawaaiColors.DesignGold,
                unfocusedLabelColor = MawaaiColors.DesignTextLight.copy(alpha = 0.6f),
                focusedTextColor = MawaaiColors.DesignTextLight,
                unfocusedTextColor = MawaaiColors.DesignTextLight,
                cursorColor = MawaaiColors.DesignGold
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ColorModeTabs(
    selected: ColorPickerMode,
    onSelect: (ColorPickerMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ColorModeTab(
            label = stringResource(R.string.customize_tab_hsl),
            selected = selected == ColorPickerMode.HSL,
            onClick = { onSelect(ColorPickerMode.HSL) },
            modifier = Modifier.weight(1f)
        )
        ColorModeTab(
            label = stringResource(R.string.customize_tab_rgb),
            selected = selected == ColorPickerMode.RGB,
            onClick = { onSelect(ColorPickerMode.RGB) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ColorModeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MawaaiColors.DesignGold.copy(alpha = 0.22f) else Color.Transparent
    val borderColor = if (selected) MawaaiColors.DesignGold else MawaaiColors.DesignGold.copy(alpha = 0.3f)
    val borderWidth = if (selected) 2.dp else 1.dp
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .semantics { role = Role.Tab }
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = CairoFamily,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MawaaiColors.DesignGold else MawaaiColors.DesignTextLight.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun HslSliders(
    color: HslColor,
    onColorChange: (HslColor) -> Unit
) {
    PickerSlider(
        label = stringResource(R.string.customize_hue),
        value = color.hue,
        valueRange = 0f..360f,
        unit = SliderUnit.DEGREES,
        onValueChange = { onColorChange(color.copy(hue = it)) }
    )
    PickerSlider(
        label = stringResource(R.string.customize_saturation),
        value = color.saturation,
        valueRange = 0f..1f,
        unit = SliderUnit.PERCENT,
        onValueChange = { onColorChange(color.copy(saturation = it)) }
    )
    PickerSlider(
        label = stringResource(R.string.customize_lightness),
        value = color.lightness,
        valueRange = 0f..1f,
        unit = SliderUnit.PERCENT,
        onValueChange = { onColorChange(color.copy(lightness = it)) }
    )
}

/**
 * RGB sliders 0-255. The current ARGB is decomposed once per recomposition
 * via [AndroidColor.red] / [green] / [blue], and slider drags reassemble
 * the new ARGB with full alpha and convert back through
 * [HslColor.fromColor]. Float-precision round-tripping is fine here —
 * the engine consumes whole bytes anyway.
 */
@Composable
private fun RgbSliders(
    color: HslColor,
    onColorChange: (HslColor) -> Unit
) {
    val argb = color.toArgb()
    val r = AndroidColor.red(argb)
    val g = AndroidColor.green(argb)
    val b = AndroidColor.blue(argb)
    PickerSlider(
        label = stringResource(R.string.customize_red),
        value = r.toFloat(),
        valueRange = 0f..255f,
        unit = SliderUnit.RAW_BYTE,
        onValueChange = { newR ->
            onColorChange(HslColor.fromColor(AndroidColor.argb(255, newR.toInt(), g, b)))
        }
    )
    PickerSlider(
        label = stringResource(R.string.customize_green),
        value = g.toFloat(),
        valueRange = 0f..255f,
        unit = SliderUnit.RAW_BYTE,
        onValueChange = { newG ->
            onColorChange(HslColor.fromColor(AndroidColor.argb(255, r, newG.toInt(), b)))
        }
    )
    PickerSlider(
        label = stringResource(R.string.customize_blue),
        value = b.toFloat(),
        valueRange = 0f..255f,
        unit = SliderUnit.RAW_BYTE,
        onValueChange = { newB ->
            onColorChange(HslColor.fromColor(AndroidColor.argb(255, r, g, newB.toInt())))
        }
    )
}

@Composable
private fun PickerSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: SliderUnit,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontFamily = CairoFamily,
                fontSize = 13.sp,
                color = MawaaiColors.DesignTextLight
            )
            Text(
                text = formatSliderValue(value, unit),
                fontFamily = CairoFamily,
                fontSize = 12.sp,
                color = MawaaiColors.DesignTextLight.copy(alpha = 0.7f)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MawaaiColors.DesignGold,
                activeTrackColor = MawaaiColors.DesignGold,
                inactiveTrackColor = MawaaiColors.DesignGold.copy(alpha = 0.25f)
            )
        )
    }
}

private fun formatSliderValue(value: Float, unit: SliderUnit): String = when (unit) {
    SliderUnit.DEGREES -> "${value.toInt()}°"
    SliderUnit.PERCENT -> "${(value * 100f).toInt()}%"
    SliderUnit.RAW_BYTE -> "${value.toInt()}"
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun CustomizeScreenPreview() {
    CustomizeScreen(nav = rememberNavController(), sessionId = "preview")
}
