package com.mawaai.love.app.design.ai.groq

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.mawaai.love.app.BuildConfig
import com.mawaai.love.app.design.ai.gateway.ProviderFatalError
import com.mawaai.love.app.design.ai.gateway.ProviderId
import com.mawaai.love.app.design.ai.gateway.ProviderRecoverableError
import com.mawaai.love.app.design.ai.gateway.VisionProvider
import com.mawaai.love.app.design.ai.gateway.TextProvider
import java.io.ByteArrayOutputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Groq Cloud vision provider. Implements [VisionProvider] for the gateway
 * fallback chain.
 *
 * Live-verified on 2026-05-25 against [Groq ListModels](https://api.groq.com/openai/v1/models):
 * - `meta-llama/llama-4-scout-17b-16e-instruct` returns multimodal chat output
 *   in ~180ms p50 for free-tier requests with a single 64×64 image.
 *
 * **Important deprecation note:** The original integration draft referenced
 * `llama-3.2-90b-vision-preview` — that model is no longer listed by Groq's
 * ListModels endpoint (verified 2026-05-25). If Groq deprecates
 * `llama-4-scout-17b-16e-instruct` later, replace the [MODEL] constant and
 * re-run the ListModels probe to pick a fresh multimodal model.
 *
 * Self-contained Retrofit instance to avoid coupling with the broader
 * NetworkModule (mirrors the OpenRouterClient pattern from MT-012). The
 * second OkHttp instance is acceptable for a fallback path.
 */
@Singleton
class GroqVisionProvider @Inject constructor() : VisionProvider {

    override val id: ProviderId = ProviderId.GROQ

    override val isConfigured: Boolean
        get() = BuildConfig.GROQ_API_KEY.isNotBlank()

    private val api: GroqApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApi::class.java)
    }

    override suspend fun visionAnalyze(prompt: String, image: Bitmap): Result<String> {
        val key = BuildConfig.GROQ_API_KEY
        if (key.isBlank()) {
            return Result.failure(
                ProviderFatalError.InvalidKey("GROQ_API_KEY missing from local.properties")
            )
        }

        return runCatching {
            // Encode bitmap on a background dispatcher — JPEG compression is CPU-bound.
            val b64 = withContext(Dispatchers.Default) {
                encodeJpegBase64(image, MAX_DIMENSION, JPEG_QUALITY)
            }

            val body = GroqChatRequest(
                model = MODEL,
                messages = listOf(
                    GroqChatRequest.Message(
                        role = "user",
                        content = listOf(
                            GroqChatRequest.Content.text(prompt),
                            GroqChatRequest.Content.imageUrl("data:image/jpeg;base64,$b64"),
                        ),
                    ),
                ),
                maxTokens = MAX_OUTPUT_TOKENS,
                temperature = 0.2f,
            )

            val response: GroqChatResponse = withContext(Dispatchers.IO) {
                api.chatCompletion(authorization = "Bearer $key", body = body)
            }

            if (response.error != null) {
                throw mapErrorBody(response.error)
            }

            response.choices?.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
                ?: throw ProviderRecoverableError.ServiceUnavailable(
                    "Groq returned empty content (no choices/text)"
                )
        }.recoverCatching { e ->
            throw translateError(e)
        }
    }

    private companion object {
        const val TAG = "GroqVisionProvider"
        const val BASE_URL = "https://api.groq.com/"
        // Live-verified 2026-05-25 against api.groq.com/openai/v1/models.
        // Llama 4 Scout is Groq's current multimodal flagship; older models
        // (llama-3.2-90b-vision-preview etc.) returned 404 / were removed.
        const val MODEL = "meta-llama/llama-4-scout-17b-16e-instruct"
        const val MAX_DIMENSION = 1024
        const val JPEG_QUALITY = 85
        const val MAX_OUTPUT_TOKENS = 1024
    }
}

/**
 * Groq Cloud text provider. Implements [TextProvider].
 *
 * Live-verified on 2026-05-25:
 * - `llama-3.3-70b-versatile` returns chat completion in ~100ms p50.
 *
 * This provider exposes the optional `systemPrompt` argument by prepending a
 * `system` role message, matching the OpenAI / Groq contract.
 */
@Singleton
class GroqTextProvider @Inject constructor() : TextProvider {

    override val id: ProviderId = ProviderId.GROQ

    override val isConfigured: Boolean
        get() = BuildConfig.GROQ_API_KEY.isNotBlank()

