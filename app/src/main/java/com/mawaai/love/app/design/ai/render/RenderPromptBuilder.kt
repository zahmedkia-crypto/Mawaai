package com.mawaai.love.app.design.ai.render

import com.mawaai.love.app.data.database.entities.TemplateEntity
import com.mawaai.love.app.design.ai.intelligence.SurfaceCatalog
import com.mawaai.love.app.design.ai.intelligence.SurfaceDirections
import com.mawaai.love.app.design.ai.intelligence.templateIntelligencePrompt
import com.mawaai.love.app.design.ai.suggestions.Suggestion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RenderPromptBuilder @Inject constructor() {

    fun build(
        template: TemplateEntity,
        colorOverride: String?,
        acceptedSuggestions: List<Suggestion>
    ): RenderPrompt {
        val profile = SurfaceCatalog.forTemplate(template)
        
        return RenderPrompt(
            structurePreservation = "CRITICAL RULE: Preserve the exact spatial structure and composition of the user's sketch. The sketch is the authoritative layout. Do not move, rotate, or re-scale the primary elements unless they violate the masking rules.",
            templateIntelligence = templateIntelligencePrompt(template),
            baseDirection = SurfaceDirections.forProfile(profile),
            palette = template.traditionalPaletteJson.takeIf { it.isNotBlank() },
            colorOverride = colorOverride?.let { "OVERRIDE COLOR: Use the specific hex color $it for the primary design elements (e.g. the henna paste or the garment embroidery)." },
            refinements = acceptedSuggestions.takeIf { it.isNotEmpty() }?.let { suggestions ->
                "ACCEPTED REFINEMENTS: " + suggestions.joinToString("; ") { it.previewHint }
            }
        )
    }
}
