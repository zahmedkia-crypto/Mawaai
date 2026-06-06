package com.mawaai.love.app.ui.design.suggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mawaai.love.app.design.ai.suggestions.Suggestion

/**
 * MT-025: a vertically scrolling list of refinement-suggestion cards generated
 * by the analysis pipeline (MT-019 / MT-023). User taps a card to flip its
 * accepted/skipped state; the bottom button persists the selection so the
 * next render call (MT-027) honors it.
 *
 * Compose style mirrors `AiProviderSettingsScreen`:
 *  - Scaffold + TopAppBar at the top.
 *  - LazyColumn body with 16.dp page inset.
 *  - One section header item, then card items, then a sticky bottom action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionCardsScreen(
    projectId: String,
    viewModel: SuggestionCardsViewModel,
    onAcceptanceSubmitted: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.load(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Refinement Suggestions") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null && uiState.suggestions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                    ) {
                        item {
                            Text(
                                text = "Tap a card to accept it for your next render.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                        items(uiState.suggestions, key = { it.id }) { suggestion ->
                            SuggestionCard(
                                suggestion = suggestion,
                                isAccepted = suggestion.id in uiState.acceptedIds,
                                onToggle = { viewModel.toggle(suggestion.id) },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                    Button(
                        onClick = { viewModel.submit(onDone = onAcceptanceSubmitted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        enabled = uiState.acceptedIds.isNotEmpty(),
                    ) {
                        Text(
                            text = if (uiState.acceptedIds.isEmpty()) {
                                "Select at least one to continue"
                            } else {
                                "Accept ${uiState.acceptedIds.size} suggestion(s) and render"
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: Suggestion,
    isAccepted: Boolean,
    onToggle: () -> Unit,
) {
    val accentColor = categoryColor(suggestion.category)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAccepted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                CategoryChip(label = suggestion.category.name, color = accentColor)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = if (isAccepted) "Accepted" else "Tap to accept",
                        tint = if (isAccepted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = suggestion.explanation,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (suggestion.culturalContext.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = suggestion.culturalContext,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            ImpactBar(impact = suggestion.impact)
            if (suggestion.autoFixable) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Auto-fixable",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ImpactBar(impact: Int) {
    val normalizedImpact = impact.coerceIn(0, 10)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Impact",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$normalizedImpact / 10",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = normalizedImpact / 10f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        )
    }
}

/**
 * Map each Suggestion.Category to a stable Material-friendly accent color.
 * Same 6 categories as defined in design/ai/suggestions/Suggestion.kt.
 */
private fun categoryColor(category: Suggestion.Category): Color = when (category) {
    Suggestion.Category.LINE -> Color(0xFF6B5B95)         // muted purple
    Suggestion.Category.SYMMETRY -> Color(0xFFB8860B)     // dark goldenrod
    Suggestion.Category.TEMPLATE -> Color(0xFF2E7D32)     // forest green
    Suggestion.Category.CULTURAL -> Color(0xFFB86B3A)     // henna brown
    Suggestion.Category.PRINT -> Color(0xFF455A64)        // blue grey
    Suggestion.Category.COLOR -> Color(0xFFC2185B)        // crimson pink
}