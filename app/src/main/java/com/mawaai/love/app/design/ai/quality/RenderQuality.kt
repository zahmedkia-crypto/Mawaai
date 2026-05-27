package com.mawaai.love.app.design.ai.quality

data class RenderQuality(
    val compositionPreservation: Int,   // 0..100
    val surfaceFit: Int,                // 0..100
    val lightingRealism: Int,           // 0..100
    val passed: Boolean,
    val issues: List<String>,           // max 6
    val notes: String                   // max 500 chars
) {
    val overallScore: Int get() = (compositionPreservation + surfaceFit + lightingRealism) / 3
}

data class QualityResponse(
    val quality: RenderQuality
)
