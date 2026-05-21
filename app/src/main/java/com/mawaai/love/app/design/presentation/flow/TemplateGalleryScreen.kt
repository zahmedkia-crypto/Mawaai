package com.mawaai.love.app.design.presentation.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.design.ai.RefinementStage
import com.mawaai.love.app.design.domain.model.Template
import com.mawaai.love.app.design.presentation.main.DesignRoute
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

@Composable
fun TemplateGalleryScreen(
    nav: NavController,
    sessionId: String,
    viewModel: TemplateGalleryViewModel = hiltViewModel()
) {
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.nav.collect {
            nav.navigate(DesignRoute.Customize.create(sessionId)) {
                popUpTo(DesignRoute.TemplateGallery.route) { inclusive = true }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingState()
            state.templates.isEmpty() -> EmptyState()
            else -> Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.templates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            selected = template.id == state.selectedTemplateId,
                            onClick = { viewModel.select(template.id) }
                        )
                    }
                }
            }
        }

        state.errorMessage?.let { msg ->
            Text(
                text = msg,
                fontFamily = CairoFamily,
                color = MawaaiColors.DesignHennaLight,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        if (state.isApplying) {
            ApplyStageIndicator(
                stage = state.applyStage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        Button(
            onClick = { viewModel.apply(sessionId) },
            enabled = state.selectedTemplateId != null && !state.isApplying && state.templates.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MawaaiColors.DesignGold,
                contentColor = MawaaiColors.DesignBgDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            if (state.isApplying) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp).width(18.dp),
                    color = MawaaiColors.DesignBgDark,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = stringResource(R.string.template_gallery_apply),
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Phase 25 — three-step progress strip shown while [TemplateGalleryViewModel.apply]
 * is running. Renders a row of three pill-shaped indicators, one per
 * stage (Compose → Refine → Polish). The current stage glows in
 * [MawaaiColors.DesignGold]; completed ones are dim gold; upcoming
 * ones are outlined only.
 */
@Composable
private fun ApplyStageIndicator(
    stage: RefinementStage,
    modifier: Modifier = Modifier
) {
    val stages = listOf(
        StageInfo(R.string.template_apply_stage_compose, RefinementStage.Compositing),
        StageInfo(R.string.template_apply_stage_refine, RefinementStage.Refining),
        StageInfo(R.string.template_apply_stage_polish, RefinementStage.Polishing)
    )
    val activeIndex = when (stage) {
        RefinementStage.Compositing -> 0
        RefinementStage.Refining -> 1
        RefinementStage.Polishing -> 2
        RefinementStage.Done -> 3
        else -> -1
    }
    Column(modifier = modifier) {
        Text(
            text = stringResource(currentStageLabel(stage)),
            fontFamily = CairoFamily,
            fontWeight = FontWeight.SemiBold,
            color = MawaaiColors.DesignGold,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            stages.forEachIndexed { index, info ->
                val state = when {
                    index < activeIndex -> StageDotState.Completed
                    index == activeIndex -> StageDotState.Active
                    else -> StageDotState.Pending
                }
                StageDot(
                    label = stringResource(info.label),
                    state = state,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private data class StageInfo(val label: Int, val stage: RefinementStage)
private enum class StageDotState { Pending, Active, Completed }

@Composable
private fun StageDot(
    label: String,
    state: StageDotState,
    modifier: Modifier = Modifier
) {
    val (background, contentColor) = when (state) {
        StageDotState.Active -> MawaaiColors.DesignGold to MawaaiColors.DesignBgDark
        StageDotState.Completed -> MawaaiColors.DesignGold.copy(alpha = 0.45f) to MawaaiColors.DesignBgDark
        StageDotState.Pending -> MawaaiColors.DesignSurface to MawaaiColors.DesignTextLight.copy(alpha = 0.6f)
    }
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = CairoFamily,
            fontSize = 11.sp,
            color = contentColor,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun currentStageLabel(stage: RefinementStage): Int = when (stage) {
    RefinementStage.Compositing -> R.string.template_apply_status_compose
    RefinementStage.Refining -> R.string.template_apply_status_refine
    RefinementStage.Polishing -> R.string.template_apply_status_polish
    RefinementStage.Done -> R.string.template_apply_status_done
    else -> R.string.template_apply_status_idle
}

@Composable
private fun TemplateCard(
    template: Template,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MawaaiColors.DesignGold else MawaaiColors.DesignGold.copy(alpha = 0.25f)
    val borderWidth = if (selected) 3.dp else 1.dp
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MawaaiColors.DesignSurface)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/${template.assetPath}")
                .crossfade(true)
                .build(),
            contentDescription = template.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = template.displayName,
            fontFamily = CairoFamily,
            color = MawaaiColors.DesignTextLight,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MawaaiColors.DesignGold)
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            tint = MawaaiColors.DesignGold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.template_gallery_empty),
            fontFamily = CairoFamily,
            color = MawaaiColors.DesignTextLight,
            fontSize = 14.sp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun TemplateGalleryScreenPreview() {
    TemplateGalleryScreen(nav = rememberNavController(), sessionId = "preview")
}
