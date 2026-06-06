package com.mawaai.love.app.design.ai.render

import android.graphics.Bitmap
import com.mawaai.love.app.data.database.entities.TemplateEntity
import com.mawaai.love.app.design.ai.analysis.SketchAnalysis
import com.mawaai.love.app.design.ai.gateway.ImageEditFallbackChain
import com.mawaai.love.app.design.ai.quality.HeuristicQualityCheck
import com.mawaai.love.app.design.ai.suggestions.Suggestion
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase-5 render orchestrator: takes a sketch + template + accepted suggestions,
 * composes the structure-preserving prompt via [RenderPromptBuilder], runs the
 * heuristic quality gate, then drives the image-edit fallback chain to produce
 * the final rendered bitmap.
 *
 * Goes through the gateway via [ImageEditFallbackChain] (the image-edit
 * analogue of [com.mawaai.love.app.design.ai.gateway.VisionFallbackChain]),
 * so a provider deprecation never reaches the user.
 *
 * MT-032 quality gate: when an [SketchAnalysis] is supplied, the renderer
 * runs [HeuristicQualityCheck] BEFORE calling the chain so a doomed render
 * never spends HuggingFace quota. The AI post-render reviewer
 * ([com.mawaai.love.app.design.ai.quality.AiQualityReviewer]) is called by
 * the caller after a successful render because it needs both bitmaps.
 *
 * Mirrors the Lovable Creative Studio render flow from `lib/render.functions.ts`
 * -- structure preservation rule first, then template intelligence, then surface
 * direction, realism/material/camera constraints, palette, color override,
 * accepted refinements, negative prompt, and a terminator forbidding
 * annotations/labels/watermarks.
 */
@Singleton
class ImageEditRenderer @Inject constructor(
    private val promptBuilder: RenderPromptBuilder,
    private val chain: ImageEditFallbackChain,
) {

    /**
     * Render [sketch] onto [template] honoring the optional [colorOverride]
     * and any user-accepted [acceptedSuggestions].
     *
     * When [analysis] is provided, the renderer runs the cheap heuristic
     * quality gate first; if it fails, the call returns [Result.failure]
     * with the typed quality result before spending any provider quota.
     *
     * @return [Result.success] with the rendered bitmap, or a typed
     * [Result.failure] describing why the chain or the quality gate declined.
     */
    suspend fun render(
        sketch: Bitmap,
        template: TemplateEntity,
        colorOverride: String? = null,
        acceptedSuggestions: List<Suggestion> = emptyList(),
        analysis: SketchAnalysis? = null,
    ): Result<Bitmap> {
        // MT-032 quality gate -- block doomed renders before spending API quota.
        analysis?.let { sketchAnalysis ->
            val quality = HeuristicQualityCheck.evaluate(sketchAnalysis)
            if (!quality.passed) {
                return Result.failure(
                    QualityGateBlocked(
                        message = "Quality gate blocked render: ${quality.issues.joinToString(" | ")}",
                        blockers = quality.issues,
                    )
                )
            }
        }

        val renderPrompt = promptBuilder.build(
            template = template,
            colorOverride = colorOverride,
            acceptedSuggestions = acceptedSuggestions,
        )
        return chain.renderFromSketch(sketch = sketch, prompt = flattenPrompt(renderPrompt))
    }

    /**
     * Thrown into [Result.failure] when the heuristic quality gate refuses
     * the render. Callers can pattern-match on this to show a friendly
     * "design isn't ready yet" message instead of a generic error.
     */
    class QualityGateBlocked(
        message: String,
        val blockers: List<String>,
    ) : Exception(message)

    companion object {
        const val FINAL_IMAGE_TERMINATOR =
            "Final image only -- no annotations, labels, text, watermarks, UI chrome, borders, frames, or before/after panels."

        /**
         * Collapse the multi-field [RenderPrompt] into the single text prompt the
         * downstream img2img model expects.
         *
         * Field order:
         *   structure -> template intelligence -> surface direction -> realism
         *   direction -> material physics -> camera/lighting -> palette ->
         *   color override -> user refinements -> negative prompt -> terminator.
         *
         * Null / blank fields are silently dropped so the model never sees empty
         * preamble fragments. Pure function -- safe to unit-test without
         * constructing the renderer.
         */
        internal fun flattenPrompt(p: RenderPrompt): String = listOfNotNull(
            p.structurePreservation,
            p.templateIntelligence,
            p.baseDirection,
            p.realismDirection,
            p.materialPhysics,
            p.cameraAndLighting,
            p.palette?.let { "Honor the traditional palette where natural: $it." },
            p.colorOverride,
            p.refinements,
            p.negativePrompt,
            p.finalImageOnly.ifBlank { FINAL_IMAGE_TERMINATOR },
        )
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")
    }
}