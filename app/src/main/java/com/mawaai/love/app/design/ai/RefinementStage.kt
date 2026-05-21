package com.mawaai.love.app.design.ai

/**
 * Phase 25 — multi-step compose-refine pipeline.
 *
 * The template-application flow is no longer a single "compose and
 * navigate" call. The user now sees four distinct stages, each one
 * doing meaningful work:
 *
 *   1. [Compositing]  — perspective warp + per-category blend
 *      (NORMAL/MULTIPLY/OVERLAY). Fast, on-device, deterministic.
 *      Produces a "draft" with the design in the right place but
 *      with hard edges and no awareness of the template's lighting.
 *
 *   2. [Refining]     — Cloudflare img2img on the draft with a
 *      template-aware prompt and low strength (~0.30). Preserves
 *      the spatial layout while making the design feel naturally
 *      part of the fabric / skin / wall: fabric folds run through
 *      the design, light from the template reaches the design's
 *      shadows, hard composite edges soften into the surface.
 *      Skipped when no cloud refinement provider is configured.
 *
 *   3. [Polishing]    — final OfflineEnhancer pass for unsharp mask
 *      + saturation lift, category-aware. Cheap, local, brings the
 *      img2img output back to gallery-print sharpness (img2img can
 *      slightly soften details).
 *
 *   4. [Done]         — final image persisted, navigation triggered.
 *
 * [Failed] is a terminal failure with the original throwable. The
 * UI surfaces a retry affordance.
 */
sealed class RefinementStage {
    object Idle : RefinementStage()
    object Compositing : RefinementStage()
    object Refining : RefinementStage()
    object Polishing : RefinementStage()
    object Done : RefinementStage()
    data class Failed(val cause: Throwable) : RefinementStage()
}
