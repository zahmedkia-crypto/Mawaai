package com.mawaai.love.app.design.ai.suggestions

import com.mawaai.love.app.design.ai.analysis.NormalizedRect
import com.mawaai.love.app.design.ai.analysis.SketchAnalysis
import com.mawaai.love.app.design.rendering.RenderAssessment
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives the creative loop after the user finishes drawing:
 *
 * 1. Analyze drawing.
 * 2. Show five focused AI suggestions.
 * 3. User accepts any subset.
 * 4. Render with accepted suggestions.
 * 5. Assess render.
 * 6. Show five new refinement suggestions.
 * 7. Repeat until the user is happy.
 */
data class SuggestionIteration(
    val round: Int,
    val stage: Stage,
    val suggestions: List<Suggestion>,
    val acceptedSuggestionIds: Set<String> = emptySet()
) {
    enum class Stage { AFTER_ANALYSIS, AFTER_RENDER }

    val acceptedSuggestions: List<Suggestion>
        get() = suggestions.filter { it.id in acceptedSuggestionIds }
}

@Singleton
class IterativeSuggestionLoop @Inject constructor() {

    fun afterAnalysis(analysis: SketchAnalysis): SuggestionIteration {
        return SuggestionIteration(
            round = 1,
            stage = SuggestionIteration.Stage.AFTER_ANALYSIS,
            suggestions = buildInitialSuggestions(analysis).take(MAX_VISIBLE_SUGGESTIONS)
        )
    }

    fun afterRender(
        previous: SuggestionIteration,
        assessment: RenderAssessment
    ): SuggestionIteration {
        return SuggestionIteration(
            round = previous.round + 1,
            stage = SuggestionIteration.Stage.AFTER_RENDER,
            suggestions = buildRenderRefinementSuggestions(assessment).take(MAX_VISIBLE_SUGGESTIONS)
        )
    }

    fun accept(iteration: SuggestionIteration, suggestionIds: Set<String>): SuggestionIteration {
        val visibleIds = iteration.suggestions.map { it.id }.toSet()
        return iteration.copy(acceptedSuggestionIds = suggestionIds.intersect(visibleIds))
    }

    private fun buildInitialSuggestions(analysis: SketchAnalysis): List<Suggestion> {
        val findings = analysis.findings.sortedByDescending { severityWeight(it.severity) }
        val fromFindings = findings.mapIndexed { index, finding ->
            Suggestion(
                id = "analysis-${finding.id.ifBlank { UUID.randomUUID().toString() }}",
                category = finding.toSuggestionCategory(),
                location = finding.region,
                title = finding.what.takeIf { it.isNotBlank() } ?: "Improve drawing detail",
                explanation = finding.why,
                principle = finding.principle,
                culturalContext = finding.culturalContext,
                impact = (9 - index).coerceIn(5, 10),
                autoFixable = true,
                previewHint = "Improve ${finding.what.lowercase()} while preserving ${analysis.sketchStructure.mustPreserve.joinToString()}"
            )
        }

        val strategic = listOf(
            Suggestion(
                id = "analysis-line-quality",
                category = Suggestion.Category.LINE,
                location = FULL_CANVAS,
                title = "Clean and sharpen line quality",
                explanation = "Reduce shaky edges and make important strokes more confident before rendering.",
                principle = "Cleaner source lines produce more realistic generated details.",
                culturalContext = analysis.culturalOrigin,
                impact = analysis.lineQuality.shakiness.coerceIn(5, 10),
                autoFixable = true,
                previewHint = "Sharpen linework, preserve motif identity, keep handmade character"
            ),
            Suggestion(
                id = "analysis-surface-fit",
                category = Suggestion.Category.TEMPLATE,
                location = FULL_CANVAS,
                title = "Improve fit to target surface",
                explanation = analysis.templateMapping.surfaceFitNotes.ifBlank { "Adapt motif scale and placement to the selected template surface." },
                principle = "Designs look real when scale, density, and placement match the physical surface.",
                culturalContext = analysis.culturalOrigin,
                impact = (10 - analysis.templateFit.scaleMatch / 12).coerceIn(5, 10),
                autoFixable = true,
                previewHint = "Resize and position motifs for ${analysis.templateMapping.surfaceType} and ${analysis.templateMapping.primaryZone}"
            ),
            Suggestion(
                id = "analysis-realism-prep",
                category = Suggestion.Category.PRINT,
                location = FULL_CANVAS,
                title = "Prepare for photoreal rendering",
                explanation = "Add material-aware details so the final image looks manufactured, painted, embroidered, carved, or applied naturally.",
                principle = "Rendering needs surface-aware texture and lighting cues, not only decorative shapes.",
                culturalContext = analysis.culturalOrigin,
                impact = 9,
                autoFixable = true,
                previewHint = "Add surface texture cues, contact shadows, material thickness, and realistic edge behavior"
            )
        )

        return (fromFindings + strategic).distinctBy { it.id }
    }

