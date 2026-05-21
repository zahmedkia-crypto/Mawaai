package com.mawaai.love.app.design.ai

import android.graphics.Bitmap
import com.mawaai.love.app.design.domain.model.FabricTone
import com.mawaai.love.app.design.domain.model.SkinTone

interface AIEngine {
    fun isReady(): Boolean
    val openCvAvailable: Boolean
    val subjectSegmenterAvailable: Boolean

    /**
     * True when at least one cloud text-to-image provider is configured.
     * UI surfaces (e.g. a "generate from description" entry point) can
     * gate themselves on this flag so they hide cleanly when the user
     * hasn't supplied any cloud keys.
     */
    val cloudTextToImageAvailable: Boolean

    suspend fun processSpecialized(
        input: Bitmap,
        categoryId: String,
        subTypeId: String?,
        styleId: String?,
        skinTone: SkinTone?,
        fabricTone: FabricTone?,
        onProgress: (ProcessingStage) -> Unit
    ): Bitmap

    suspend fun processConverter(
        input: Bitmap,
        styleId: String?,
        onProgress: (ProcessingStage) -> Unit
    ): Bitmap

    /**
     * Pure text-to-image generation. Used by romantic-side features
     * that don't have a sketch — e.g. a "describe what you want" card
     * generator, custom backgrounds for love letters, etc.
     *
     * Routes through Cloudflare Workers AI (SDXL Lightning by default,
     * ~2-3s inference, free up to 10K neurons/day). Returns null when
     * no cloud T2I provider is configured or the call fails — callers
     * should fall back to a static asset / graceful empty state.
     *
     * The prompt is sent as-is. English produces the best output;
     * Arabic-only prompts work but are weaker. UI code that takes
     * Arabic input should translate via Gemini before calling.
     */
    suspend fun generateRomanticImage(prompt: String): Bitmap?

    /**
     * Phase 25 — refines a freshly composited (artwork-on-template)
     * image so the design feels naturally integrated rather than
     * pasted. The img2img call:
     *  - PRESERVES the spatial layout of the composite (low strength)
     *  - REFINES fabric folds, lighting consistency, edge integration
     *  - REMOVES hard seams visible at the warp boundary
     *
     * Returns null when the refinement provider is unavailable so the
     * caller falls back to the un-refined composite. The compose-then-
     * refine flow lives in [com.mawaai.love.app.design.presentation
     * .flow.TemplateGalleryViewModel].
     */
    suspend fun refineComposite(
        composite: Bitmap,
        categoryId: String,
        subTypeId: String?
    ): Bitmap?

    /**
     * True when a cloud refinement provider is configured. UI can gate
     * the "Smart Integration" stage on this so it hides when the user
     * has no cloud keys set.
     */
    val cloudRefinementAvailable: Boolean
}
