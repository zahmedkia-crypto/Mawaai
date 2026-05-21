package com.mawaai.love.app.design.template

import android.graphics.Bitmap
import android.graphics.PointF
import com.mawaai.love.app.design.ai.processors.BlendMode

/**
 * Phase 3 — Template Intelligence semantic layer.
 *
 * This package describes WHAT a template *is* (taxonomy + anatomy + application
 * medium + cultural context). It is intentionally separate from:
 *
 *  - [com.mawaai.love.app.design.domain.model.Template]          — a specific
 *    asset on disk + per-template composition tuning.
 *  - [com.mawaai.love.app.design.domain.model.DesignCatalog]      — the JSON
 *    catalogue used to drive the picker UI.
 *  - [com.mawaai.love.app.design.render.TemplateCompositor]       — the runtime
 *    pipeline that warps + blends a [Template] onto an artwork bitmap.
 *
 * The intelligence layer feeds the AI orchestrator (Phase 2) with structured
 * prompts for Vision Analysis, ControlNet enhancement, background removal,
 * and upscaling, and feeds the renderer with semantic blend hints. Nothing
 * here calls into the orchestrator directly — these are pure declarations.
 */

/**
 * Taxonomy of supported template families. Drives prompt construction for
 * the vision and control-net models, and selects the matching
 * [TemplateContext] from [HennaTemplates] or [GarmentTemplates].
 *
 * [MURAL] is recognised here for completeness but is rendered through the
 * cinematic Showcase system (`design/showcase/`) rather than the standard
 * template compositor.
 *
 * [CUSTOM] is a fallback for user-supplied templates that don't map to a
 * known family — the orchestrator falls back to a generic prompt.
 */
enum class TemplateType {
    HENNA_PALM,
    HENNA_FOOT,
    HENNA_WRIST,
    ISLAMIC_ABAYA,
    SUDANESE_TOUB,
    SAUDI_THOBE,
    BISHT,
    MURAL,
    CUSTOM
}

/**
 * Physical application medium. Selects which post-processing chain runs after
 * AI generation (skin-texture blending vs. fabric embroidery vs. mural
 * compositing) and informs the textureBlendMode choice on [PlacementZone].
 */
enum class ApplicationMethod {
    /** Natural henna dye on skin. */
    HENNA_ENGRAVING,
    /** Stitched thread on fabric. */
    EMBROIDERY,
    /** Flat printed pattern on fabric. */
    FABRIC_PRINT,
    /** Paint applied to a wall surface. */
    WALL_PAINTING,
    /** Pure digital overlay with no surface simulation. */
    DIGITAL_OVERLAY
}

/**
 * Visual emphasis tier of a placement zone within a template. PRIMARY zones
 * carry the densest detail; ACCENT zones provide sparse framing motifs.
 */
enum class ZonePriority { PRIMARY, SECONDARY, ACCENT }

/**
 * A single application zone on a template surface (e.g. the dorsum of a foot,
 * the hem of a toub, the cuffs of an abaya).
 *
 * @property id              Stable identifier, lowercase snake_case.
 * @property priority        Detail tier — see [ZonePriority].
 * @property boundaryPoints  Polygon vertices normalised to `[0, 1]` of the
 *                           base template bitmap (top-left origin), matching
 *                           the convention used by
 *                           [com.mawaai.love.app.design.domain.model.TemplateMetadata.targetQuad].
 *                           Vertices are open-ended — do NOT repeat the
 *                           first point at the end.
 * @property warpIntensity   Fraction of the bounding box the perspective warp
 *                           may distort to match surface curvature, `0.0..1.0`.
 * @property textureBlendMode  How the AI-generated design composites onto the
 *                           underlying texture (uses the existing processor
 *                           [BlendMode] enum so the renderer wires straight
 *                           in without translation).
 * @property maxDesignCoverage  Upper bound on how much of the zone's area the
 *                           generated design may fill, `0.0..1.0`.
 */
data class PlacementZone(
    val id: String,
    val priority: ZonePriority,
    val boundaryPoints: List<PointF>,
    val warpIntensity: Float,
    val textureBlendMode: BlendMode,
    val maxDesignCoverage: Float
)

/**
 * Semantic specification for one template family.
 *
 * Static instances in [HennaTemplates] / [GarmentTemplates] leave
 * [lightingMap], [shadowMap], and [warpMap] null — they are populated at
 * runtime by `TemplateAnalyzer` (Phase 3.3) from a vision analysis of the
 * live template image.
 *
 * @property colorPalette  ARGB color ints (use [android.graphics.Color.rgb] /
 *                         [android.graphics.Color.argb] to construct). A
 *                         [List] is used instead of `IntArray` so data-class
 *                         equality compares the contents structurally,
 *                         matching the existing
 *                         [com.mawaai.love.app.design.domain.model.TemplateMetadata.targetQuad]
 *                         pattern (`List<PointF>` rather than
 *                         `Array<PointF>`).
 * @property warpMap       Optional flat per-pixel displacement field;
 *                         populated only at runtime from vision analysis.
 */
data class TemplateContext(
    val type: TemplateType,
    val applicationMethod: ApplicationMethod,
    val surfaceTexture: String,
    val placementZones: List<PlacementZone>,
    val culturalOrigin: String,
    val colorPalette: List<Int>,
    val designConstraints: List<String>,
    val lightingMap: Bitmap? = null,
    val shadowMap: Bitmap? = null,
    val warpMap: List<Float>? = null
)