    private fun buildRenderRefinementSuggestions(assessment: RenderAssessment): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        if (assessment.realism < RenderAssessment.REALISM_MIN) {
            suggestions += refinement(
                id = "render-realism",
                category = Suggestion.Category.PRINT,
                title = "Make the render more real",
                explanation = "The result still looks too digital or pasted. Push it toward real product photography.",
                impact = 10,
                previewHint = "Increase photorealism, natural camera response, realistic surface defects, and believable manufacture"
            )
        }
        if (assessment.structurePreservation < RenderAssessment.STRUCTURE_MIN) {
            suggestions += refinement(
                id = "render-structure",
                category = Suggestion.Category.SYMMETRY,
                title = "Restore the original drawing structure",
                explanation = "The AI changed too much from the user's idea. Lock the motif layout more strongly.",
                impact = 10,
                previewHint = "Preserve motif positions, proportions, symmetry, spacing, and primary design identity"
            )
        }
        if (assessment.materialIntegration < RenderAssessment.MATERIAL_MIN) {
            suggestions += refinement(
                id = "render-material",
                category = Suggestion.Category.TEMPLATE,
                title = "Blend design into the material",
                explanation = "The design should inherit folds, pores, glaze, grain, seams, or curvature from the target surface.",
                impact = 9,
                previewHint = "Integrate artwork into material texture with occlusion, grain, folds, glaze, and surface response"
            )
        }
        if (assessment.lightingConsistency < RenderAssessment.LIGHTING_MIN) {
            suggestions += refinement(
                id = "render-lighting",
                category = Suggestion.Category.COLOR,
                title = "Fix lighting and shadows",
                explanation = "Highlights and shadows should match the scene so the artwork belongs to the photo.",
                impact = 8,
                previewHint = "Match scene lighting, add ambient occlusion, contact shadows, and consistent highlights"
            )
        }

        suggestions += listOf(
            refinement(
                id = "render-premium-finish",
                category = Suggestion.Category.PRINT,
                title = "Add premium finish",
                explanation = "Give the result a final professional finish with subtle imperfections and high-end product detail.",
                impact = 8,
                previewHint = "Add premium product finish, microtexture, realistic edges, and refined camera depth"
            ),
            refinement(
                id = "render-user-intent",
                category = Suggestion.Category.CULTURAL,
                title = "Move closer to user intent",
                explanation = "Keep refining until the result matches the image the user has in mind.",
                impact = 9,
                previewHint = "Make the design closer to the user's intent while preserving cultural authenticity and realistic material behavior"
            )
        )

        return suggestions.distinctBy { it.id }
    }

    private fun refinement(
        id: String,
        category: Suggestion.Category,
        title: String,
        explanation: String,
        impact: Int,
        previewHint: String
    ): Suggestion = Suggestion(
        id = id,
        category = category,
        location = FULL_CANVAS,
        title = title,
        explanation = explanation,
        principle = "Iterative refinement improves the render by correcting only the weakest quality dimensions.",
        culturalContext = "Preserve cultural identity and user intent.",
        impact = impact,
        autoFixable = true,
        previewHint = previewHint
    )

    private fun SketchAnalysis.Finding.toSuggestionCategory(): Suggestion.Category = when (severity) {
        SketchAnalysis.Finding.Severity.CRITICAL -> Suggestion.Category.TEMPLATE
        SketchAnalysis.Finding.Severity.WARNING -> Suggestion.Category.LINE
        SketchAnalysis.Finding.Severity.INFO -> Suggestion.Category.CULTURAL
    }

    private fun severityWeight(severity: SketchAnalysis.Finding.Severity): Int = when (severity) {
        SketchAnalysis.Finding.Severity.CRITICAL -> 3
        SketchAnalysis.Finding.Severity.WARNING -> 2
        SketchAnalysis.Finding.Severity.INFO -> 1
    }

    private companion object {
        const val MAX_VISIBLE_SUGGESTIONS = 5
        val FULL_CANVAS = NormalizedRect(0f, 0f, 1f, 1f)
    }
}
