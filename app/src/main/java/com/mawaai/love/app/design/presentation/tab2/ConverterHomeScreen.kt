package com.mawaai.love.app.design.presentation.tab2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.design.presentation.main.DesignRoute
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

@Composable
fun ConverterHomeScreen(
    nav: NavController,
    viewModel: ConverterHomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MawaaiColors.GradDesignAccent)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MawaaiColors.DesignTextLight
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.converter_headline),
                    fontFamily = CairoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MawaaiColors.DesignTextLight
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.converter_subheadline),
                    fontFamily = CairoFamily,
                    fontSize = 13.sp,
                    color = MawaaiColors.DesignTextLight.copy(alpha = 0.85f)
                )
            }
        }

        if (state.isLoading || state.prompts.isNotEmpty()) {
            InspirationSection(
                prompts = state.prompts,
                isLoading = state.isLoading,
                onRefresh = viewModel::loadPrompts
            )
        }

        com.mawaai.love.app.design.presentation.common.DesignActionCard(
            icon = Icons.Default.AutoAwesome,
            title = stringResource(R.string.converter_tip_anything),
            subtitle = stringResource(R.string.input_draw_subtitle),
            onClick = { nav.navigate(DesignRoute.ConverterInput.route) }
        )
    }
}

@Composable
private fun InspirationSection(
    prompts: List<String>,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.converter_inspiration_title),
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MawaaiColors.DesignTextLight,
                modifier = Modifier.weight(1f)
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MawaaiColors.DesignGold,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.converter_inspiration_refresh),
                        tint = MawaaiColors.DesignGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        if (prompts.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(prompts, key = { it }) { prompt -> InspirationChip(prompt) }
            }
        }
    }
}

@Composable
private fun InspirationChip(text: String) {
    Text(
        text = text,
        fontFamily = CairoFamily,
        fontSize = 12.sp,
        color = MawaaiColors.DesignTextLight,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MawaaiColors.DesignSurface)
            .border(
                width = 1.dp,
                color = MawaaiColors.DesignGold.copy(alpha = 0.4f),
                shape = RoundedCornerShape(50)
            )
            .clickable(enabled = false) {}
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun ConverterHomeScreenPreview() {
    ConverterHomeScreen(nav = rememberNavController())
}
