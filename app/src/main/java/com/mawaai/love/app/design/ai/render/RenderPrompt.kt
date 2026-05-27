package com.mawaai.love.app.design.ai.render

data class RenderPrompt(
    val structurePreservation: String,
    val templateIntelligence: String,
    val baseDirection: String,
    val palette: String?,
    val colorOverride: String?,
    val refinements: String?,
    val finalImageOnly: String = "Final image only — no annotations, labels, text, watermarks, or framing."
) {
    fun toPromptString(): String = listOfNotNull(
        structurePreservation,
        templateIntelligence,
        baseDirection,
        palette,
        colorOverride,
        refinements,
        finalImageOnly
    ).filter { it.isNotBlank() }.joinToString(" ")
}
