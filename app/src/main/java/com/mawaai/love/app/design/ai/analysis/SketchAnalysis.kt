package com.mawaai.love.app.design.ai.analysis

data class SketchAnalysis(
    val artStyle: String,
    val culturalOrigin: String,
    val symmetry: Symmetry,
    val lineQuality: LineQuality,
    val composition: Composition,
    val sketchStructure: SketchStructure,
    val templateMapping: TemplateMapping,
    val templateFit: TemplateFit,
    val findings: List<Finding>
) {
    data class Symmetry(
        val type: String,
        val accuracyPct: Int,
        val weakerSide: String,
        val notes: String
    )

    data class LineQuality(
        val confidence: Int,
        val consistency: Int,
        val shakiness: Int,
        val weightVarianceNotes: String
    )

    data class Composition(
        val visualCenterX: Float,
        val visualCenterY: Float,
        val balanceScore: Int,
        val negativeSpacePct: Int,
        val hierarchyNotes: String
    )

    data class SketchStructure(
        val primaryMotifs: List<String>,
        val strokeFlow: String,
        val proportionNotes: String,
        val mustPreserve: List<String>
    )

    data class TemplateMapping(
        val surfaceType: String,
        val primaryZone: String,
        val safeZones: List<String>,
        val lightingDirection: String,
        val maskingNotes: String,
        val surfaceFitNotes: String
    )

    data class TemplateFit(
        val scaleMatch: Int,
        val densityMatch: Int,
        val styleCompat: Int,
        val blockers: List<String>
    )

    data class Finding(
        val id: String,
        val severity: Severity,
        val region: NormalizedRect,
        val what: String,
        val why: String,
        val principle: String,
        val culturalContext: String
    ) {
        enum class Severity { INFO, WARNING, CRITICAL }
    }
}

data class NormalizedRect(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
) {
    init {
        require(x in 0f..1f) { "x must be in 0..1, was $x" }
        require(y in 0f..1f) { "y must be in 0..1, was $y" }
        require(w in 0f..1f) { "w must be in 0..1, was $w" }
        require(h in 0f..1f) { "h must be in 0..1, was $h" }
    }
}
