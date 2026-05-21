package com.mawaai.love.app.design.presentation.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.design.domain.model.ConversionStyle
import com.mawaai.love.app.design.domain.model.FabricTone
import com.mawaai.love.app.design.domain.model.SkinTone
import com.mawaai.love.app.design.presentation.main.DesignRoute

@Composable
fun SuggestionsScreen(
    nav: NavController,
    sessionId: String,
    viewModel: SuggestionsViewModel = hiltViewModel()
) {
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.palette == TonePalette.SKIN && state.skinTones.isNotEmpty()) {
                item {
                    SectionTitle(stringResource(R.string.suggestions_pick_skin_tone))
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(state.skinTones, key = { it.id }) { tone ->
                            ToneSwatch(
                                color = Color(tone.argb),
                                label = stringResource(tone.labelRes()),
                                selected = state.selectedSkinToneId == tone.id,
                                onClick = { viewModel.selectSkinTone(tone.id) }
                            )
                        }
                    }
                }
            }

            if (state.palette == TonePalette.FABRIC && state.fabricTones.isNotEmpty()) {
                item {
                    SectionTitle(stringResource(R.string.suggestions_pick_fabric_tone))
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(state.fabricTones, key = { it.id }) { tone ->
                            ToneSwatch(
                                color = Color(tone.argb),
                                label = stringResource(tone.labelRes()),
                                selected = state.selectedFabricToneId == tone.id,
                                onClick = { viewModel.selectFabricTone(tone.id) }
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle(stringResource(R.string.suggestions_pick_style))
            }
            items(state.styles, key = { it.id }) { style ->
                StyleSuggestionRow(
                    style = style,
                    selected = state.selectedStyleId == style.id,
                    onClick = { viewModel.selectStyle(style.id) }
                )
            }
        }

        Button(
            onClick = {
                viewModel.persist(sessionId)
                nav.navigate(DesignRoute.Processing.create(sessionId))
            },
            enabled = state.canContinue,
            colors = ButtonDefaults.buttonColors(
                containerColor = MawaaiColors.DesignGold,
                contentColor = MawaaiColors.DesignBgDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.action_continue),
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontFamily = CairoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = MawaaiColors.DesignTextLight
    )
}

@Composable
private fun ToneSwatch(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MawaaiColors.DesignGold else MawaaiColors.DesignGold.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontFamily = CairoFamily,
            fontSize = 11.sp,
            color = MawaaiColors.DesignTextLight,
            maxLines = 1
        )
    }
}

@Composable
private fun StyleSuggestionRow(
    style: ConversionStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MawaaiColors.DesignGold else MawaaiColors.DesignGold.copy(alpha = 0.25f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MawaaiColors.DesignSurface)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = style.nameAr,
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MawaaiColors.DesignTextLight
            )
            Text(
                text = style.descriptionAr,
                fontFamily = CairoFamily,
                fontSize = 12.sp,
                color = MawaaiColors.DesignHennaLight
            )
        }
    }
}

private fun SkinTone.labelRes(): Int = when (this) {
    SkinTone.LIGHT -> R.string.skin_tone_light
    SkinTone.MEDIUM_LIGHT -> R.string.skin_tone_medium_light
    SkinTone.MEDIUM -> R.string.skin_tone_medium
    SkinTone.MEDIUM_DARK -> R.string.skin_tone_medium_dark
    SkinTone.DEEP -> R.string.skin_tone_deep
}

private fun FabricTone.labelRes(): Int = when (this) {
    FabricTone.WHITE -> R.string.fabric_tone_white
    FabricTone.BEIGE -> R.string.fabric_tone_beige
    FabricTone.GOLD -> R.string.fabric_tone_gold
    FabricTone.NAVY -> R.string.fabric_tone_navy
    FabricTone.BLACK -> R.string.fabric_tone_black
    FabricTone.BURGUNDY -> R.string.fabric_tone_burgundy
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun SuggestionsScreenPreview() {
    SuggestionsScreen(nav = rememberNavController(), sessionId = "preview")
}
