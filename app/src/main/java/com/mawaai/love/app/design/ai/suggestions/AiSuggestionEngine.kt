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
        val prompt = buildAfterAnalysisPrompt(analysis)
        val suggestions = generateFiveSuggestions(prompt, fallback.suggestions)
        return fallback.copy(suggestions = suggestions)
    }

    suspend fun afterRender(
        previous: SuggestionIteration,
        assessment: RenderAssessment,
        feedback: SatisfactionFeedback? = null
    ): SuggestionIteration {
        val fallback = fallbackLoop.afterRender(previous, assessment)
        val prompt = buildAfterRenderPrompt(assessment, feedback)
        val suggestions = generateFiveSuggestions(prompt, fallback.suggestions)
        return fallback.copy(suggestions = suggestions)
    }

    fun accept(iteration: SuggestionIteration, suggestionIds: Set<String>): SuggestionIteration =
        fallbackLoop.accept(iteration, suggestionIds)

    private suspend fun generateFiveSuggestions(
        prompt: String,
        fallback: List<Suggestion>
    ): List<Suggestion> {
        val response = providerRegistry.activeTextChain()
            .generate(prompt = prompt, systemPrompt = SYSTEM_PROMPT)
            .getOrElse { return fallback.take(MAX_VISIBLE_SUGGESTIONS) }

        val json = extractJsonObject(response) ?: return fallback.take(MAX_VISIBLE_SUGGESTIONS)
        val parsed = runCatching { gson.fromJson(json, SuggestionsResponse::class.java) }.getOrNull()
            ?: return fallback.take(MAX_VISIBLE_SUGGESTIONS)

        val cleaned = parsed.suggestions
            .mapIndexedNotNull { index, suggestion -> suggestion.toSafeSuggestion(index) }
            .distinctBy { it.id }
            .sortedByDescending { it.impact }
            .take(MAX_VISIBLE_SUGGESTIONS)

        return cleaned.ifEmpty { fallback.take(MAX_VISIBLE_SUGGESTIONS) }
    }

    private fun buildAfterAnalysisPrompt(analysis: SketchAnalysis): String = """
        The user finished drawing. Based on this AI analysis, return exactly 5 suggestions that will help the user improve the design before rendering.

        Goals:
        - Keep the user's original idea and motif identity.
        - Improve realism, line quality, cultural authenticity, surface fit, composition, and render readiness.
        - Suggestions must be selectable by the user, not automatic commands.
        - Each previewHint must be useful inside an image-render prompt.

        Analysis:
        ${gson.toJson(analysis)}

        Return ONLY JSON in this exact shape:
        {
          "suggestions": [
            {
              "id": "short-stable-id",
              "category": "LINE|SYMMETRY|TEMPLATE|CULTURAL|PRINT|COLOR",
              "location": {"x":0.0,"y":0.0,"w":1.0,"h":1.0},
              "title": "short user-facing title",
              "explanation": "why this helps",
              "principle": "design principle",
              "culturalContext": "cultural/material context",
              "impact": 1,
              "autoFixable": true,
              "previewHint": "instruction to apply if user accepts"
            }
          ]
        }
    """.trimIndent()

    private fun buildAfterRenderPrompt(
        assessment: RenderAssessment,
        feedback: SatisfactionFeedback?
    ): String = """
        The user already rendered once. Based on this render assessment and optional user feedback, return exactly 5 next-step refinement suggestions.

        Goals:
        - Correct the weakest quality dimensions first.
        - Preserve what the user likes.
        - Move closer to the user's mental image, not a random new style.
        - Each previewHint must be ready to append to the next render prompt.

        Render assessment:
        ${gson.toJson(assessment)}

        User feedback:
        ${feedback?.toSuggestionHint().orEmpty()}

        Return ONLY JSON with the same SuggestionsResponse shape and exactly 5 suggestions.
    """.trimIndent()

    private fun Suggestion.toSafeSuggestion(index: Int): Suggestion? = runCatching {
        val safeRect = NormalizedRect(
            x = location.x.coerceIn(0f, 1f),
            y = location.y.coerceIn(0f, 1f),
            w = location.w.coerceIn(0f, 1f),
            h = location.h.coerceIn(0f, 1f)
        )
        copy(
            id = id.ifBlank { "ai-suggestion-${index + 1}" },
            location = safeRect,
            title = title.ifBlank { "Improve design quality" }.take(MAX_TITLE_CHARS),
            explanation = explanation.ifBlank { "This makes the next render closer to the user's vision." }.take(MAX_BODY_CHARS),
            principle = principle.ifBlank { "Improve realism while preserving structure." }.take(MAX_BODY_CHARS),
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
        const val SYSTEM_PROMPT =
            "You are Mawaai's design coach. Produce concise, culturally respectful, photorealism-focused suggestions. Return valid JSON only."
    }
}
