package com.mawaai.love.app.design.ai.intelligence

import com.mawaai.love.app.data.database.entities.TemplateEntity

/**
 * Phase 2 prompt block. Embedded in every analysis + render call so the AI
 * has explicit knowledge of the surface constraints, masking, perspective,
 * and material response.
 */
fun templateIntelligencePrompt(template: TemplateEntity): String {
    val profile = SurfaceCatalog.forTemplate(template)
    val zones = template.zonesJson.ifBlank { "use the whole visible surface as one safe zone" }
    val lighting = template.primaryLight.ifBlank { "soft front" }
    val material = template.material.ifBlank { profile.label }
    val reflectance = template.surfaceReflectance.ifBlank { "medium" }

    return buildString {
        appendLine("PHASE 2 TEMPLATE INTELLIGENCE:")
        appendLine("- Surface type: ${profile.id} (${profile.label})")
        appendLine("- Target surface: ${profile.targetSurface}")
        appendLine("- Zones: $zones")
        appendLine("- Constraints: ${profile.constraints.joinToString("; ")}")
        appendLine("- Lighting: $lighting, material=$material, reflectance=$reflectance")
        appendLine("- Masking rules: ${profile.maskingRules.joinToString("; ")}")
        appendLine("- Perspective rules: ${profile.perspectiveRules.joinToString("; ")}")
        appendLine("- Material response: ${profile.materialResponse}")
        appendLine("- Coverage ceiling: ${template.maxCoveragePct}%")
    }
}
