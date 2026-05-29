package com.mawaai.love.app.design.ai

import android.graphics.Bitmap
import com.mawaai.love.app.design.domain.model.FabricTone
import com.mawaai.love.app.design.domain.model.SkinTone

import com.mawaai.love.app.design.ai.analysis.SketchAnalysis
import com.mawaai.love.app.design.ai.suggestions.Suggestion

interface AIEngine {
    fun isReady(): Boolean
    val openCvAvailable: Boolean
    val subjectSegmenterAvailable: Boolean

    /**
     * True when a cloud text-to-image provider (e.g. Cloudflare) is configured.
     */
    val cloudTextToImageAvailable: Boolean

    /**
     * Phase 3: Analyzes the sketch in a project using vision models.
     * Persistence is handled internally via [ProjectRepository].
     */
    suspend fun analyzeProject(projectId: String): SketchAnalysis

    /**
     * Phase 4: Generates suggestions based on analysis and template.
     */
    suspend fun generateSuggestions(projectId: String): List<Suggestion>

    /**
     * Phase 5: Renders the final high-quality design using the full
     * Creative Studio intelligence pipeline.
     */
    suspend fun renderProject(
        projectId: String,
        onProgress: (ProcessingStage) -> Unit
    ): Bitmap

    /**
     * Phase 7: Applies color override or traditional palette refinement.
     */
    suspend fun updateProjectColor(projectId: String, colorHex: String?)

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
