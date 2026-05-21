package com.mawaai.love.app.design.ai.huggingface

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.mawaai.love.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud AI client that wraps the HuggingFace Inference API for two
 * concrete use-cases:
 *
 *  - [removeBackground]: cuts the subject out of an image using
 *    `briaai/RMBG-1.4`. Higher quality than ML Kit Subject Segmentation
 *    on cluttered backgrounds and works on photos that aren't single-
 *    subject portraits. Returns a PNG with the alpha channel encoded.
 *  - [controlNetFromSketch]: takes a Canny-edge or sketch image plus a
 *    short Arabic prompt, returns a stylized rendered image via
 *    `lllyasviel/sd-controlnet-canny`. Used by the converter flow when
 *    the user wants a "real" cloud-AI generation instead of just style
 *    transfer.
 *
 * Both methods:
 *  - Return null on any failure (no API key, network error, decode
 *    error). The AIEngine treats null as "fall back to on-device".
 *  - Cache successful results on disk under `cacheDir/hf_cache/`
 *    keyed by `(modelId, sha256(input bytes))`. Repeat calls with the
 *    same input return the cached PNG without touching the network —
 *    saves quota and gives instant feedback for slider drags / re-taps.
 *  - Handle HuggingFace's cold-start 503 by parsing the `estimated_time`
 *    field, sleeping for that duration (capped at
 *    [MAX_COLD_START_SLEEP_MS]), and retrying once.
 *
 * The cache is intentionally simple: file-system entries under
 * `hf_cache/<sha>-<modelTail>.png`. Android may evict cacheDir under
 * disk pressure; we don't manage eviction ourselves. Repeat reads use
 * `File.readBytes()` + `BitmapFactory.decodeByteArray` rather than
 * `decodeFile` so the JNI path matches the network path (both decode
 * from a byte array).
 */
