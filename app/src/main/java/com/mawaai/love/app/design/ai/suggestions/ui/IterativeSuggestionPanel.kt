package com.mawaai.love.app.design.ai.suggestions.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mawaai.love.app.core.components.RoseGlassCard
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.design.ai.analysis.NormalizedRect
import com.mawaai.love.app.design.ai.suggestions.SatisfactionFeedback
import com.mawaai.love.app.design.ai.suggestions.Suggestion
import com.mawaai.love.app.design.ai.suggestions.SuggestionIteration

@Composable
fun IterativeSuggestionPanel(
    iteration: SuggestionIteration,
    selectedIds: Set<String>,
    onToggleSuggestion: (String) -> Unit,
    onRenderSelected: () -> Unit,
    onSkipAndRender: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (iteration.stage == SuggestionIteration.Stage.AFTER_ANALYSIS) {
                "AI found 5 ways to improve your drawing"
            } else {
                "AI found 5 refinements for this render"
            },
            style = MaterialTheme.typography.titleLarge,
            color = MawaaiColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Choose what feels right. Mawaai will only apply the ideas you accept, then render again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MawaaiColors.TextSecondary
        )

        iteration.suggestions.forEachIndexed { index, suggestion ->
            SuggestionChoiceCard(
                index = index + 1,
                suggestion = suggestion,
                selected = suggestion.id in selectedIds,
                onClick = { onToggleSuggestion(suggestion.id) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                modifier = Modifier.weight(1f),
                enabled = selectedIds.isNotEmpty(),
                onClick = onRenderSelected
            ) {
                Text("Apply & render")
            }
            TextButton(onClick = onSkipAndRender) {
                Text("Render without changes")
            }
        }
    }
}

@Composable
private fun SuggestionChoiceCard(
    index: Int,
    suggestion: Suggestion,
    selected: Boolean,
    onClick: () -> Unit
) {
    RoseGlassCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() }
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$index. ${suggestion.title}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MawaaiColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selected,
                        onClick = onClick,
                        label = { Text("Impact ${suggestion.impact}/10") }
                    )
                }
                Text(
                    text = suggestion.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MawaaiColors.TextSecondary
                )
                Text(
                    text = suggestion.previewHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MawaaiColors.TextPoetic
                )
            }
        }
    }
}

@Composable
fun SatisfactionFeedbackPanel(
    feedback: SatisfactionFeedback,
    onFeedbackChanged: (SatisfactionFeedback) -> Unit,
    onRefineAgain: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    RoseGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "How close is this to your idea?",
                style = MaterialTheme.typography.titleLarge,
                color = MawaaiColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${feedback.closenessToIntentPct}%",
                style = MaterialTheme.typography.displaySmall,
                color = MawaaiColors.ChampagneGold,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { feedback.closenessToIntentPct / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Slider(
                value = feedback.closenessToIntentPct.toFloat(),
                onValueChange = {
                    onFeedbackChanged(feedback.copy(closenessToIntentPct = it.toInt().coerceIn(0, 100)))
                },
                valueRange = 0f..100f
            )
            OutlinedTextField(
                value = feedback.keepFromCurrent,
                onValueChange = { onFeedbackChanged(feedback.copy(keepFromCurrent = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("What should stay exactly like this?") },
                minLines = 1
            )
            OutlinedTextField(
                value = feedback.changeNext,
                onValueChange = { onFeedbackChanged(feedback.copy(changeNext = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("What should change next?") },
                minLines = 1
            )
            OutlinedTextField(
                value = feedback.whatIsMissing,
                onValueChange = { onFeedbackChanged(feedback.copy(whatIsMissing = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("What is still missing from your mind?") },
                minLines = 2
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onRefineAgain
                ) {
                    Text("Refine again")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = feedback.closenessToIntentPct >= 95,
                    onClick = onFinish
                ) {
                    Text("Perfect")
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0510)
@Composable
private fun IterativeSuggestionPanelPreview() {
    val suggestions = listOf(
        Suggestion(
            id = "line",
            category = Suggestion.Category.LINE,
            location = NormalizedRect(0f, 0f, 1f, 1f),
            title = "Clean and sharpen line quality",
            explanation = "Reduce shaky edges and keep the handmade character.",
            principle = "Cleaner lines produce better render detail.",
            culturalContext = "Henna",
            impact = 9,
            autoFixable = true,
            previewHint = "Sharpen linework while preserving motif identity"
        ),
        Suggestion(
            id = "material",
            category = Suggestion.Category.TEMPLATE,
            location = NormalizedRect(0f, 0f, 1f, 1f),
            title = "Blend design into the material",
            explanation = "Make the render inherit the target surface texture.",
            principle = "Material response creates realism.",
            culturalContext = "Ceramic",
            impact = 10,
            autoFixable = true,
            previewHint = "Add glaze, curvature, shadows, and realistic reflections"
        )
    )
    IterativeSuggestionPanel(
        iteration = SuggestionIteration(
            round = 1,
            stage = SuggestionIteration.Stage.AFTER_ANALYSIS,
            suggestions = suggestions
        ),
        selectedIds = setOf("line"),
        onToggleSuggestion = {},
        onRenderSelected = {},
        onSkipAndRender = {},
        modifier = Modifier.padding(16.dp)
    )
}
