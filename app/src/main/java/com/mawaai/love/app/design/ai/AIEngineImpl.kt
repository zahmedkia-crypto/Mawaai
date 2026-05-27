package com.mawaai.love.app.design.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.mawaai.love.app.core.opencv.OpenCVBootstrap
import com.mawaai.love.app.data.database.entities.ProjectEntity
import com.mawaai.love.app.data.repository.ProjectRepository
import com.mawaai.love.app.data.repository.TemplateRepository
import com.mawaai.love.app.design.ai.analysis.StructuredAnalysisClient
import com.mawaai.love.app.design.ai.cloudflare.CloudflareWorkersAiClient
import com.mawaai.love.app.design.ai.gemini.GeminiVisionClient
import com.mawaai.love.app.design.ai.huggingface.HuggingFaceClient
import com.mawaai.love.app.design.ai.pipelines.CnParams
import com.mawaai.love.app.design.ai.pipelines.compositeRefinePromptFor
import com.mawaai.love.app.design.ai.pipelines.controlNetParamsFor
import com.mawaai.love.app.design.ai.pipelines.createSolidBitmap
import com.mawaai.love.app.design.ai.pipelines.downsizeIfNeeded
import com.mawaai.love.app.design.ai.pipelines.negativePromptFor
import com.mawaai.love.app.design.ai.pipelines.recycleIntermediates
import com.mawaai.love.app.design.ai.pipelines.safeRecycle
import com.mawaai.love.app.design.ai.pipelines.specializedNegativePromptFor
import com.mawaai.love.app.design.ai.pipelines.specializedPromptFor
import com.mawaai.love.app.design.ai.pipelines.stylePromptFor
import com.mawaai.love.app.design.ai.preservation.ControlledImprovementEngine
import com.mawaai.love.app.design.ai.preservation.MaterialRenderer
import com.mawaai.love.app.design.ai.preservation.SketchScanner
import com.mawaai.love.app.design.ai.preservation.SketchStructureAnalyzer
import com.mawaai.love.app.design.ai.processors.BlendMode
import com.mawaai.love.app.design.ai.processors.BlendModeProcessor
import com.mawaai.love.app.design.ai.processors.EdgeDetectionProcessor
import com.mawaai.love.app.design.ai.processors.SegmentationProcessor
import com.mawaai.love.app.design.ai.processors.StyleTransferProcessor
import com.mawaai.love.app.design.ai.processors.SuperResolutionProcessor
import com.mawaai.love.app.design.ai.quality.AiQualityReviewer
import com.mawaai.love.app.design.ai.removebg.RemoveBgClient
import com.mawaai.love.app.design.ai.render.RenderPromptBuilder
import com.mawaai.love.app.design.ai.suggestions.SuggestionsClient
import com.mawaai.love.app.design.domain.model.FabricTone
import com.mawaai.love.app.design.domain.model.SkinTone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routing model:
 *  - Converter flow uses Cloudflare Workers AI first, with Gemini prompt
 *    shaping when available.
 *  - Specialized garment flow stays local-first for preservation.
 *  - Both paths fall back to the on-device pipeline when cloud calls fail.
 *  - Every successful output goes through [OfflineEnhancer] for the final polish.
 *
 * Design contract: a cloud call returning null is **always** treated as
 * "not configured / error / fallback to local" — never as a hard error.
 * The user shouldn't see a 503 toast or know the call went out at all;
 * they should just see slightly slower / lower-quality output when the
 * network is unreachable.
 *
 * Heavy cloud / TFLite clients are injected as [dagger.Lazy] so the
 * AI graph isn't fully instantiated at app start; each provider is
 * created the first time its code path is taken.
 */
