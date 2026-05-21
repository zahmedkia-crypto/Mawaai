package com.mawaai.love.app.design.domain.model

import android.graphics.PointF

data class Template(
    val id: String,
    val categoryId: String,
    val assetPath: String,
    val metadata: TemplateMetadata? = null
) {
    val displayName: String
        get() = id.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/**
 * Optional per-template tuning that overrides the category-level defaults in
 * [com.mawaai.love.app.design.render.TemplateCompositor]. All fields are
 * nullable so the compositor can mix-and-match (e.g. a template authors a
 * custom quad but uses the category's blend mode).
 *
 * [targetQuad] coordinates are normalized to `[0, 1]` of the base bitmap
 * dimensions. Order is top-left, top-right, bottom-right, bottom-left.
 */
data class TemplateMetadata(
    val targetQuad: List<PointF>? = null,
    val blendMode: String? = null,
    val overlayAlpha: Double? = null
)
