package com.mawaai.love.app.design.render

import android.graphics.Bitmap
import android.graphics.PointF
import com.mawaai.love.app.design.ai.processors.BlendMode
import com.mawaai.love.app.design.ai.processors.BlendModeProcessor
import com.mawaai.love.app.design.ai.processors.PerspectiveWarpProcessor
import com.mawaai.love.app.design.domain.model.Template
import com.mawaai.love.app.design.domain.model.TemplateMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.core.Size
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateCompositor @Inject constructor(
    private val templates: TemplateAssetManager,
    private val warp: PerspectiveWarpProcessor,
    private val blender: BlendModeProcessor
) {

    suspend fun compose(template: Template, artwork: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        // base is owned by TemplateAssetManager's LruCache. We must NOT recycle.
        val base = templates.loadBitmap(template)
        val rules = blendFor(template.categoryId, template.metadata)
        val quad = quadFor(
            normalizedQuad = template.metadata?.targetQuad,
            categoryDefault = defaultQuadFor(template.categoryId),
            width = base.width,
            height = base.height,
            insetFraction = rules.insetFraction
        )
        val warped = warp.warp(
            source = artwork,
            destinationSize = Size(base.width.toDouble(), base.height.toDouble()),
            destinationQuad = quad
        )
        val composited = blender.blend(
            base = base,
            overlay = warped,
            mode = rules.mode,
            overlayAlpha = rules.overlayAlpha
        )
        if (warped !== composited) warped.recycle()
        composited
    }

    private fun blendFor(categoryId: String, metadata: TemplateMetadata?): BlendRules {
        val defaults = when (categoryId) {
            // Henna inks darkly onto skin — MULTIPLY simulates the dye.
            "henna" -> BlendRules(mode = BlendMode.MULTIPLY, overlayAlpha = 0.85, insetFraction = 0.18f)
            // Abaya fabric needs the artwork to ride on top of folds — OVERLAY.
            "abaya" -> BlendRules(mode = BlendMode.OVERLAY, overlayAlpha = 0.75, insetFraction = 0.22f)
            // Walls are flat painted surfaces — NORMAL keeps the artwork crisp.
            "walls" -> BlendRules(mode = BlendMode.NORMAL, overlayAlpha = 0.95, insetFraction = 0.12f)
            // Sudanese thob: lightweight draped fabric; slightly higher alpha
            // than abaya since raqma / fatla patterns need to read through.
            "thob_sudani" -> BlendRules(mode = BlendMode.OVERLAY, overlayAlpha = 0.82, insetFraction = 0.20f)
            else -> BlendRules(mode = BlendMode.NORMAL, overlayAlpha = 0.8, insetFraction = 0.15f)
        }
        if (metadata == null) return defaults
        return defaults.copy(
            mode = metadata.blendMode?.let { parseBlend(it) } ?: defaults.mode,
            overlayAlpha = metadata.overlayAlpha ?: defaults.overlayAlpha
        )
    }

    private fun parseBlend(name: String): BlendMode? = runCatching {
        BlendMode.valueOf(name.uppercase())
    }.getOrNull()

    /**
     * Resolves the destination quad. Priority:
     *  1. Per-template `targetQuad` from `templates.json` (best — authored).
     *  2. Category-tuned default quad — biased toward where subjects
     *     usually sit in stock photos (chest/torso for abaya/thob,
     *     hand region for henna, painting area for walls).
     *  3. Last-resort centered-with-inset quad — same as the old
     *     behavior, kept as a final fallback for unknown categories.
     *
     * The category default is a real visual improvement over the prior
     * centered-22%-inset rule because most photos in our catalog place
     * the subject off-centre vertically (model fills the upper-mid of
     * the frame, hand pose touches the bottom edge, etc.).
     */
    private fun quadFor(
        normalizedQuad: List<PointF>?,
        categoryDefault: List<PointF>?,
        width: Int,
        height: Int,
        insetFraction: Float
    ): List<PointF> {
        val source = normalizedQuad?.takeIf { it.size == 4 } ?: categoryDefault
        if (source != null && source.size == 4) {
            return source.map { pt ->
                PointF(
                    (pt.x * width).coerceIn(0f, width.toFloat()),
                    (pt.y * height).coerceIn(0f, height.toFloat())
                )
            }
        }
        return centeredQuad(width, height, insetFraction)
    }

    private fun defaultQuadFor(categoryId: String): List<PointF>? =
        TemplateQuadDefaults.forCategory(categoryId)

    private fun centeredQuad(width: Int, height: Int, inset: Float): List<PointF> {
        val dx = width * inset
        val dy = height * inset
        val left = dx
        val top = dy
        val right = width - dx
        val bottom = height - dy
        return listOf(
            PointF(left, top),
            PointF(right, top),
            PointF(right, bottom),
            PointF(left, bottom)
        )
    }

    private data class BlendRules(
        val mode: BlendMode,
        val overlayAlpha: Double,
        val insetFraction: Float
    )
}
