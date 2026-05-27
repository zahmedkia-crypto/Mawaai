package com.mawaai.love.app.design.ai.analysis

import com.mawaai.love.app.data.database.entities.TemplateEntity
import com.mawaai.love.app.design.ai.intelligence.SurfaceCatalog

object FallbackAnalysis {
    fun build(template: TemplateEntity): SketchAnalysis {
        val profile = SurfaceCatalog.forTemplate(template)
        
        return SketchAnalysis(
            artStyle = "Manual Sketch",
            culturalOrigin = "Modern / Traditional Mix",
            symmetry = SketchAnalysis.Symmetry(
                type = "none",
                accuracyPct = 50,
                weakerSide = "none",
                notes = "Heuristic fallback - symmetry not measured."
            ),
            lineQuality = SketchAnalysis.LineQuality(
                confidence = 5,
                consistency = 5,
                shakiness = 2,
                weightVarianceNotes = "Heuristic fallback."
            ),
            composition = SketchAnalysis.Composition(
                visualCenterX = 0.5f,
                visualCenterY = 0.5f,
                balanceScore = 5,
                negativeSpacePct = 40,
                hierarchyNotes = "Centered by default."
            ),
            sketchStructure = SketchAnalysis.SketchStructure(
                primaryMotifs = listOf("Hand-drawn strokes"),
                strokeFlow = "Undetermined",
                proportionNotes = "Standard proportions assumed.",
                mustPreserve = listOf("Central form")
            ),
            templateMapping = SketchAnalysis.TemplateMapping(
                surfaceType = profile.id,
                primaryZone = "Center",
                safeZones = listOf("Main surface"),
                lightingDirection = template.primaryLight,
                maskingNotes = "Follow standard ${profile.label} constraints.",
                surfaceFitNotes = "Basic fit verification."
            ),
            templateFit = SketchAnalysis.TemplateFit(
                scaleMatch = 8,
                densityMatch = 8,
                styleCompat = 10,
                blockers = emptyList()
            ),
            findings = listOf(
                SketchAnalysis.Finding(
                    id = "f_001",
                    severity = SketchAnalysis.Finding.Severity.INFO,
                    region = NormalizedRect(0.1f, 0.1f, 0.8f, 0.8f),
                    what = "Heuristic analysis active",
                    why = "The AI was unreachable or returned an invalid response.",
                    principle = "Safety Fallback",
                    culturalContext = "Preserving traditional layout patterns."
                )
            )
        )
    }
}
