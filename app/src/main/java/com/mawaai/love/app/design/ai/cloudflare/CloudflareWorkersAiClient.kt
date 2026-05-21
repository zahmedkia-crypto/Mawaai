package com.mawaai.love.app.design.ai.cloudflare

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.mawaai.love.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud AI client wrapping Cloudflare Workers AI for image generation.
 *
 * Why it exists alongside [com.mawaai.love.app.design.ai.huggingface.HuggingFaceClient]:
 *  - **Quota headroom**: 10,000 neurons/day on the free tier — roughly
 *    50-100 SDXL renders or 200+ SDXL-Lightning renders per day. The
 *    HF Inference free tier has no published quota and silently throttles
 *    after ~30 renders, so CF makes a great primary path for prompts
 *    that don't need ControlNet conditioning.
 *  - **Newer models**: CF hosts FLUX-1-Schnell, SDXL-Lightning, and
 *    Dreamshaper-LCM with sub-2s inference, which the HF Inference API
 *    does not. The [Model] enum surfaces the ones tested with Mawaai.
 *  - **Different failure mode**: CF rate-limits with HTTP 429 and a
 *    clear retry-after header; HF returns 503 with `estimated_time`.
 *    Each provider's outage is independent — having both lets the
 *    AIEngine fail over gracefully.
 *
 * Contract mirrors the other cloud clients:
 *  - Returns null on any failure (no key, network, decode error).
 *  - Caches PNG bytes on disk under `cacheDir/cf_cache/<sha>.png` keyed
 *    by `sha256(prompt + params)` so repeat requests are free + instant.
 *  - All I/O on `Dispatchers.IO`.
 */
