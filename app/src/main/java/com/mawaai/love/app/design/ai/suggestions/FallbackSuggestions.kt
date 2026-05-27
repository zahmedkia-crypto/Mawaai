package com.mawaai.love.app.design.ai.suggestions

import com.mawaai.love.app.data.database.entities.TemplateEntity
import com.mawaai.love.app.design.ai.analysis.NormalizedRect
import com.mawaai.love.app.design.ai.intelligence.SurfaceCatalog

object FallbackSuggestions {
    fun build(template: TemplateEntity): List<Suggestion> {
        val profile = SurfaceCatalog.forTemplate(template)
        
        return listOf(
            Suggestion(
                id = "s_001",
                category = Suggestion.Category.TEMPLATE,
                location = NormalizedRect(0.2f, 0.2f, 0.6f, 0.6f),
                title = "Adapt to ${profile.label}",
                explanation = "Ensure the design flows naturally with the ${profile.targetSurface} contours.",
                principle = "Surface Harmony",
                culturalContext = "Respecting the natural canvas.",
                impact = 80,
                autoFixable = true,
                previewHint = "The AI will gently warp the design to fit."
            ),
            Suggestion(
                id = "s_002",
                category = Suggestion.Category.LINE,
                location = NormalizedRect(0.3f, 0.3f, 0.4f, 0.4f),
                title = "Refine Line Quality",
                explanation = "Smoothen strokes to match traditional craftsmanship standards.",
                principle = "Technical Precision",
                culturalContext = "Emulating master artisan steady-hand techniques.",
                impact = 70,
                autoFixable = true,
                previewHint = "Lines will be stabilized and cleaned."
            )
        )
    }
}
