package com.mawaai.love.app.design.ai.render

data class RenderPrompt(
    val structurePreservation: String,
    val templateIntelligence: String,
    val baseDirection: String,
    val palette: String?,
    val colorOverride: String?,
    val refinements: String?,
    val realismDirection: String = REALISM_DIRECTION,
    val materialPhysics: String = MATERIAL_PHYSICS,
    val cameraAndLighting: String = CAMERA_AND_LIGHTING,
    val negativePrompt: String = NEGATIVE_PROMPT,
    val finalImageOnly: String = "Final image only — no annotations, labels, text, watermarks, UI chrome, borders, frames, or before/after panels."
) {
    fun toPromptString(): String = listOfNotNull(
        structurePreservation,
        templateIntelligence,
        baseDirection,
        realismDirection,
        materialPhysics,
        cameraAndLighting,
        palette,
        colorOverride,
        refinements,
        negativePrompt,
        finalImageOnly
    ).filter { it.isNotBlank() }.joinToString(" ")

    companion object {
        const val REALISM_DIRECTION =
            "PHOTOREALISM TARGET: Convert the user drawing into a finished real-world photographed design, not a flat sticker, icon, poster, or digital illustration. Preserve the user's motif layout while making the output look physically manufactured and captured by a camera."

        const val MATERIAL_PHYSICS =
            "MATERIAL PHYSICS: The artwork must inherit the target surface texture, curvature, seams, folds, pores, grain, glaze, thread, paint thickness, edge wear, occlusion, and contact shadows. Let highlights and shadows pass over the design so it belongs to the surface."

        const val CAMERA_AND_LIGHTING =
            "CAMERA + LIGHTING: Use realistic studio/product photography, natural lens perspective, soft directional key light, gentle fill light, ambient occlusion, subtle depth of field, and physically plausible reflections."

        const val NEGATIVE_PROMPT =
            "AVOID: cartoon, vector art, flat mockup, sticker look, floating design, pasted overlay, plastic shine on fabric, impossible shadows, blurry motif, distorted anatomy, extra fingers, fake text, watermark, logo, frame, UI, collage, low resolution, oversaturated colors."
    }
}