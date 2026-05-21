package com.mawaai.love.app.design.presentation.canvas

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.design.domain.model.DrawingSuggestion

@Composable
fun CanvasRecommendationsScreen(
    nav: NavController,
    artworkId: Long,
    viewModel: CanvasRecommendationsViewModel = hiltViewModel()
) {
    LaunchedEffect(artworkId) { viewModel.load(artworkId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val appliedMsg = stringResource(R.string.recommendations_apply_success)
    val applyFailedPrefix = stringResource(R.string.recommendations_apply_failed)
    val revertedMsg = stringResource(R.string.recommendations_revert_success)
    val revertFailedPrefix = stringResource(R.string.recommendations_revert_failed)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RecommendationsEvent.ApplySuccess ->
                    Toast.makeText(context, appliedMsg, Toast.LENGTH_SHORT).show()
                RecommendationsEvent.RevertSuccess ->
                    Toast.makeText(context, revertedMsg, Toast.LENGTH_SHORT).show()
                is RecommendationsEvent.ApplyFailed -> Toast.makeText(
                    context,
                    if (event.message.isNullOrBlank()) applyFailedPrefix
                    else "$applyFailedPrefix: ${event.message}",
                    Toast.LENGTH_LONG
                ).show()
                is RecommendationsEvent.RevertFailed -> Toast.makeText(
                    context,
                    if (event.message.isNullOrBlank()) revertFailedPrefix
                    else "$revertFailedPrefix: ${event.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MawaaiColors.DesignBgDark)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MawaaiColors.DesignGold
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.recommendations_title),
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MawaaiColors.DesignTextLight,
                modifier = Modifier.weight(1f)
            )
            if (state.canUndo) {
                IconButton(
                    onClick = { viewModel.revert() },
                    enabled = !state.isReverting && state.applyingMessage == null
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = stringResource(R.string.action_revert),
                        tint = MawaaiColors.DesignGold
                    )
                }
            }
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.recommendations_refresh),
                    tint = MawaaiColors.DesignGold
                )
            }
        }

        Text(
            text = stringResource(R.string.recommendations_subtitle),
            fontFamily = CairoFamily,
            fontSize = 13.sp,
            color = MawaaiColors.DesignHennaLight,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Preview of the user's drawing.
        state.artworkUri?.let { uri ->
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MawaaiColors.DesignGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                if (state.applyingMessage != null || state.isReverting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MawaaiColors.DesignBgDark.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MawaaiColors.DesignGold)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MawaaiColors.DesignGold)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.recommendations_loading),
                        fontFamily = CairoFamily,
                        color = MawaaiColors.DesignTextLight,
                        fontSize = 13.sp
                    )
                }
            }

            state.analysis.suggestions.isEmpty() -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.recommendations_empty),
                    fontFamily = CairoFamily,
                    color = MawaaiColors.DesignTextLight,
                    fontSize = 14.sp
                )
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = state.analysis.suggestions,
                    key = { it.message }
                ) { suggestion ->
                    SuggestionCard(
                        suggestion = suggestion,
                        isApplying = state.applyingMessage == suggestion.message,
                        canApply = !state.isReverting && state.applyingMessage == null,
                        onApply = { viewModel.apply(suggestion.message) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: DrawingSuggestion,
    isApplying: Boolean,
    canApply: Boolean,
    onApply: () -> Unit
) {
    val applyingLabel = stringResource(R.string.recommendations_applying)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MawaaiColors.DesignSurface)
            .border(1.dp, MawaaiColors.DesignGold.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MawaaiColors.DesignGold)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = suggestion.message,
            fontFamily = CairoFamily,
            fontSize = 14.sp,
            color = MawaaiColors.DesignTextLight,
            modifier = Modifier.weight(1f)
        )
        if (suggestion.action != null) {
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = onApply,
                enabled = canApply && !isApplying,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        color = MawaaiColors.DesignGold,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(16.dp)
                            .semantics { contentDescription = applyingLabel }
                    )
                } else {
                    // AutoAwesome matches the Canvas top-bar AI Tips
                    // icon — sets a softer "polish" expectation than
                    // the heavier Bolt glyph that initially shipped.
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MawaaiColors.DesignGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_apply),
                        fontFamily = CairoFamily,
                        fontWeight = FontWeight.Bold,
                        color = MawaaiColors.DesignGold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun CanvasRecommendationsScreenPreview() {
    CanvasRecommendationsScreen(nav = rememberNavController(), artworkId = -1L)
}
