package com.mawaai.love.app.design.presentation.flow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import com.mawaai.love.app.design.ai.ProcessingStage
import com.mawaai.love.app.design.presentation.main.DesignRoute

@Composable
fun ProcessingScreen(
    nav: NavController,
    sessionId: String,
    viewModel: ProcessingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) { viewModel.start(sessionId) }

    LaunchedEffect(sessionId) {
        viewModel.nav.collect { event ->
            val target = when (event) {
                ProcessingNavEvent.NavigateToResult -> DesignRoute.Result.create(sessionId)
                ProcessingNavEvent.NavigateToTemplate -> DesignRoute.TemplateGallery.create(sessionId)
            }
            nav.navigate(target) {
                popUpTo(DesignRoute.Processing.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val stage = state.stage) {
            is ProcessingStage.Failed -> FailedView(
                message = stage.cause.localizedMessage,
                onRetry = { viewModel.retry(sessionId) }
            )
            else -> RunningView(state = state)
        }
    }
}

@Composable
private fun RunningView(state: ProcessingUiState) {
    CircularProgressIndicator(color = MawaaiColors.DesignGold)
    Spacer(Modifier.height(24.dp))
    Text(
        text = stringResource(stageLabel(state.stage)),
        fontFamily = CairoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = MawaaiColors.DesignTextLight
    )
    if (state.modelFallbackHinted) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.ai_model_missing),
            fontFamily = CairoFamily,
            fontSize = 12.sp,
            color = MawaaiColors.DesignHennaLight
        )
    }
}

@Composable
private fun FailedView(message: String?, onRetry: () -> Unit) {
    Text(
        text = stringResource(R.string.processing_error_title),
        fontFamily = CairoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = MawaaiColors.DesignTextLight
    )
    if (!message.isNullOrBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            fontFamily = CairoFamily,
            fontSize = 13.sp,
            color = MawaaiColors.DesignHennaLight
        )
    }
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = onRetry,
        colors = ButtonDefaults.buttonColors(
            containerColor = MawaaiColors.DesignGold,
            contentColor = MawaaiColors.DesignBgDark
        )
    ) {
        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.processing_retry),
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun stageLabel(stage: ProcessingStage): Int = when (stage) {
    ProcessingStage.Init -> R.string.stage_analyzing
    ProcessingStage.Segmenting -> R.string.stage_extracting
    ProcessingStage.EdgeDetecting -> R.string.stage_edge_detecting
    ProcessingStage.Stylizing -> R.string.stage_applying
    ProcessingStage.Upscaling -> R.string.stage_upscaling
    is ProcessingStage.Done -> R.string.stage_done
    is ProcessingStage.Failed -> R.string.processing_error_title
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun ProcessingScreenPreview() {
    ProcessingScreen(nav = rememberNavController(), sessionId = "preview")
}
