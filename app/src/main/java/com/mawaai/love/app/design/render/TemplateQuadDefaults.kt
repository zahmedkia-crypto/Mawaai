package com.mawaai.love.app.design.render

import android.graphics.PointF

/**
 * Per-category default placement quads for template compositing.
 *
 * Authored quads in `templates/<category>/templates.json` always take
 * priority. When a template lacks an authored quad, the compositor +
 * the garment-color engine fall back to these per-category defaults
 * instead of the historical "centered with X% inset on all sides" rule
 * that produced visibly off-axis composites on stock photos (the
 * subject sits in the upper-mid of the frame, not the geometric centre).
 *
 * Coordinates are normalized `[0..1]` and follow the standard corner
 * order `(top-left, top-right, bottom-right, bottom-left)` that
 * `TemplateMetadata.targetQuad` uses, so this shape can be dropped
 * straight into `templates.json` if a per-template override is ever
 * authored later.
 *
 * Numbers tuned for the Mawaai stock-photo catalog as of 2026-05-13.
 * Re-tune via on-device QA if a different photo style is shipped.
 */
internal object TemplateQuadDefaults {

    /**
     * Henna: hands / feet usually fill the middle 64% width and run
     * from ~20% top down to almost the bottom edge. Tight horizontal
     * margin, generous vertical extent.
     */
    val HENNA: List<PointF> = listOf(
        PointF(0.18f, 0.20f),
        PointF(0.82f, 0.20f),
        PointF(0.82f, 0.88f),
        PointF(0.18f, 0.88f)
    )

    /**
     * Abaya: model centred horizontally; chest/torso area sits between
     * ~18% and ~82% of the frame both ways. Slightly tighter horizontal
     * margin than henna because the garment is narrower than a hand.
     */
    val ABAYA: List<PointF> = listOf(
        PointF(0.22f, 0.18f),
        PointF(0.78f, 0.18f),
        PointF(0.78f, 0.82f),
        PointF(0.22f, 0.82f)
    )

    /**
     * Sudanese thob: similar drape to abaya but slightly higher top
     * margin since toubs leave more headroom above the shoulders.
     */
    val THOB: List<PointF> = listOf(
        PointF(0.22f, 0.16f),
        PointF(0.78f, 0.16f),
        PointF(0.78f, 0.80f),
        PointF(0.22f, 0.80f)
    )

    /**
     * Walls: painting area is upper-mid of the frame, leaving room for
     * floor + ceiling. All 5 shipped wall photos already have authored
     * quads, so this default is mostly a defensive fallback for new
     * wall assets dropped without metadata.
     */
    val WALLS: List<PointF> = listOf(
        PointF(0.20f, 0.16f),
        PointF(0.80f, 0.16f),
        PointF(0.80f, 0.74f),
        PointF(0.20f, 0.74f)
    )

    /**
     * Single lookup used by every quad resolver. Returns null for
     * unknown categories so the caller can fall through to a
     * geometric centered-quad as a last resort.
     */
    fun forCategory(categoryId: String): List<PointF>? = when (categoryId) {
        "henna" -> HENNA
        "abaya" -> ABAYA
        "thob_sudani" -> THOB
        "walls" -> WALLS
        else -> null
    }
}