@Singleton
class HuggingFaceClient @Inject constructor(
    private val api: HuggingFaceApi,
    private val gson: Gson,
    @ApplicationContext private val appContext: Context
) {

    val isConfigured: Boolean get() = BuildConfig.HUGGINGFACE_API_KEY.isNotBlank()

    /**
     * Removes the background from [input] using `briaai/RMBG-1.4`.
     * Returns a new ARGB bitmap with the alpha channel encoded by the
     * model — ready to drop into [com.mawaai.love.app.design.ai.AIEngine]
     * as a foreground bitmap. Returns null when the cloud path is
     * unavailable.
     *
     * Input is downsized to [MAX_UPLOAD_DIMENSION] before upload
     * (RMBG-1.4 is trained on 1024 px max; sending bigger wastes
     * bandwidth without quality gain).
     */
    suspend fun removeBackground(input: Bitmap): Bitmap? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        val resized = resizeForUpload(input)
        val jpegBytes = compressToJpeg(resized)
        if (resized !== input) resized.recycle()

        val cacheKey = sha256(jpegBytes)
        cacheLookup(MODEL_RMBG, cacheKey)?.let { return@withContext it }

        val responseBytes = inferOctetStream(MODEL_RMBG, jpegBytes) ?: return@withContext null
        val bitmap = BitmapFactory.decodeByteArray(responseBytes, 0, responseBytes.size)
            ?: return@withContext null
        cacheStore(MODEL_RMBG, cacheKey, responseBytes)
        bitmap
    }

    /**
     * Phase 22: cloud upscaler. Sends [input] to a Real-ESRGAN endpoint
     * on HuggingFace and returns the 4×-upscaled PNG. Used as the FINAL
     * pass before saving / sharing the result image — mid-pipeline
     * intermediates use the on-device TFLite ESRGAN
     * (`assets/models/esrgan.tflite`) for latency.
     *
     * Returns null when:
     *  - No HuggingFace key configured.
     *  - Network failure / model 503 / decode failure.
     *  - The result would be larger than [MAX_UPLOAD_DIMENSION] × 4
     *    (8 MP), which the HF free tier rejects.
     *
     * The caller falls back to the existing TFLite ESRGAN result on
     * any null return — the user gets slightly lower quality but no
     * pipeline failure.
     *
     * Cache: same `(model, sha256(input))` scheme as RMBG. Repeat
     * upscales of the same intermediate hit the cache without going
     * out to the network.
     */
    suspend fun cloudUpscale(input: Bitmap): Bitmap? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        // Real-ESRGAN scales 4×; reject inputs that would exceed
        // free-tier dimensional limits to avoid a 413 round-trip.
        if (maxOf(input.width, input.height) > MAX_UPSCALE_INPUT_DIMENSION) {
            Log.i(TAG, "Skipping cloud upscale; input ${input.width}×${input.height} exceeds free-tier cap")
            return@withContext null
        }
        val pngBytes = compressToPng(input)
        val cacheKey = sha256(pngBytes)
        cacheLookup(MODEL_REAL_ESRGAN, cacheKey)?.let { return@withContext it }

        val responseBytes = inferOctetStream(MODEL_REAL_ESRGAN, pngBytes) ?: return@withContext null
        val bitmap = BitmapFactory.decodeByteArray(responseBytes, 0, responseBytes.size)
            ?: return@withContext null
        cacheStore(MODEL_REAL_ESRGAN, cacheKey, responseBytes)
        bitmap
    }

    /**
     * Generates a stylized image conditioned on [edges] (a Canny-edge
     * pass over the user's sketch is the typical input) and the Arabic
     * [prompt]. Returns null when the cloud path is unavailable. The
     * caller is responsible for the edge-extraction step — this method
     * does not run Canny itself.
     *
     * The optional [negativePrompt] / [inferenceSteps] / [guidanceScale]
     * overrides let the AIEngine tune per-style: realistic renders use
     * more steps + higher guidance for crisp detail, artistic ones use
     * lower guidance to preserve painterliness. Defaults match the
     * pre-Phase-10 behaviour so existing callers are unaffected.
     */
    suspend fun controlNetFromSketch(
        edges: Bitmap,
        prompt: String,
        negativePrompt: String = NEGATIVE_PROMPT,
        inferenceSteps: Int = CONTROLNET_STEPS,
        guidanceScale: Double = CONTROLNET_GUIDANCE
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        val resized = resizeForUpload(edges)
        val pngBytes = compressToPng(resized)
        if (resized !== edges) resized.recycle()

        // Cache key includes the tuning parameters — same sketch with a
        // different style/steps/guidance must NOT collide on disk.
        val cacheKey = sha256(
            pngBytes +
                prompt.toByteArray() +
                negativePrompt.toByteArray() +
                "s${inferenceSteps}g${guidanceScale}".toByteArray()
        )
        cacheLookup(MODEL_CONTROLNET, cacheKey)?.let { return@withContext it }

        val body = HuggingFaceJsonRequest(
            inputs = prompt,
            parameters = HuggingFaceJsonParameters(
                image = Base64.encodeToString(pngBytes, Base64.NO_WRAP),
                negativePrompt = negativePrompt,
                numInferenceSteps = inferenceSteps,
                guidanceScale = guidanceScale
            )
        )

        val responseBytes = inferJsonWithRetry(MODEL_CONTROLNET, body) ?: return@withContext null
        val bitmap = BitmapFactory.decodeByteArray(responseBytes, 0, responseBytes.size)
            ?: return@withContext null
        cacheStore(MODEL_CONTROLNET, cacheKey, responseBytes)
        bitmap
    }

    private suspend fun inferOctetStream(model: String, bytes: ByteArray): ByteArray? {
        val auth = "Bearer ${BuildConfig.HUGGINGFACE_API_KEY}"
        return retryingInfer(label = "$model image") {
            api.inferImage(model = model, authorization = auth, body = bytes.toRequestBody(OCTET_STREAM))
        }
    }

    private suspend fun inferJsonWithRetry(model: String, body: HuggingFaceJsonRequest): ByteArray? {
        val auth = "Bearer ${BuildConfig.HUGGINGFACE_API_KEY}"
        return retryingInfer(label = "$model json") {
            api.inferJson(model = model, authorization = auth, body = body)
        }
    }

    /**
     * Shared retry loop used by both inference paths. On each attempt:
     *  1. Run [call] (the actual Retrofit method). A thrown exception
     *     ends the loop with null.
     *  2. Pass the response through [handleResponse]:
     *     - Success → return the bytes immediately.
     *     - Retry  → sleep for the model's estimated cold-start time
     *       (clamped to [MIN_COLD_START_SLEEP_MS]..[MAX_COLD_START_SLEEP_MS])
     *       and try again.
     *     - Failure → return null.
     *  3. Give up after [MAX_ATTEMPTS] iterations and return null.
     *
     * Pre-Phase-14 this body was inlined as `inferOctetStream` and
     * `inferJsonWithRetry`; the two methods only differed in the actual
     * Retrofit call, so we now thread that as a lambda. [label] is
     * mixed into the log lines so logcat is still diagnostic.
     */
    private suspend fun retryingInfer(
        label: String,
        call: suspend () -> Response<okhttp3.ResponseBody>
    ): ByteArray? {
        repeat(MAX_ATTEMPTS) { attempt ->
            val response = try {
                call()
            } catch (e: Throwable) {
                Log.w(TAG, "HF $label threw on attempt ${attempt + 1}", e)
                return null
            }
            when (val outcome = handleResponse(response)) {
                is Outcome.Success -> return outcome.bytes
                is Outcome.Retry -> {
                    val sleepMs = (outcome.estimatedSeconds * 1000.0)
                        .toLong()
                        .coerceIn(MIN_COLD_START_SLEEP_MS, MAX_COLD_START_SLEEP_MS)
                    Log.i(TAG, "HF $label warming; sleeping ${sleepMs}ms before retry")
                    delay(sleepMs)
                }
                Outcome.Failure -> return null
            }
        }
        return null
    }

    private fun handleResponse(response: Response<okhttp3.ResponseBody>): Outcome {
        if (response.isSuccessful) {
            val bytes = response.body()?.bytes() ?: return Outcome.Failure
            return Outcome.Success(bytes)
        }
        if (response.code() == 503) {
            // Model is loading. Parse estimated_time from the JSON body
            // and surface a Retry outcome so the caller can sleep.
            val errorJson = response.errorBody()?.string()
            val payload = errorJson?.let {
                runCatching { gson.fromJson(it, HuggingFaceErrorPayload::class.java) }
                    .getOrNull()
            }
            val seconds = payload?.estimatedTime ?: DEFAULT_COLD_START_SECONDS
            return Outcome.Retry(seconds)
        }
        Log.w(TAG, "HF call failed with ${response.code()}: ${response.errorBody()?.string()}")
        return Outcome.Failure
    }

    private sealed interface Outcome {
        data class Success(val bytes: ByteArray) : Outcome
        data class Retry(val estimatedSeconds: Double) : Outcome
        object Failure : Outcome
    }

    // ------- cache + image helpers -----------------------------------------

    private fun cacheLookup(model: String, key: String): Bitmap? {
        val file = cacheFile(model, key)
        if (!file.exists() || file.length() == 0L) return null
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun cacheStore(model: String, key: String, bytes: ByteArray) {
        runCatching {
            val file = cacheFile(model, key)
            FileOutputStream(file).use { it.write(bytes) }
        }.onFailure { Log.w(TAG, "Failed to write HF cache entry", it) }
    }

    private fun cacheFile(model: String, key: String): File {
        val dir = File(appContext.cacheDir, "hf_cache").apply { mkdirs() }
        // Use the trailing path segment of the model id as the filename
        // prefix so cached files sort + skim cleanly when inspecting the
        // directory ("rmbg-<sha>.png" / "controlnet-<sha>.png").
        val tail = model.substringAfterLast('/').lowercase()
        return File(dir, "$tail-$key.png")
    }

    private fun resizeForUpload(bitmap: Bitmap): Bitmap {
        val max = maxOf(bitmap.width, bitmap.height)
        if (max <= MAX_UPLOAD_DIMENSION) return bitmap
        val scale = MAX_UPLOAD_DIMENSION.toFloat() / max
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private fun compressToJpeg(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        return stream.toByteArray()
    }

    private fun compressToPng(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    internal companion object {
        const val TAG = "HuggingFaceClient"

        // Model IDs. Update here if the upstream models change paths.
        const val MODEL_RMBG = "briaai/RMBG-1.4"
        const val MODEL_CONTROLNET = "lllyasviel/sd-controlnet-canny"
        // Phase 22: Real-ESRGAN endpoint. The community-maintained
        // `nateraw/real-esrgan` HF Space wraps the official xinntao
        // weights and exposes them through the standard Inference API
        // octet-stream contract. If this model 404s in production,
        // swap to `qualcomm/Real-ESRGAN-x4plus` or to
        // `stabilityai/stable-diffusion-x4-upscaler` (the latter
        // requires a JSON inference shape — see existing
        // `controlNetFromSketch` for the pattern).
        const val MODEL_REAL_ESRGAN = "nateraw/real-esrgan"

        // 768 px matches what most diffusion models prefer; RMBG accepts
        // larger inputs but downsampling gives identical-quality cuts at
        // a fraction of the upload time.
        const val MAX_UPLOAD_DIMENSION = 768
        const val JPEG_QUALITY = 90

        // Real-ESRGAN scales 4×; an input dimension cap of 1024 px
        // produces a 4096 px (16 MP) output before disk encode, which
        // the free tier accepts but anything larger gets 413'd. Mid-
        // pipeline images already pass through `MAX_INPUT_DIMENSION`
        // (1024) in `AIEngine.processSpecialized`, so this cap is
        // effectively a belt-and-suspenders guard.
        const val MAX_UPSCALE_INPUT_DIMENSION = 1024

        // Retry policy: at most 2 attempts (initial + 1 retry on cold
        // start). Cold start sleep clamped to [3s, 30s] so a misbehaving
        // server can't pin us indefinitely.
        const val MAX_ATTEMPTS = 2
        const val MIN_COLD_START_SLEEP_MS: Long = 3_000L
        const val MAX_COLD_START_SLEEP_MS: Long = 30_000L
        const val DEFAULT_COLD_START_SECONDS: Double = 8.0

        // ControlNet generation defaults. 30 steps is the sweet spot
        // between quality and time on the free tier; guidance 7.5 is
        // standard for SD-1.5 derivatives.
        const val CONTROLNET_STEPS: Int = 30
        const val CONTROLNET_GUIDANCE: Double = 7.5

        // Default negative prompt for ControlNet — bans the most common
        // failure modes (extra limbs, watermarks, low quality) so the
        // renderer doesn't have to relearn them per prompt.
        const val NEGATIVE_PROMPT: String =
            "blurry, low quality, watermark, extra limbs, deformed, ugly, distorted"

        val OCTET_STREAM = "application/octet-stream".toMediaTypeOrNull()!!
    }
}