@Singleton
class AIEngineImpl @Inject constructor(
    private val segmentation: SegmentationProcessor,
    private val edges: EdgeDetectionProcessor,
    private val styleTransfer: dagger.Lazy<StyleTransferProcessor>,
    private val superResolution: dagger.Lazy<SuperResolutionProcessor>,
    private val blend: BlendModeProcessor,
    private val huggingFace: dagger.Lazy<HuggingFaceClient>,
    private val offlineEnhancer: OfflineEnhancer,
    private val autoStylePicker: AutoStylePicker,
    private val sketchScanner: SketchScanner,
    private val structureAnalyzer: SketchStructureAnalyzer,
    private val improvementEngine: ControlledImprovementEngine,
    private val materialRenderer: MaterialRenderer,
    private val visionClient: GeminiVisionClient,
    
    // Phase 25/MT-015/16/17 Intelligence Wiring
    private val analysisClient: StructuredAnalysisClient,
    private val suggestionsClient: SuggestionsClient,
    private val promptBuilder: RenderPromptBuilder,
    private val qualityReviewer: AiQualityReviewer,
    private val projectRepository: ProjectRepository,
    private val templateRepository: TemplateRepository,

    // Phase 23 — extra cloud providers wired into the DI graph.
    //
    // [removeBg]: PREMIUM emergency fallback for the segmentation step.
    // Used in `processSpecialized` only when HuggingFace RMBG returns
    // null, so the 50/month free quota stays intact during normal use.
    //
    // [cloudflare]: pure text-to-image (SDXL / FLUX / Lightning).
    // Currently NOT plumbed into `processSpecialized` / `processConverter`
    // because CF doesn't accept the user's sketch as a conditioning
    // input — using it as a converter fallback would silently lose
    // sketch fidelity. The client lives here so future UIs that DO
    // need pure text-to-image (e.g. a "describe a romantic scene"
    // generator on the romantic side) can call it directly via
    // [generateRomanticImage].
    private val removeBg: dagger.Lazy<RemoveBgClient>,
    private val cloudflare: dagger.Lazy<CloudflareWorkersAiClient>
) : AIEngine {

    @Volatile private var initialized = false
    @Volatile private var openCvOk = false
    @Volatile private var segmenterOk = false

    override val openCvAvailable: Boolean get() {
        ensureInit()
        return openCvOk
    }

    override val subjectSegmenterAvailable: Boolean get() {
        ensureInit()
        return segmenterOk
    }

    override val cloudTextToImageAvailable: Boolean get() = cloudflare.get().isConfigured

    override val cloudRefinementAvailable: Boolean get() = cloudflare.get().isConfigured

    override suspend fun analyzeProject(projectId: String): com.mawaai.love.app.design.ai.analysis.SketchAnalysis {
        val project = projectRepository.getProjectById(projectId) ?: error("Project $projectId not found")
        val template = templateRepository.getTemplateById(project.templateId) ?: error("Template ${project.templateId} not found")
        val sketch = loadBitmap(project.sketchPath)

        val analysis = analysisClient.analyze(sketch, template).getOrThrow()
        projectRepository.saveAnalysis(projectId, analysis)
        return analysis
    }

    override suspend fun generateSuggestions(projectId: String): List<com.mawaai.love.app.design.ai.suggestions.Suggestion> {
        val project = projectRepository.getProjectById(projectId) ?: error("Project $projectId not found")
        val template = templateRepository.getTemplateById(project.templateId) ?: error("Template ${project.templateId} not found")
        val sketch = loadBitmap(project.sketchPath)
        val analysis = project.analysisJson?.let { 
            com.google.gson.Gson().fromJson(it, com.mawaai.love.app.design.ai.analysis.SketchAnalysis::class.java) 
        } ?: analyzeProject(projectId)

        val suggestions = suggestionsClient.generateSuggestions(sketch, template, analysis).getOrThrow()
        val updated = project.copy(
            suggestionsJson = com.google.gson.Gson().toJson(suggestions),
            updatedAt = System.currentTimeMillis()
        )
        projectRepository.updateProject(updated)
        return suggestions
    }

    override suspend fun renderProject(
        projectId: String,
        onProgress: (ProcessingStage) -> Unit
    ): Bitmap {
        ensureInit()
        val project = projectRepository.getProjectById(projectId) ?: error("Project $projectId not found")
        val template = templateRepository.getTemplateById(project.templateId) ?: error("Template ${project.templateId} not found")
        val sketch = loadBitmap(project.sketchPath)
        
        val acceptedIds = project.acceptedSuggestionIds.split(",").filter { it.isNotBlank() }.toSet()
        val allSuggestions: List<com.mawaai.love.app.design.ai.suggestions.Suggestion> = project.suggestionsJson?.let {
            val type = object : com.google.gson.reflect.TypeToken<List<com.mawaai.love.app.design.ai.suggestions.Suggestion>>() {}.type
            com.google.gson.Gson().fromJson(it, type)
        } ?: emptyList()
        val acceptedSuggestions = allSuggestions.filter { it.id in acceptedIds }

        onProgress(ProcessingStage.Init)
        val renderPrompt = promptBuilder.build(template, project.colorOverride, acceptedSuggestions)
        
        onProgress(ProcessingStage.Stylizing)
        // For Phase 5, we use Cloudflare img2img for the heavy lifting of "rendering" the sketch 
        // into a clean design based on template intelligence.
        val cf = cloudflare.get()
        val rendered = if (cf.isConfigured) {
             cf.imageToImage(
                input = sketch,
                prompt = renderPrompt.toPromptString(),
                strength = 0.45 // Higher strength than composite refinement to allow artistic interpretation
            ) ?: throw IllegalStateException("Cloudflare render failed")
        } else {
            // Fallback to specialized local pipeline if cloud is down
            processSpecialized(sketch, template.categoryId, null, "auto", null, null, onProgress)
        }

        onProgress(ProcessingStage.FinalPolish)
        val polished = offlineEnhancer.enhance(rendered, template.categoryId)

        // Save result
        val path = saveBitmap(polished, "render_$projectId.png")
        val finalProject = project.copy(
            renderedPath = path,
            renderPrompt = renderPrompt.toPromptString(),
            renderedAt = System.currentTimeMillis(),
            status = "RENDERED",
            updatedAt = System.currentTimeMillis()
        )
        projectRepository.updateProject(finalProject)
        
        onProgress(ProcessingStage.Done)
        return polished
    }

    override suspend fun updateProjectColor(projectId: String, colorHex: String?) {
        val project = projectRepository.getProjectById(projectId) ?: return
        val updated = project.copy(
            colorOverride = colorHex,
            updatedAt = System.currentTimeMillis()
        )
        projectRepository.updateProject(updated)
    }

    private fun loadBitmap(path: String): Bitmap {
        return BitmapFactory.decodeFile(path) ?: error("Failed to load bitmap at $path")
    }

    private fun saveBitmap(bitmap: Bitmap, fileName: String): String {
        val file = java.io.File(appContext.cacheDir, fileName)
        java.io.FileOutputStream(file).use { 
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return file.absolutePath
    }

    override fun isReady(): Boolean {
        ensureInit()
        return openCvOk && segmenterOk
    }

    override suspend fun generateRomanticImage(prompt: String): Bitmap? {
        val cf = cloudflare.get()
        if (!cf.isConfigured) return null
        // SDXL Lightning is the default — fastest CF model that still
        // produces gallery-quality output. Callers that want maximum
        // fidelity over speed can build on this method or call the
        // [CloudflareWorkersAiClient] directly.
        return tryOrNull("CF generateRomanticImage failed") {
            cf.generateImage(
                prompt = prompt,
                model = CloudflareWorkersAiClient.Model.SDXL_LIGHTNING
            )
        }
    }

    override suspend fun refineComposite(
        composite: Bitmap,
        categoryId: String,
        subTypeId: String?
    ): Bitmap? {
        val cf = cloudflare.get()
        if (!cf.isConfigured) return null

        val prompt = compositeRefinePromptFor(categoryId, subTypeId)
        Log.i(
            TAG,
            "Refining composite category=$categoryId sub=$subTypeId via CF img2img"
        )
        return tryOrNull("CF imageToImage refine failed") {
            cf.imageToImage(
                input = composite,
                prompt = prompt
            )
        }
    }

    @Synchronized
    private fun ensureInit() {
        if (initialized) return
        // OpenCV is now loaded once, eagerly, from MawaaiApp.onCreate via
        // OpenCVBootstrap. We just read the cached availability flag so the
        // public openCvAvailable getter stays accurate.
        openCvOk = OpenCVBootstrap.ensureLoaded()

        segmenterOk = runCatching {
            val options = SubjectSegmenterOptions.Builder()
                .enableForegroundConfidenceMask()
                .enableForegroundBitmap()
                .build()
            SubjectSegmentation.getClient(options)
        }.onFailure { Log.e(TAG, "Subject segmenter init failed", it) }.isSuccess
        Log.i(TAG, "SubjectSegmentation client ready = $segmenterOk")

        initialized = true
    }

    override suspend fun processSpecialized(
        input: Bitmap,
        categoryId: String,
        subTypeId: String?,
        styleId: String?,
        skinTone: SkinTone?,
        fabricTone: FabricTone?,
        onProgress: (ProcessingStage) -> Unit
    ): Bitmap {
        ensureInit()
        onProgress(ProcessingStage.Scanning)
        val scanned = sketchScanner.scan(input, MAX_INPUT_DIMENSION)

        onProgress(ProcessingStage.Understanding)
        val analysis = structureAnalyzer.analyze(scanned)

        onProgress(ProcessingStage.Improving)
        val improved = improvementEngine.improve(scanned, analysis)

        onProgress(ProcessingStage.RenderingMaterial)
        val rendered = materialRenderer.render(improved, categoryId)
        val preservedOutput = tryOrDefault("OfflineEnhancer skipped", rendered.bitmap) {
            offlineEnhancer.enhance(rendered.bitmap, categoryId)
        }

        recycleIntermediates(
            candidates = listOf(
                scanned.cleanPng,
                scanned.inkMask,
                scanned.contourMask,
                improved.bitmap,
                rendered.bitmap
            ),
            keep = listOf(input, preservedOutput)
        )

        onProgress(ProcessingStage.Done)
        return preservedOutput
    }

    override suspend fun processConverter(
        input: Bitmap,
        styleId: String?,
        onProgress: (ProcessingStage) -> Unit
    ): Bitmap {
        ensureInit()
        onProgress(ProcessingStage.Init)
        val downsized = downsizeIfNeeded(input, MAX_INPUT_DIMENSION)

        // Cloud-first converter: Cloudflare img2img keeps the sketch
        // composition while Gemini helps shape the prompt. If that path
        // fails, the existing on-device branch still returns something.
        val cloud = runConverterCloud(downsized, styleId, onProgress)
        if (cloud != null) {
            val polished = tryOrDefault("OfflineEnhancer skipped", cloud) {
                offlineEnhancer.enhance(cloud)
            }
            recycleIntermediates(
                candidates = listOf(downsized, cloud),
                keep = listOf(input, polished)
            )
            onProgress(ProcessingStage.Done)
            return polished
        }

        // ----- on-device fallback (unchanged from Phase 4) ---------------
        onProgress(ProcessingStage.Segmenting)
        val foreground = safeSegment(downsized) ?: downsized

        onProgress(ProcessingStage.Stylizing)
        val stylized = runCatching { styleTransfer.get().stylize(foreground, styleId ?: "auto") }
            .onFailure { Log.w(TAG, "Style transfer skipped: ${it.message}") }
            .getOrElse {
                if (openCvOk) runCatching { edges.cannyEdges(foreground) }.getOrDefault(foreground)
                else foreground
            }

        onProgress(ProcessingStage.Upscaling)
        val upscaled = tryOrDefaultBrief("Upscale skipped", stylized) {
            superResolution.get().upscale(stylized)
        }

        val polished = tryOrDefault("OfflineEnhancer skipped", upscaled) {
            offlineEnhancer.enhance(upscaled)
        }

        recycleIntermediates(
            candidates = listOf(downsized, foreground, stylized, upscaled),
            keep = listOf(input, polished)
        )

        onProgress(ProcessingStage.Done)
        return polished
    }

    /**
     * Cloud-first converter pipeline. Returns null on any failure so the
     * caller can fall back to the on-device path. Reports
     * [ProcessingStage.EdgeDetecting] + [ProcessingStage.Stylizing]
     * progress so the existing UI hooks keep working.
     *
     * Gemini shapes the prompt when available; Cloudflare Workers AI
     * does the actual image-to-image render.
     */
    private suspend fun runConverterCloud(
        downsized: Bitmap,
        styleId: String?,
        onProgress: (ProcessingStage) -> Unit
    ): Bitmap? {
        val resolvedStyle = resolveStyle(styleId, downsized)

        onProgress(ProcessingStage.EdgeDetecting)
        onProgress(ProcessingStage.Stylizing)

        val tailored = if (visionClient.isConfigured) {
            tryOrNull("Tailored prompt fetch threw") {
                visionClient.tailoredControlNetPrompt(downsized, resolvedStyle)
            }
        } else null
        val prompt = tailored ?: stylePromptFor(resolvedStyle)
        val negativePrompt = negativePromptFor(resolvedStyle)

        val rendered = runConverterCloudflare(
            sketch = downsized,
            prompt = prompt,
            negativePrompt = negativePrompt
        ) ?: return null

        return rendered
    }

    /**
     * Cloudflare Workers AI img2img render used by the converter path.
     * The input bitmap is the user's sketch; a low-strength img2img pass
     * keeps the layout while turning it into a finished render.
     */
    private suspend fun runConverterCloudflare(
        sketch: Bitmap,
        prompt: String,
        negativePrompt: String
    ): Bitmap? {
        val cf = cloudflare.get()
        if (!cf.isConfigured) return null
        return tryOrNull("Cloudflare img2img converter failed") {
            cf.imageToImage(
                input = sketch,
                prompt = prompt,
                negativePrompt = negativePrompt,
                strength = 0.32
            )
        }
    }

    /**
     * Phase-16 grade-and-retry wrapper around the ControlNet call.
     *
     * 1. Render once with [baseParams].
     * 2. If Gemini Vision is configured, ask it to grade the output
     *    1-5 against the original sketch. Log the grade.
     * 3. If the grade is ≤ [GRADE_RETRY_THRESHOLD], render ONE more
     *    time with stronger params:
     *    - `steps × RETRY_STEPS_FACTOR` (capped at [RETRY_STEPS_MAX])
     *    - `guidance + RETRY_GUIDANCE_BUMP`
     *    Use the retry output regardless of its own grade — a second
     *    retry would compound latency without diminishing-returns
     *    upside.
     * 4. When Vision is not configured, skip grading entirely — the
     *    pre-Phase-16 single-render behaviour is preserved.
     *
     * Returns the chosen bitmap (initial or retry) or null on render
     * failure. The non-chosen bitmap (when a retry happens) is recycled
     * here so the caller doesn't have to track it.
     */
    private suspend fun renderWithGradeRetry(
        edges: Bitmap,
        sketch: Bitmap,
        prompt: String,
        negativePrompt: String,
        baseParams: CnParams,
        resolvedStyle: String
    ): Bitmap? {
        val hf = huggingFace.get()
        Log.i(
            TAG,
            "ControlNet (attempt 1) style=$resolvedStyle steps=${baseParams.steps} g=${baseParams.guidance}"
        )
        val first = tryOrNull("ControlNet attempt 1 failed") {
            hf.controlNetFromSketch(
                edges = edges,
                prompt = prompt,
                negativePrompt = negativePrompt,
                inferenceSteps = baseParams.steps,
                guidanceScale = baseParams.guidance
            )
        } ?: return null

        if (!visionClient.isConfigured) return first

        val grade = tryOrNull("Vision gradeOutput threw") {
            visionClient.gradeOutput(sketch, first, resolvedStyle)
        }
        Log.i(TAG, "Vision grade attempt 1 = $grade")
        if (grade == null || grade > GRADE_RETRY_THRESHOLD) return first

        // Low-grade path: retry once with stronger sampling. The cache
        // key in `HuggingFaceClient` includes steps + guidance so this
        // is guaranteed to miss the cache and produce a fresh render.
        val retrySteps = (baseParams.steps * RETRY_STEPS_FACTOR).toInt().coerceAtMost(RETRY_STEPS_MAX)
        val retryGuidance = baseParams.guidance + RETRY_GUIDANCE_BUMP
        Log.i(
            TAG,
            "ControlNet (attempt 2 retry, grade=$grade was low) style=$resolvedStyle " +
                "steps=$retrySteps g=$retryGuidance"
        )
        val second = tryOrNull("ControlNet attempt 2 failed") {
            hf.controlNetFromSketch(
                edges = edges,
                prompt = prompt,
                negativePrompt = negativePrompt,
                inferenceSteps = retrySteps,
                guidanceScale = retryGuidance
            )
        }
        if (second == null) return first
        // We're keeping the retry; recycle the first attempt so we
        // don't leak the first bitmap.
        first.safeRecycle()
        return second
    }

    /**
     * Resolves the user-supplied [styleId] to a concrete catalog style.
     *
     * - Named styles (`vector_clean` / `artistic` / `minimalist` /
     *   `realistic`) pass straight through.
     * - `"auto"` and null route through a two-tier picker:
     *   - **Cloud-first**: when Gemini Vision is configured, ask the
     *     model to classify the sketch semantically. Vision understands
     *     "this is a flower" / "this is a portrait" — the local picker
     *     can't.
     *   - **Local fallback**: `AutoStylePicker.pick(sketch)` runs when
     *     Vision is unavailable or returns null. ~30 ms,
     *     deterministic.
     * - If both return "auto" (degenerate sketch), the AIEngine keeps
     *   the safe generic prompt — never fabricates a concrete style.
     */
    private suspend fun resolveStyle(styleId: String?, sketch: Bitmap): String {
        val normalized = styleId?.takeIf { it.isNotBlank() } ?: AutoStylePicker.AUTO
        if (normalized != AutoStylePicker.AUTO) return normalized

        if (visionClient.isConfigured) {
            val cloud = tryOrNull("Vision classifyStyle threw") { visionClient.classifyStyle(sketch) }
            if (cloud != null) {
                Log.i(TAG, "AutoStyle picked by Vision = $cloud")
                return cloud
            }
        }

        val local = autoStylePicker.pick(sketch)
        Log.i(TAG, "AutoStyle picked by local heuristic = $local")
        return local
    }

    /**
     * Specialized-flow cloud renderer. Same shape as [runConverterCloud]
     * but uses [specializedPromptFor] (category + subType + style) and
     * returns only the design pattern — the surrounding pipeline is
     * still responsible for tone application, super-resolution, polish,
     * and the per-template warp + blend.
     *
     * Returns null when the cloud path is unavailable so the caller
     * falls back to the existing on-device TFLite style-transfer path.
     */
    private suspend fun runSpecializedCloud(
        edges: Bitmap,
        categoryId: String,
        subTypeId: String?,
        styleId: String?
    ): Bitmap? {
        val hf = huggingFace.get()
        if (!hf.isConfigured) return null

        val prompt = specializedPromptFor(categoryId, subTypeId, styleId)
        val negativePrompt = specializedNegativePromptFor(categoryId)
        // Reuse the converter's per-style sampling parameters — the
        // material descriptors in [specializedPromptFor] benefit from
        // the same "more steps for realistic" trade-off.
        val params = controlNetParamsFor(styleId)

        Log.i(
            TAG,
            "Specialized ControlNet category=$categoryId sub=$subTypeId style=$styleId " +
                "steps=${params.steps} g=${params.guidance}"
        )
        return tryOrNull("Specialized ControlNet failed") {
            hf.controlNetFromSketch(
                edges = edges,
                prompt = prompt,
                negativePrompt = negativePrompt,
                inferenceSteps = params.steps,
                guidanceScale = params.guidance
            )
        }
    }

    private suspend fun safeSegment(input: Bitmap): Bitmap? =
        if (segmenterOk) tryOrNull("Segmentation failed") { segmentation.extractForeground(input) }
        else null

    private suspend fun applyTone(
        bitmap: Bitmap,
        categoryId: String,
        skinTone: SkinTone?,
        fabricTone: FabricTone?,
        mask: Bitmap?
    ): Bitmap {
        val argb = when (categoryId) {
            "henna" -> skinTone?.argb
            "abaya", "thob_sudani" -> fabricTone?.argb
            else -> null
        } ?: return bitmap
        if (!openCvOk) return bitmap
        val solid = createSolidBitmap(bitmap.width, bitmap.height, argb)
        return tryOrDefault("Tone blend skipped", bitmap) {
            blend.blend(bitmap, solid, BlendMode.MULTIPLY, overlayAlpha = 0.35, mask = mask)
        }.also { if (solid !== it) solid.safeRecycle() }
    }

    /**
     * Run [block], swallow any exception, log a warning with the full
     * stack trace, and return [fallback]. Used pervasively across the
     * AI pipeline where a single processor failure should NOT abort the
     * whole render — every stage either succeeds or is skipped, and the
     * pipeline composes downstream stages on whatever bitmap survived.
     *
     * `inline` lets the lambda call suspend functions when the helper is
     * invoked from a suspend context (the entire pipeline is suspend).
     * Eager [fallback] is fine for our call sites: every fallback is an
     * already-computed bitmap from the previous stage, never a heavy
     * recomputation.
     */
    private inline fun <T> tryOrDefault(message: String, fallback: T, block: () -> T): T =
        runCatching(block).onFailure { Log.w(TAG, message, it) }.getOrDefault(fallback)

    /**
     * Sibling of [tryOrDefault] for the cloud-call paths that report
     * "no result" via null. The AIEngine treats null as "fall back to
     * the on-device path" — see the class docstring.
     */
    private inline fun <T : Any> tryOrNull(message: String, block: () -> T?): T? =
        runCatching(block).onFailure { Log.w(TAG, message, it) }.getOrNull()

    /**
     * Variant of [tryOrDefault] that logs `"<message>: <throwable.message>"`
     * without the full stack trace. Used for high-frequency "expected
     * skip" paths (TFLite-not-loaded → style transfer / upscale) where
     * the full stack would spam logcat on every render — the short
     * message is enough to diagnose and the throwable kind is implied
     * by the call site.
     */
    private inline fun <T> tryOrDefaultBrief(message: String, fallback: T, block: () -> T): T =
        runCatching(block)
            .onFailure { Log.w(TAG, "$message: ${it.message}") }
            .getOrDefault(fallback)

    companion object {
        private const val TAG = "AIEngine"
        private const val MAX_INPUT_DIMENSION = 1024

        // Phase 16 — grade-and-retry tuning.
        // GRADE_RETRY_THRESHOLD == 1: retry only on the model's lowest
        // rating (severe artefacts / unrecognisable output). Raising to
        // 2 would retry on the bottom 40% of renders and roughly double
        // the average latency. Keep conservative.
        // RETRY_STEPS_FACTOR == 1.33: ~33% more diffusion steps on the
        // retry pass. Combined with the guidance bump, this gives the
        // sampler more headroom to lock onto details it missed.
        // RETRY_STEPS_MAX == 60: hard cap so the realistic preset
        // (40 steps default → 53 on retry) doesn't accidentally jump
        // to a value the HF free tier rejects.
        // RETRY_GUIDANCE_BUMP == 1.5: pushes the sampler harder toward
        // the prompt. Empirically lifts identifiable subjects out of
        // the abstract-noise failure mode.
        private const val GRADE_RETRY_THRESHOLD = 1
        private const val RETRY_STEPS_FACTOR = 1.33
        private const val RETRY_STEPS_MAX = 60
        private const val RETRY_GUIDANCE_BUMP = 1.5
    }
}
