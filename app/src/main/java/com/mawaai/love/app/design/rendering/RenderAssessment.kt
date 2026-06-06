package com.mawaai.love.app.design.rendering

/**
 * Deterministic assessment model for deciding whether a generated render is
 * ready to show/export or should be regenerated with stronger instructions.
 */
data class RenderAssessment(
    val realism: Float,
    val structurePreservation: Float,
    val materialIntegration: Float,
    val lightingConsistency: Float,
    val notes: String = ""
) {
    val overall: Float
        get() = listOf(realism, structurePreservation, materialIntegration, lightingConsistency).average().toFloat()

    val isProductionReady: Boolean
        get() = realism >= REALISM_MIN &&
            structurePreservation >= STRUCTURE_MIN &&
            materialIntegration >= MATERIAL_MIN &&
            lightingConsistency >= LIGHTING_MIN

    fun retryInstruction(): String = buildString {
        append("Improve render realism while preserving the user's drawing. ")
        if (realism < REALISM_MIN) append("Make the image look like real product photography, not digital art. ")
        if (structurePreservation < STRUCTURE_MIN) append("Preserve the exact motif layout and proportions from the sketch. ")
        if (materialIntegration < MATERIAL_MIN) append("Blend the design into the surface texture, folds, curvature, pores, glaze, or grain. ")
        if (lightingConsistency < LIGHTING_MIN) append("Match shadows, highlights, reflections, and ambient occlusion to the scene. ")
        if (notes.isNotBlank()) append(notes)
    }.trim()

    companion object {
        const val REALISM_MIN = 8.5f
        const val STRUCTURE_MIN = 9.0f
        const val MATERIAL_MIN = 8.0f
        const val LIGHTING_MIN = 8.0f
    }
}

object RenderAssessmentPolicy {
    fun manual(
        realism: Float,
        structurePreservation: Float,
        materialIntegration: Float,
        lightingConsistency: Float,
        notes: String = ""
    ): RenderAssessment = RenderAssessment(
        realism = realism.coerceIn(0f, 10f),
        structurePreservation = structurePreservation.coerceIn(0f, 10f),
        materialIntegration = materialIntegration.coerceIn(0f, 10f),
        lightingConsistency = lightingConsistency.coerceIn(0f, 10f),
        notes = notes
    )
}
