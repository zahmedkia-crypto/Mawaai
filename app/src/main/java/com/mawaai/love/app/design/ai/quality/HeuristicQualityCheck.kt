package com.mawaai.love.app.design.ai.quality

import com.mawaai.love.app.design.ai.analysis.SketchAnalysis

/**
 * MT-030: deterministic, AI-free pre-check that gates whether the renderer
 * should even spend a HuggingFace inference on this sketch / template pair.
 *
 * Mirrors the heuristic block in Lovable's `render.functions.ts:validateRenderQuality`
 * (the `heuristic = { passed, score, blockers, checks }` object that runs
 * before the multi-modal LLM review).
 *
 * Scoring formula (verbatim port from the Lovable TS):
 *   score = ((scaleMatch + densityMatch + styleCompat) / 30) * 100
 *
 * Blockers come from:
 *  - scale_match < 4  -> surface scale is wrong (motif too big / too small)
 *  - density_match < 4 -> sketch is too dense for the surface constraints
 *  - any AI-supplied blockers in `templateFit.blockers` (capped at 3)
 *
 * The returned [RenderQuality] is shaped exactly like the AI reviewer's
 * output so callers can swap heuristic vs. AI gating without changing
 * downstream code.
 *
 * Pure function -- no Hilt, no IO, no coroutines. Safe to call from any
 * thread, trivially unit-testable.
 */
object HeuristicQualityCheck {

    /**
     * Evaluate [analysis] and produce a [RenderQuality] describing whether
     * this sketch is safe to render given the template's structural fit
     * scores.
     */
    fun evaluate(analysis: SketchAnalysis): RenderQuality {
        val fit = analysis.templateFit

        val blockers = mutableListOf<String>()
        if (fit.scaleMatch < SCALE_MIN) {
            blockers += "Scale match too low for realistic surface fit (${fit.scaleMatch}/10)."
        }
        if (fit.densityMatch < DENSITY_MIN) {
            blockers += "Sketch density exceeds the template surface constraints (${fit.densityMatch}/10)."
        }
        // Append AI-supplied template_fit blockers (capped to match Lovable).
        blockers += fit.blockers.take(MAX_AI_BLOCKERS)

        val sumOver30 = (fit.scaleMatch + fit.densityMatch + fit.styleCompat).coerceIn(0, MAX_SUM)
        val score = (sumOver30 * SCALE_TO_PERCENT) / MAX_SUM

        return RenderQuality(
            compositionPreservation = score,
            surfaceFit = score,
            lightingRealism = score,
            passed = blockers.isEmpty(),
            issues = blockers,
            notes = if (blockers.isEmpty()) {
                "Heuristic pre-check passed (score $score)."
            } else {
                "Heuristic pre-check blocked: ${blockers.size} issue(s)."
            },
        )
    }

    private const val SCALE_MIN = 4
    private const val DENSITY_MIN = 4
    private const val MAX_AI_BLOCKERS = 3
    private const val MAX_SUM = 30
    private const val SCALE_TO_PERCENT = 100
}