@Singleton
class CloudflareWorkersAiClient @Inject constructor(
    private val api: CloudflareWorkersAiApi,
    @ApplicationContext private val appContext: Context
) {

    val isConfigured: Boolean
        get() = BuildConfig.CLOUDFLARE_API_TOKEN.isNotBlank() &&
            BuildConfig.CLOUDFLARE_ACCOUNT_ID.isNotBlank()

    /**
     * Identifiers of the Workers AI text-to-image models Mawaai uses.
     * Update the [path] string here when CF rolls a new version — every
     * call site reads from the enum so version flips are one-line.
     */
    enum class Model(val path: String) {
        // SDXL base — best quality, ~6-12s inference. Default for the
        // "premium" path when the user can afford the latency.
        SDXL_BASE("@cf/stabilityai/stable-diffusion-xl-base-1.0"),

        // SDXL Lightning — 4-step distilled SDXL, ~1.5-3s inference.
        // Slightly softer than base; use for live previews / sliders.
        SDXL_LIGHTNING("@cf/bytedance/stable-diffusion-xl-lightning"),

        // FLUX-1-Schnell — fastest CF-hosted T2I at the time of writing.
        // Excellent on photographic prompts, weaker on illustration.
        FLUX_SCHNELL("@cf/black-forest-labs/flux-1-schnell"),

        // Dreamshaper LCM — illustration-leaning, fast distilled SD-1.5
        // descendant. Good fallback when SDXL is overloaded.
        DREAMSHAPER_LCM("@cf/lykon/dreamshaper-8-lcm"),

        // Stable Diffusion 1.5 img2img — takes an input image + prompt
        // + strength and returns a refined version. Used by the
        // compose-refine flow to integrate a freshly composited design
        // into the underlying template (fabric folds, lighting, edge
        // blending) without losing the spatial layout. ~3-6s inference.
        IMG2IMG("@cf/runwayml/stable-diffusion-v1-5-img2img")
    }

    /**
     * Generates an image from [prompt] using [model]. Returns null when
     * the cloud path is unavailable so callers fall back to the existing
     * HF / on-device paths.
     *
     * Negative prompt + steps + guidance + dimensions default to values
     * that match SDXL's native expectations; smaller / faster models
     * tolerate the same defaults so the call site doesn't have to know
     * which model is in play.
     */
    suspend fun generateImage(
        prompt: String,
        model: Model = Model.SDXL_BASE,
        negativePrompt: String = DEFAULT_NEGATIVE_PROMPT,
        numSteps: Int = DEFAULT_STEPS,
        guidance: Double = DEFAULT_GUIDANCE,
        width: Int = DEFAULT_DIMENSION,
        height: Int = DEFAULT_DIMENSION,
        seed: Long? = null
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null

        val cacheKey = sha256(
            (prompt +
                negativePrompt +
                model.path +
                "s${numSteps}g${guidance}w${width}h${height}" +
                (seed?.toString() ?: "")).toByteArray()
        )
        cacheLookup(cacheKey)?.let { return@withContext it }

        val responseBytes = try {
            val response = api.generateImage(
                accountId = BuildConfig.CLOUDFLARE_ACCOUNT_ID,
                model = model.path,
                authorization = "Bearer ${BuildConfig.CLOUDFLARE_API_TOKEN}",
                body = CloudflareGenerateRequest(
                    prompt = prompt,
                    negativePrompt = negativePrompt,
                    numSteps = numSteps,
                    guidance = guidance,
                    width = width,
                    height = height,
                    seed = seed
                )
            )
            if (!response.isSuccessful) {
                // 401 = bad token, 402 = neuron quota exhausted, 429 =
                // rate limit, 5xx = server error. None should surface
                // as an exception — return null and let AIEngine fall
                // back to the HF / on-device path.
                Log.w(
                    TAG,
                    "CF ${model.path} failed: ${response.code()} ${response.errorBody()?.string()}"
                )
                return@withContext null
            }
            response.body()?.bytes() ?: return@withContext null
        } catch (e: Throwable) {
            Log.w(TAG, "CF ${model.path} threw", e)
            return@withContext null
        }

        val bitmap = BitmapFactory.decodeByteArray(responseBytes, 0, responseBytes.size)
            ?: return@withContext null
        cacheStore(cacheKey, responseBytes)
        bitmap
    }

    /**
     * Image-to-image refinement. Takes [input] + [prompt] + [strength]
     * and returns a refined bitmap. Designed for the compose-refine
     * flow: pass a simple-composited design + a template-aware prompt
     * + low strength (~0.30) to get a photorealistic, naturally-
     * integrated final image.
     *
     * Returns null on failure (no key, network, decode, or quota).
     * Callers should fall back to the un-refined composite so the
     * pipeline still produces output.
     *
     * Caching: same `(prompt + image-sha + params)` scheme as
     * [generateImage]. Repeat refines of the same composite with the
     * same prompt are free + instant.
     */
    suspend fun imageToImage(
        input: Bitmap,
        prompt: String,
        strength: Double = DEFAULT_REFINE_STRENGTH,
        negativePrompt: String = DEFAULT_NEGATIVE_PROMPT,
        numSteps: Int = DEFAULT_REFINE_STEPS,
        guidance: Double = DEFAULT_GUIDANCE
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null

        // Resize for upload — img2img on CF SD-1.5 wants 512×512
        // multiples-of-8. Higher resolutions get rejected with a 400.
        val resized = resizeForRefine(input)
        val pngBytes = compressToPng(resized)
        if (resized !== input) resized.recycle()
        val imageB64 = Base64.encodeToString(pngBytes, Base64.NO_WRAP)

        val cacheKey = sha256(
            (prompt +
                negativePrompt +
                Model.IMG2IMG.path +
                "s${strength}n${numSteps}g${guidance}").toByteArray() +
                pngBytes
        )
        cacheLookup(cacheKey)?.let { return@withContext it }

        val responseBytes = try {
            val response = api.generateImage(
                accountId = BuildConfig.CLOUDFLARE_ACCOUNT_ID,
                model = Model.IMG2IMG.path,
                authorization = "Bearer ${BuildConfig.CLOUDFLARE_API_TOKEN}",
                body = CloudflareGenerateRequest(
                    prompt = prompt,
                    negativePrompt = negativePrompt,
                    numSteps = numSteps,
                    guidance = guidance,
                    imageB64 = imageB64,
                    strength = strength
                )
            )
            if (!response.isSuccessful) {
                Log.w(
                    TAG,
                    "CF img2img failed: ${response.code()} ${response.errorBody()?.string()}"
                )
                return@withContext null
            }
            response.body()?.bytes() ?: return@withContext null
        } catch (e: Throwable) {
            Log.w(TAG, "CF img2img threw", e)
            return@withContext null
        }

        val bitmap = BitmapFactory.decodeByteArray(responseBytes, 0, responseBytes.size)
            ?: return@withContext null
        cacheStore(cacheKey, responseBytes)
        bitmap
    }

    /** Img2img is sized to 512×512 — SD-1.5's native resolution.
     *  Larger inputs get rejected with a 400. */
    private fun resizeForRefine(bitmap: Bitmap): Bitmap {
        val targetSize = REFINE_DIMENSION
        if (bitmap.width == targetSize && bitmap.height == targetSize) return bitmap
        return Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
    }

    private fun compressToPng(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun cacheLookup(key: String): Bitmap? {
        val file = cacheFile(key)
        if (!file.exists() || file.length() == 0L) return null
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun cacheStore(key: String, bytes: ByteArray) {
        runCatching {
            FileOutputStream(cacheFile(key)).use { it.write(bytes) }
        }.onFailure { Log.w(TAG, "Failed to write CF cache entry", it) }
    }

    private fun cacheFile(key: String): File {
        val dir = File(appContext.cacheDir, "cf_cache").apply { mkdirs() }
        return File(dir, "$key.png")
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    internal companion object {
        const val TAG = "CloudflareAi"

        // SDXL native resolution. CF rejects non-multiple-of-8 dims and
        // anything outside [256..2048].
        const val DEFAULT_DIMENSION = 1024

        // CF's SDXL caps num_steps at 20; 20 is the sweet spot between
        // quality and neuron cost on the free tier.
        const val DEFAULT_STEPS = 20

        // Guidance 7.5 matches SD-1.5/SDXL community defaults.
        const val DEFAULT_GUIDANCE = 7.5

        // Default negative prompt — bans the common failure modes so
        // the model doesn't have to relearn them per call.
        const val DEFAULT_NEGATIVE_PROMPT: String =
            "blurry, low quality, watermark, extra limbs, deformed, ugly, distorted, " +
                "text, signature, jpeg artifacts"

        // ── img2img defaults ──────────────────────────────────────────
        // SD-1.5 img2img native resolution. Cloudflare rejects anything
        // bigger than 512 for this endpoint.
        const val REFINE_DIMENSION = 512

        // 0.30 strength preserves the spatial layout of the input
        // composite (so the design stays IN the right place) while
        // letting the AI refine fabric folds, lighting, edges. Lower
        // values make the refinement too subtle; higher values let
        // the AI move the design around or change its colour.
        const val DEFAULT_REFINE_STRENGTH = 0.30

        // Img2img tolerates fewer steps than text-to-image because
        // the input already gives the model a strong starting point.
        const val DEFAULT_REFINE_STEPS = 12
    }
}
