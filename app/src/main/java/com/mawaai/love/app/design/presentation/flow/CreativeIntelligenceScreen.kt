package com.mawaai.love.app.design.presentation.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.mawaai.love.app.design.presentation.common.CreativeSuggestionCard
import com.mawaai.love.app.design.presentation.main.DesignRoute

@Composable
fun CreativeIntelligenceScreen(
    nav: NavController,
    projectId: String,
    viewModel: CreativeAnalysisViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(projectId) {
        viewModel.load(projectId)
    }

    Scaffold(
        bottomBar = {
            if (!state.isLoading && state.error == null) {
                Surface(
                    color = MawaaiColors.DesignBgDark,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = {
                            viewModel.applyAndContinue(projectId) {
                                nav.navigate(DesignRoute.Processing.create(projectId))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MawaaiColors.DesignGold
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (state.acceptedSuggestionIds.isEmpty()) {
                                "Render Original Design"
                            } else {
                                "Apply ${state.acceptedSuggestionIds.size} & Render"
                            },
                            fontFamily = CairoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        },
        containerColor = MawaaiColors.DesignBgDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                IntelligenceLoadingState()
            } else if (state.error != null) {
                ErrorState(state.error!!) { viewModel.load(projectId) }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        IntelligenceHeader()
                    }

                    state.analysis?.let { analysis ->
                        item {
                            AnalysisSummaryCard(analysis = analysis)
                        }
                    }

                    item {
                        Text(
                            text = "Choose up to 5 AI refinements",
                            fontFamily = CairoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MawaaiColors.DesignTextLight,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            text = "Select only the ideas that match what is in your mind. The renderer will preserve your drawing and apply the accepted refinements.",
                            fontFamily = CairoFamily,
                            fontSize = 13.sp,
                            color = MawaaiColors.DesignHennaLight
                        )
                    }

                    items(state.suggestions) { suggestion ->
                        CreativeSuggestionCard(
                            suggestion = suggestion,
                            isSelected = state.acceptedSuggestionIds.contains(suggestion.id),
                            onToggle = { viewModel.toggleSuggestion(suggestion.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntelligenceHeader() {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MawaaiColors.DesignGold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Design Intelligence",
                fontFamily = CairoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = MawaaiColors.DesignTextLight
            )
        }
        Text(
            text = "AI coach: analyze, suggest, render, refine",
            fontFamily = CairoFamily,
            fontSize = 14.sp,
            color = MawaaiColors.DesignHennaLight
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AnalysisSummaryCard(analysis: com.mawaai.love.app.design.ai.analysis.SketchAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MawaaiColors.DesignSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sketch Analysis Result",
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MawaaiColors.DesignGold
            )
            Spacer(Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoItem(label = "Style", value = analysis.artStyle, modifier = Modifier.weight(1f))
                InfoItem(label = "Origin", value = analysis.culturalOrigin, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoItem(label = "Symmetry", value = "${analysis.symmetry.accuracyPct}%", modifier = Modifier.weight(1f))
                InfoItem(label = "Balance", value = "${analysis.composition.balanceScore}/100", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontFamily = CairoFamily,
            fontSize = 11.sp,
            color = MawaaiColors.DesignHennaLight
        )
        Text(
            text = value,
            fontFamily = CairoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MawaaiColors.DesignTextLight
        )
    }
}

@Composable
private fun IntelligenceLoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = MawaaiColors.DesignGold)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Analyzing design context...",
            fontFamily = CairoFamily,
            color = MawaaiColors.DesignTextLight
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            fontFamily = CairoFamily,
            color = Color.Red.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}