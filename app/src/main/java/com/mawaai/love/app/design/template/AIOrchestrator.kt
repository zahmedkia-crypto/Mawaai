package com.mawaai.love.app.design.template

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Structured plan describing how the AI pipeline should process a sketch for
 * a given template family.
 *
 * Pure data — producing a plan has **no side effects** and does **not invoke**
 * any AI provider. Consumed by the existing
 * [com.mawaai.love.app.design.ai.AIEngineImpl] (in a follow-up PR) or by any
 * caller that wants to inspect/log/A-B-test the prompt set before generation.
 *
 * @property context                 Enriched [TemplateContext] returned by
 *                                   [TemplateAnalyzer]. May include vision-
 *                                   detected style hints appended to
 *                                   [TemplateContext.designConstraints].
 * @property visionSystemPrompt      Verbatim system prompt for the Vision
 *                                   model (Model A). Defaults to the
 *                                   sketch-analysis prompt; orchestrators
 *                                   targeting template image analysis can
 *                                   override to [SystemPrompts.TEMPLATE_ANALYSIS].
 * @property enhancementPrompt       Structured ControlNet prompt (Model B)
 *                                   keyed to [context.type].
 * @property backgroundRemovalPrompt System prompt for the background-removal
 *                                   step (Model C).
 * @property upscalePrompt           System prompt for the upscaler step
 *                                   (Model D).
 * @property detectedStyleHint       Optional textual style hint extracted by
 *                                   the analyser. Mirrors the entry appended
 *                                   to [TemplateContext.designConstraints]
 *                                   when Gemini classifies a style; surfaced
 *                                   separately here so logs and UI can show
 *                                   `"detected style: X"` without re-parsing
 *                                   the constraints list.
 */
data class OrchestrationPlan(
    val context: TemplateContext,
    val visionSystemPrompt: String = SystemPrompts.VISION_ANALYSIS,
    val enhancementPrompt: EnhancementPrompt,
    val backgroundRemovalPrompt: String = SystemPrompts.BACKGROUND_REMOVAL,
    val upscalePrompt: String = SystemPrompts.UPSCALE,
    val detectedStyleHint: String? = null
)

/**
 * Phase 2 — thin orchestrator that converts a [TemplateType] + reference
 * image into a structured [OrchestrationPlan].
 *
 * Steps:
 *  1. Delegate to [TemplateAnalyzer.analyze] for an enriched [TemplateContext].
 *  2. Run [EnhancementPromptBuilder.build] over the enriched context.
 *  3. Extract any `"Vision-detected style: …"` entry [TemplateAnalyzer] may
 *     have appended to [TemplateContext.designConstraints] and surface it as
 *     [OrchestrationPlan.detectedStyleHint].
 *  4. Return the assembled plan.
 *
 * The orchestrator is a [Singleton] `@Inject`-constructed wrapper; no Hilt
 * module bindings are required because [TemplateAnalyzer] is already
 * `@Singleton` `@Inject`-constructed.
 *
 * Why this class does NOT invoke `AIEngine` directly:
 *  - `AIEngineImpl.kt` is 27 KB and on AGENTS.md's "do NOT read in full" list.
 *    Modifying it via the GitHub Contents API would require a wholesale
 *    overwrite, which violates the surgical-changes rule.
 *  - Producing a pure plan keeps Phase 2 reviewable in isolation. Wiring the
 *    plan into the existing engine is its own focused PR, best done from a
 *    local checkout where `str_replace` is available.
 *
 * Errors from analysis propagate as [Result.failure] carrying the typed
 * [TemplateAnalysisException].
 */
@Singleton
class AIOrchestrator @Inject constructor(
    private val analyzer: TemplateAnalyzer
) {
    suspend fun plan(
        templateType: TemplateType,
        templateImage: Bitmap
    ): Result<OrchestrationPlan> {
        val context = analyzer.analyze(templateType, templateImage)
            .getOrElse { return Result.failure(it) }

        val enhancement = EnhancementPromptBuilder.build(context)

        val styleHint = context.designConstraints
            .firstOrNull { it.startsWith(STYLE_HINT_PREFIX) }
            ?.removePrefix(STYLE_HINT_PREFIX)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        return Result.success(
            OrchestrationPlan(
                context = context,
                enhancementPrompt = enhancement,
                detectedStyleHint = styleHint
            )
        )
    }

    private companion object {
        // Must match the prefix written by TemplateAnalyzer when it appends
        // the classifyStyle() result to designConstraints. Keeping the
        // prefix here as a const ties the two together at compile time.
        const val STYLE_HINT_PREFIX = "Vision-detected style:"
    }
}
