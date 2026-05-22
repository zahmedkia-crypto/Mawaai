package com.mawaai.love.app.design.template

import android.graphics.Bitmap
import com.mawaai.love.app.design.ai.gemini.GeminiVisionClient
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Typed failure surface for [TemplateAnalyzer]. Always carries the requested
 * [templateType] so the caller can log/UI a meaningful fallback without
 * re-parsing the error message.
 */
class TemplateAnalysisException(
    message: String,
    val templateType: TemplateType,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Phase 3.3 — runtime enricher for [TemplateContext].
 *
 * Pipeline:
 *  1. Look up the static base context via [TemplateRegistry.contextFor]. If
 *     the type has no base ([TemplateType.MURAL] / [TemplateType.CUSTOM]),
 *     return [Result.failure] carrying a [TemplateAnalysisException] so the
 *     caller can fall back cleanly.
 *  2. If [GeminiVisionClient] is configured, ask it to classify the visible
 *     style. Append the classification to [TemplateContext.designConstraints]
 *     as a soft hint the orchestrator can fold into its ControlNet prompt.
 *  3. Otherwise (no API key, network error, vision call fails) return the
 *     base context unchanged — wrapped in [Result.success], not failure.
 *     "No Gemini configured" is normal operation, not an error.
 *
 * Why [classifyStyle] rather than a dedicated `analyzeTemplate(…)`:
 * [GeminiVisionClient] currently exposes only style/suggestion methods. Wiring
 * the full lighting/shadow/warp analysis (per the master prompt's Phase 3.3
 * spec) requires adding a new method on the vision client, which is its own
 * focused change. Using the existing `classifyStyle` shows the enrichment
 * pipeline end-to-end against a real Gemini surface today, without touching
 * a 21 KB client file we'd have to overwrite wholesale.
 *
 * The vision client is injected via [dagger.Lazy] so the AI graph is not
 * fully realised at app start — matches the pattern documented in
 * `AGENTS.md` for heavy Cloudflare / RemoveBg / HuggingFace clients.
 */
@Singleton
class TemplateAnalyzer @Inject constructor(
    private val visionClient: Lazy<GeminiVisionClient>
) {

    suspend fun analyze(
        templateType: TemplateType,
        templateImage: Bitmap
    ): Result<TemplateContext> = withContext(Dispatchers.IO) {
        try {
            val base = TemplateRegistry.contextFor(templateType)
                ?: return@withContext Result.failure(
                    TemplateAnalysisException(
                        "No base TemplateContext registered for ${templateType.name}",
                        templateType
                    )
                )

            val client = visionClient.get()
            if (!client.isConfigured) {
                // Normal case on devices with no GEMINI_API_KEY. Skip the
                // network call entirely and return the static base.
                return@withContext Result.success(base)
            }

            val styleHint = runCatching { client.classifyStyle(templateImage) }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }

            val enriched = if (styleHint == null) {
                base
            } else {
                base.copy(
                    designConstraints =
                        base.designConstraints + "Vision-detected style: $styleHint"
                )
            }
            Result.success(enriched)
        } catch (t: Throwable) {
            Result.failure(
                TemplateAnalysisException(
                    "Failed to analyse template ${templateType.name}: ${t.message}",
                    templateType,
                    t
                )
            )
        }
    }
}
