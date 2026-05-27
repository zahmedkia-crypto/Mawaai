package com.mawaai.love.app.design.ai.suggestions

import com.mawaai.love.app.design.ai.analysis.NormalizedRect

data class Suggestion(
    val id: String,
    val category: Category,
    val location: NormalizedRect,
    val title: String,
    val explanation: String,
    val principle: String,
    val culturalContext: String,
    val impact: Int,
    val autoFixable: Boolean,
    val previewHint: String
) {
    enum class Category { LINE, SYMMETRY, TEMPLATE, CULTURAL, PRINT, COLOR }
}

data class SuggestionsResponse(
    val suggestions: List<Suggestion>
)
