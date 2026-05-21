package com.mawaai.love.app.design.domain.model

/**
 * Structured output of [com.mawaai.love.app.design.ai.LocalDrawingAnalyzer]
 * and the Gemini-vision adapter on the Recommendations screen. Replaces the
 * previous unstructured `List<String>` so each suggestion can carry an
 * optional one-tap [DrawingAction] that the
 * [com.mawaai.love.app.design.ai.DrawingActionEngine] applies in place on
 * the saved artwork bitmap.
 *
 * Gemini suggestions always ship with `action = null` (free-form Arabic
 * text doesn't reliably map to a structured action); only the local
 * analyzer's heuristic conditions produce actionable suggestions.
 */
sealed interface DrawingAction {

    /**
     * Composite a warm cream rectangle behind the drawing. Useful when the
     * drawing has prominent strokes but no background — a soft cream tint
     * reads as paper and lifts the design off the dark canvas chrome.
     */
    object AddSolidBackground : DrawingAction

    /**
     * Composite a vertical rose → gold gradient behind the drawing. The
     * rose-gold range matches the rest of the Mawaai design palette and
     * gives the drawing a romantic finish.
     */
    object AddGradientBackground : DrawingAction

    /**
     * Radial vignette: leave the center untouched and ramp to ~50% black
     * at the corners. Counteracts an overly bright / washed-out
     * composition and focuses the eye on the design's center.
     */
    object DarkenEdges : DrawingAction

    /**
     * Mirror the left half of the artwork onto the right half. The
     * destructive cure for asymmetric drawings; turns any sketch into a
     * symmetric mandala-like layout in one tap.
     */
    object MirrorHorizontally : DrawingAction

    /**
     * Drape a translucent rose tint over existing strokes only (PorterDuff
     * SRC_ATOP). Used when the drawing is monochrome — adds a hint of
     * accent color without painting over empty regions.
     */
    object AddAccentColor : DrawingAction

    /**
     * Lift the brightness of every pixel by ~16% via a ColorMatrix. Used
     * when the analyzer detects an overly dark composition.
     */
    object LightenCanvas : DrawingAction

    /**
     * Phase 20: morphological dilation on the alpha channel by a small
     * structuring element (3×3) so 1-pixel-wide hand-drawn strokes thicken
     * to ~3 px. Print-readiness fix — fabric printers can lose strokes
     * thinner than ~0.3 mm at typical 12 cm garment scale, which translates
     * to ~2-3 px at the 1024-px native canvas. Implemented as a pure
     * Android `Canvas` pass so it works without OpenCV.
     */
    object ThickenThinStrokes : DrawingAction

    /**
     * Phase 20: average the left and right halves of the inked region and
     * blend the result back into both sides at 0.6 weight. Strict mirror
     * (`MirrorHorizontally`) is destructive — this softer "fix symmetry"
     * preserves intentional asymmetry while pulling drift on otherwise
     * symmetric pieces back into balance.
     */
    object FixSymmetry : DrawingAction

    /**
     * Phase 20: classify every inked pixel into one of the drawing's three
     * dominant hue buckets (Phase 20 palette analysis). Pixels whose hue is
     * far from any of the three centers get nudged toward the nearest
     * bucket center, taming a chaotic palette into a coherent one without
     * destroying the user's color choices outright.
     */
    object BalancePalette : DrawingAction
}

/**
 * One row on the Recommendations screen. [message] is the Arabic copy
 * shown to the user; [action], when non-null, surfaces an "Apply" button
 * next to the card that triggers [DrawingActionEngine.apply].
 */
data class DrawingSuggestion(
    val message: String,
    val action: DrawingAction? = null
)

/**
 * Container for a single analysis pass — either local heuristics or
 * Gemini Vision. The screen renders the suggestions verbatim; [source]
 * is preserved so a future UI tweak can label the badge differently for
 * each origin.
 */
data class DrawingAnalysis(
    val suggestions: List<DrawingSuggestion>,
    val source: Source
) {
    enum class Source { LOCAL, GEMINI }

    companion object {
        val EMPTY: DrawingAnalysis = DrawingAnalysis(emptyList(), Source.LOCAL)
    }
}
