package com.mawaai.love.app.design.ai.suggestions

import com.google.gson.Gson
import com.mawaai.love.app.design.ai.analysis.NormalizedRect
import com.mawaai.love.app.design.ai.analysis.SketchAnalysis
import com.mawaai.love.app.design.ai.gateway.ProviderRegistry
import com.mawaai.love.app.design.rendering.RenderAssessment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiSuggestionEngine @Inject constructor(
    private val providerRegistry: ProviderRegistry,
    private val fallbackLoop: IterativeSuggestionLoop,
    private val gson: Gson
) {

    suspend fun afterAnalysis(analysis: SketchAnalysis): SuggestionIteration {
        val fallback = fallbackLoop.afterAnalysis(analysis)
        val ai = requestSuggestions(
            prompt = "Create exactly 5 selectable design improvements after this analysis: ${gson.toJson(analysis)}",
            fallback = fallback.suggestions
        )
        return fallback.copy(suggestions = ai)
    }

    suspend fun afterRender(
        previous: SuggestionIteration,
        assessment: RenderAssessment,
        feedback: SatisfactionFeedback? = null
    ): SuggestionIteration {
        val fallback = fallbackLoop.afterRender(previous, assessment)
        val userFeedback = feedback?.toSuggestionHint().orEmpty()
        val ai = requestSuggestions(
            prompt = "Create exactly 5 next render refinements. Assessment=${gson.toJson(assessment)} Feedback=$userFeedback",
            fallback = fallback.suggestions
        )
        return fallback.copy(suggestions = ai)
    }

    fun accept(iteration: SuggestionIteration, suggestionIds: Set<String>): SuggestionIteration =
        fallbackLoop.accept(iteration, suggestionIds)

    private suspend fun requestSuggestions(prompt: String, fallback: List<Suggestion>): List<Suggestion> {
        val response = providerRegistry.activeTextChain()
            .generate(prompt = prompt + JSON_RULES, systemPrompt = SYSTEM_PROMPT)
            .getOrElse { return fillToFive(fallback) }
        val parsed = extractJsonObject(response)
            ?.let { runCatching { gson.fromJson(it, SuggestionsResponse::class.java) }.getOrNull() }
            ?.suggestions
            .orEmpty()
        return fillToFive(parsed.mapIndexedNotNull { index, suggestion -> suggestion.safe(index) } + fallback)
    }

    private fun fillToFive(input: List<Suggestion>): List<Suggestion> =
        (input + DEFAULT_SUGGESTIONS)
            .distinctBy { it.id }
            .take(MAX_VISIBLE_SUGGESTIONS)

    private fun Suggestion.safe(index: Int): Suggestion? = runCatching {
        copy(
            id = id.ifBlank { "ai-suggestion-${index + 1}" },
            location = NormalizedRect(
                x = location.x.coerceIn(0f, 1f),
                y = location.y.coerceIn(0f, 1f),
                w = location.w.coerceIn(0f, 1f),
                h = location.h.coerceIn(0f, 1f)
            ),
            title = title.ifBlank { "Improve design quality" }.take(MAX_TITLE_CHARS),
            explanation = explanation.ifBlank { "Move the next render closer to the user's vision." }.take(MAX_BODY_CHARS),
            principle = principle.ifBlank { "Preserve structure while improving realism." }.take(MAX_BODY_CHARS),
            culturalContext = culturalContext.ifBlank { "Preserve cultural authenticity." }.take(MAX_BODY_CHARS),
            impact = impact.coerceIn(1, 10),
            previewHint = previewHint.ifBlank { title }.take(MAX_HINT_CHARS)
        )
    }.getOrNull()

    private fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return text.substring(start, end + 1)
    }

    private companion object {
        const val MAX_VISIBLE_SUGGESTIONS = 5
        const val MAX_TITLE_CHARS = 72
        const val MAX_BODY_CHARS = 220
        const val MAX_HINT_CHARS = 260
        const val SYSTEM_PROMPT = "You are Mawaai's design coach. Return valid JSON only."
        const val JSON_RULES = " Return JSON only as {\"suggestions\":[{\"id\":\"id\",\"category\":\"LINE\",\"location\":{\"x\":0.0,\"y\":0.0,\"w\":1.0,\"h\":1.0},\"title\":\"title\",\"explanation\":\"why\",\"principle\":\"principle\",\"culturalContext\":\"context\",\"impact\":8,\"autoFixable\":true,\"previewHint\":\"render instruction\"}]} with exactly 5 suggestions."
        val DEFAULT_SUGGESTIONS = listOf(
            Suggestion("default-realism", Suggestion.Category.PRINT, NormalizedRect(0f, 0f, 1f, 1f), "Make it more realistic", "Push the result toward real product photography.", "Photorealism needs believable material and camera behavior.", "Preserve cultural identity.", 10, true, "Increase photorealism, natural camera response, realistic material texture, and believable manufacture"),
            Suggestion("default-structure", Suggestion.Category.SYMMETRY, NormalizedRect(0f, 0f, 1f, 1f), "Preserve the original drawing", "Keep the user's motif layout and proportions locked.", "The sketch is the source of truth.", "Preserve user intent.", 10, true, "Preserve motif positions, proportions, symmetry, spacing, and primary design identity"),
            Suggestion("default-material", Suggestion.Category.TEMPLATE, NormalizedRect(0f, 0f, 1f, 1f), "Blend into the surface", "Make the design inherit folds, pores, glaze, grain, or seams.", "Material integration prevents the sticker look.", "Respect target surface tradition.", 9, true, "Integrate artwork into material texture with occlusion, grain, folds, glaze, and surface response"),
            Suggestion("default-lighting", Suggestion.Category.COLOR, NormalizedRect(0f, 0f, 1f, 1f), "Fix lighting and shadows", "Match highlights and shadows to the scene.", "Lighting consistency sells realism.", "Keep natural color behavior.", 8, true, "Match scene lighting, add ambient occlusion, contact shadows, and consistent highlights"),
            Suggestion("default-premium", Suggestion.Category.CULTURAL, NormalizedRect(0f, 0f, 1f, 1f), "Add premium finish", "Add subtle high-end details without changing the idea.", "Premium detail should be controlled and intentional.", "Preserve cultural authenticity.", 8, true, "Add premium finish, refined microdetail, elegant spacing, and realistic edges")
        )
    }
}