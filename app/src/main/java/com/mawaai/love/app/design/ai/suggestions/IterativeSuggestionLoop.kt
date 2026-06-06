package com.mawaai.love.app.design.ai.suggestions

import com.mawaai.love.app.design.ai.analysis.NormalizedRect
import com.mawaai.love.app.design.ai.analysis.SketchAnalysis
import com.mawaai.love.app.design.rendering.RenderAssessment
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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
        val findings = analysis.findings.sortedByDescending { severityWeight(it.severity) }
        val fromFindings = findings.mapIndexed { index, finding ->
            Suggestion(
                id = "analysis-${finding.id.ifBlank { UUID.randomUUID().toString() }}",
                category = finding.toSuggestionCategory(),
                location = finding.region,
                title = finding.what.ifBlank { "Improve drawing detail" },
                explanation = finding.why,
                principle = finding.principle,
                culturalContext = finding.culturalContext,
                impact = (9 - index).coerceIn(5, 10),
                autoFixable = true,
                previewHint = "Improve ${finding.what.lowercase()} while preserving ${analysis.sketchStructure.mustPreserve.joinToString()}"
            )
        }
        return SuggestionIteration(
            round = 1,
            stage = SuggestionIteration.Stage.AFTER_ANALYSIS,
            suggestions = fillToFive(fromFindings + analysisDefaults(analysis))
        )
    }

    fun afterRender(previous: SuggestionIteration, assessment: RenderAssessment): SuggestionIteration {
        val targeted = mutableListOf<Suggestion>()
        if (assessment.realism < RenderAssessment.REALISM_MIN) targeted += refinement("render-realism", Suggestion.Category.PRINT, "Make the render more real", "The result still looks too digital or pasted.", 10, "Increase photorealism, natural camera response, realistic surface defects, and believable manufacture")
        if (assessment.structurePreservation < RenderAssessment.STRUCTURE_MIN) targeted += refinement("render-structure", Suggestion.Category.SYMMETRY, "Restore the original drawing structure", "The AI changed too much from the user's idea.", 10, "Preserve motif positions, proportions, symmetry, spacing, and primary design identity")
        if (assessment.materialIntegration < RenderAssessment.MATERIAL_MIN) targeted += refinement("render-material", Suggestion.Category.TEMPLATE, "Blend design into the material", "The design should inherit folds, pores, glaze, grain, seams, or curvature.", 9, "Integrate artwork into material texture with occlusion, grain, folds, glaze, and surface response")
        if (assessment.lightingConsistency < RenderAssessment.LIGHTING_MIN) targeted += refinement("render-lighting", Suggestion.Category.COLOR, "Fix lighting and shadows", "Highlights and shadows should match the scene.", 8, "Match scene lighting, add ambient occlusion, contact shadows, and consistent highlights")
        return SuggestionIteration(
            round = previous.round + 1,
            stage = SuggestionIteration.Stage.AFTER_RENDER,
            suggestions = fillToFive(targeted + renderDefaults)
        )
    }

    fun accept(iteration: SuggestionIteration, suggestionIds: Set<String>): SuggestionIteration {
        val visibleIds = iteration.suggestions.map { it.id }.toSet()
        return iteration.copy(acceptedSuggestionIds = suggestionIds.intersect(visibleIds))
    }

    private fun fillToFive(suggestions: List<Suggestion>): List<Suggestion> =
        (suggestions + renderDefaults).distinctBy { it.id }.take(MAX_VISIBLE_SUGGESTIONS)

    private fun analysisDefaults(analysis: SketchAnalysis): List<Suggestion> = listOf(
        refinement("analysis-line-quality", Suggestion.Category.LINE, "Clean and sharpen line quality", "Reduce shaky edges and make important strokes more confident before rendering.", analysis.lineQuality.shakiness.coerceIn(5, 10), "Sharpen linework, preserve motif identity, keep handmade character"),
        refinement("analysis-surface-fit", Suggestion.Category.TEMPLATE, "Improve fit to target surface", analysis.templateMapping.surfaceFitNotes.ifBlank { "Adapt motif scale and placement to the selected template surface." }, 9, "Resize and position motifs for ${analysis.templateMapping.surfaceType} and ${analysis.templateMapping.primaryZone}"),
        refinement("analysis-realism-prep", Suggestion.Category.PRINT, "Prepare for photoreal rendering", "Add material-aware details so the final image looks manufactured, painted, embroidered, carved, or applied naturally.", 9, "Add surface texture cues, contact shadows, material thickness, and realistic edge behavior"),
        refinement("analysis-balance", Suggestion.Category.SYMMETRY, "Improve balance and hierarchy", analysis.composition.hierarchyNotes.ifBlank { "Make the most important motif clearer and balance supporting details around it." }, 8, "Strengthen visual hierarchy, balance negative space, and keep the main motif dominant"),
        refinement("analysis-premium-detail", Suggestion.Category.CULTURAL, "Add premium cultural detailing", "Refine the design with culturally respectful small details that feel intentional and high-end.", 8, "Add refined cultural detail, elegant spacing, and premium finish without changing the core idea")
    )

    private fun refinement(id: String, category: Suggestion.Category, title: String, explanation: String, impact: Int, previewHint: String): Suggestion = Suggestion(
        id = id,
        category = category,
        location = FULL_CANVAS,
        title = title,
        explanation = explanation,
        principle = "Iterative refinement improves the weakest quality dimensions while preserving the user idea.",
        culturalContext = "Preserve cultural identity and user intent.",
        impact = impact.coerceIn(1, 10),
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
        val renderDefaults = listOf(
            Suggestion("render-premium-finish", Suggestion.Category.PRINT, FULL_CANVAS, "Add premium finish", "Give the result a final professional finish with subtle imperfections and high-end detail.", "Premium realism comes from controlled imperfections.", "Preserve cultural identity and user intent.", 8, true, "Add premium product finish, microtexture, realistic edges, and refined camera depth"),
            Suggestion("render-user-intent", Suggestion.Category.CULTURAL, FULL_CANVAS, "Move closer to user intent", "Keep refining until the result matches the image the user has in mind.", "Every iteration should move closer to the user's mental image.", "Preserve cultural authenticity.", 9, true, "Make the design closer to the user's intent while preserving cultural authenticity and realistic material behavior"),
            Suggestion("render-fine-detail", Suggestion.Category.LINE, FULL_CANVAS, "Increase fine detail clarity", "Make small motifs readable without adding noise.", "Detail should stay crisp and controlled.", "Respect traditional motif readability.", 7, true, "Improve fine detail clarity, crisp edges, and readable small motifs while avoiding noise"),
            Suggestion("render-color-polish", Suggestion.Category.COLOR, FULL_CANVAS, "Polish color and contrast", "Tune colors so the result feels richer and more natural.", "Color realism depends on material-appropriate saturation.", "Keep natural color behavior.", 7, true, "Refine color harmony, natural contrast, and material-appropriate saturation"),
            Suggestion("render-luxury-variant", Suggestion.Category.TEMPLATE, FULL_CANVAS, "Try a more luxurious version", "Create a more premium variant while keeping the same structure.", "Luxury should enhance, not replace, the idea.", "Preserve motif identity.", 8, true, "Make a more luxurious premium version without changing motif structure or user intent")
        )
    }
}