    private val api: GroqApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApi::class.java)
    }

    override suspend fun generateText(
        prompt: String,
        systemPrompt: String?,
    ): Result<String> {
        val key = BuildConfig.GROQ_API_KEY
        if (key.isBlank()) {
            return Result.failure(
                ProviderFatalError.InvalidKey("GROQ_API_KEY missing from local.properties")
            )
        }

        return runCatching {
            val messages = buildList {
                if (!systemPrompt.isNullOrBlank()) {
                    add(
                        GroqChatRequest.Message(
                            role = "system",
                            content = listOf(GroqChatRequest.Content.text(systemPrompt)),
                        )
                    )
                }
                add(
                    GroqChatRequest.Message(
                        role = "user",
                        content = listOf(GroqChatRequest.Content.text(prompt)),
                    )
                )
            }

            val response: GroqChatResponse = withContext(Dispatchers.IO) {
                api.chatCompletion(
                    authorization = "Bearer $key",
                    body = GroqChatRequest(
                        model = MODEL,
                        messages = messages,
                        maxTokens = MAX_OUTPUT_TOKENS,
                        temperature = 0.7f,
                    ),
                )
            }

            if (response.error != null) {
                throw mapErrorBody(response.error)
            }

            response.choices?.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
                ?: throw ProviderRecoverableError.ServiceUnavailable(
                    "Groq returned empty content (no choices/text)"
                )
        }.recoverCatching { e ->
            throw translateError(e)
        }
    }

    private companion object {
        const val TAG = "GroqTextProvider"
        // Live-verified 2026-05-25. Llama 3.3 70B Versatile is Groq's current
        // free-tier text flagship. Update via a new MT if Groq deprecates it.
        const val MODEL = "llama-3.3-70b-versatile"
        const val MAX_OUTPUT_TOKENS = 1024
    }
}

// ───────────────────────── shared helpers ─────────────────────────

/**
 * Compress [bitmap] to JPEG (downscaling to fit within [maxDimension] on its
 * longest edge), base64-encode without newlines, and return the encoded
 * String. Suitable for inlining into a data URL.
 */
private fun encodeJpegBase64(bitmap: Bitmap, maxDimension: Int, quality: Int): String {
    val (w, h) = bitmap.width to bitmap.height
    val scaled = if (w > maxDimension || h > maxDimension) {
        val scale = maxDimension.toFloat() / maxOf(w, h)
        Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    } else {
        bitmap
    }
    return ByteArrayOutputStream().use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}

/**
 * Convert a Groq error envelope to the typed gateway error hierarchy.
 * The body's `type` field follows OpenAI conventions
 * (e.g. `invalid_request_error`, `rate_limit_exceeded`).
 */
private fun mapErrorBody(err: GroqChatResponse.ErrorBody): Throwable {
    val message = err.message.orEmpty()
    return when (err.type) {
        "rate_limit_exceeded" -> ProviderRecoverableError.RateLimited("Groq: $message")
        "invalid_request_error" -> ProviderFatalError.MalformedRequest("Groq: $message")
        "authentication_error", "permission_denied" ->
            ProviderFatalError.InvalidKey("Groq: $message")
        else -> ProviderRecoverableError.ServiceUnavailable("Groq: $message")
    }
}

/**
 * Translate any HTTP / network / parsing failure thrown during the
 * Retrofit call into the typed gateway hierarchy. Mirrors the canonical
 * mapping documented in skills/ai-provider-gateway/SKILL.md.
 *
 * **Never logs the response body** — Groq's error bodies can contain prompts
 * that may include user content.
 */
private fun translateError(e: Throwable): Throwable {
    // Already typed — propagate as-is.
    if (e is ProviderRecoverableError || e is ProviderFatalError) return e

    if (e is HttpException) {
        return when (val code = e.code()) {
            404 -> ProviderRecoverableError.NotFound("Groq HTTP 404 (model deprecated?)")
            408, 504 -> ProviderRecoverableError.Timeout("Groq HTTP $code")
            429 -> ProviderRecoverableError.RateLimited("Groq HTTP 429")
            in 500..599 -> ProviderRecoverableError.ServiceUnavailable("Groq HTTP $code")
            401, 403 -> ProviderFatalError.InvalidKey("Groq HTTP $code")
            413 -> ProviderFatalError.MalformedRequest("Groq payload too large")
            in 400..499 -> ProviderFatalError.MalformedRequest("Groq HTTP $code")
            else -> ProviderRecoverableError.ServiceUnavailable("Groq HTTP $code")
        }
    }

    return when (e) {
        is SocketTimeoutException -> ProviderRecoverableError.Timeout("Groq socket timeout")
        is UnknownHostException -> ProviderRecoverableError.ServiceUnavailable("Groq DNS failure")
        is java.io.IOException -> ProviderRecoverableError.ServiceUnavailable(
            "Groq network: ${e.javaClass.simpleName}"
        )
        else -> {
            Log.w("GroqProvider", "Unmapped error class ${e.javaClass.simpleName}")
            ProviderRecoverableError.ServiceUnavailable("Groq: ${e.javaClass.simpleName}")
        }
    }
}
