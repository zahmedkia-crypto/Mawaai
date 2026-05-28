package com.mawaai.love.app.design.ai.render

import android.graphics.Bitmap
import com.mawaai.love.app.data.database.entities.TemplateEntity
import com.mawaai.love.app.design.ai.gateway.ImageEditFallbackChain
import com.mawaai.love.app.design.ai.suggestions.Suggestion
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase-5 render orchestrator: takes a sketch + template + accepted suggestions,
 * composes the structure-preserving prompt via [RenderPromptBuilder], then drives
 * the image-edit fallback chain to produce the final rendered bitmap.
 *
 * Goes through the gateway via [ImageEditFallbackChain] (the image-edit
 * analogue of [com.mawaai.love.app.design.ai.gateway.VisionFallbackChain]),
 * so a provider deprecation never reaches the user.
 *
 * Mirrors the Lovable Creative Studio render flow from `lib/render.functions.ts`
 * -- structure preservation rule first, then template intelligence, then surface
 * direction, palette, color override, accepted refinements, and a terminator
 * forbidding annotations/labels/watermarks.
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
     * @return [Result.success] with the rendered bitmap, or a typed
     * [Result.failure] describing why the chain declined.
     */
    suspend fun render(
        sketch: Bitmap,
        template: TemplateEntity,
        colorOverride: String? = null,
        acceptedSuggestions: List<Suggestion> = emptyList(),
    ): Result<Bitmap> {
        val renderPrompt = promptBuilder.build(
            template = template,
            colorOverride = colorOverride,
            acceptedSuggestions = acceptedSuggestions,
        )
        return chain.renderFromSketch(sketch = sketch, prompt = flattenPrompt(renderPrompt))
    }

    companion object {
        const val FINAL_IMAGE_TERMINATOR =
            "Final image only -- no annotations, labels, text, watermarks, or framing."

        /**
         * Collapse the multi-field [RenderPrompt] into the single text prompt the
         * downstream img2img model expects.
         *
         * Field order is the verbatim port of the Lovable TS pipeline
         * (`render.functions.ts` ~ L243-254):
         *   structure -> templateIntelligence -> baseDirection -> palette ->
         *   colorOverride -> refinements -> terminator.
         *
         * Null / blank fields are silently dropped so the model never sees empty
         * preamble fragments. Pure function -- safe to unit-test without
         * constructing the renderer.
         */
        internal fun flattenPrompt(p: RenderPrompt): String = listOfNotNull(
            p.structurePreservation,
            p.templateIntelligence,
            p.baseDirection,
            p.palette?.let { "Honor the traditional palette where natural: $it." },
            p.colorOverride,
            p.refinements,
            FINAL_IMAGE_TERMINATOR,
        )
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")
    }
}
