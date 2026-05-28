package com.mawaai.love.app.design.ai.openrouter

import android.util.Log
import com.mawaai.love.app.BuildConfig
import com.mawaai.love.app.design.ai.gateway.ProviderFatalError
import com.mawaai.love.app.design.ai.gateway.ProviderRecoverableError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drop-in fallback for [com.mawaai.love.app.design.ai.gemini.GeminiClient]
 * and the text-side gateway adapter feeding
 * [com.mawaai.love.app.design.ai.gateway.TextFallbackChain].
 *
 * Two public surfaces:
 *
 *  - [chatCompletion] returns `Result<String>` with typed
 *    [ProviderRecoverableError] / [ProviderFatalError] errors. This is the
 *    contract the gateway's chain branches on; HTTP 429 → next provider,
 *    HTTP 401 → stop the chain.
 *
 *  - [inspirationPrompts] preserves the legacy bullet-list interface used by
 *    direct callers (and by `GeminiClient`'s transparent fallback). It returns
 *    an empty list on any failure so today's call sites do not have to change.
 *
 * Self-contained Retrofit — does not depend on the existing NetworkModule
 * graph. That keeps this change additive and rollback-safe: deleting the
 * `design/ai/openrouter/` directory removes the feature without touching any
 * other file. The trade-off is a second Retrofit/OkHttp instance for this
 * single endpoint, which is acceptable for a fallback path.
 *
 * Default model is `openrouter/auto` — OpenRouter resolves at request time to
 * the best available model (typically a Gemini variant when the user has a
 * Google account linked). To pin a specific model, pass it to [chatCompletion]
 * or [inspirationPrompts] explicitly.
 *
 * Origin: MT-012 in PROJECT_SCAN_CONTINUATION_2026-05-22.md.
 */
@Singleton
class OpenRouterClient @Inject constructor() {

    private val api: OpenRouterApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenRouterApi::class.java)

    val isConfigured: Boolean get() = BuildConfig.OPENROUTER_API_KEY.isNotBlank()

    /**
     * Send a single-turn chat completion. Maps every HTTP / network failure
     * into a typed [ProviderRecoverableError] or [ProviderFatalError] so the
     * gateway's [com.mawaai.love.app.design.ai.gateway.TextFallbackChain] can
     * branch correctly.
     */
    suspend fun chatCompletion(
        prompt: String,
        systemPrompt: String? = null,
        model: String = DEFAULT_MODEL,
    ): Result<String> {
        val key = BuildConfig.OPENROUTER_API_KEY
        if (key.isBlank()) {
            return Result.failure(
                ProviderFatalError.InvalidKey("OPENROUTER_API_KEY is not set in local.properties")
            )
        }

        return try {
            val text = withContext(Dispatchers.IO) {
                val messages = buildList {
                    if (!systemPrompt.isNullOrBlank()) {
                        add(OpenRouterRequest.Message(role = "system", content = systemPrompt))
                    }
                    add(OpenRouterRequest.Message(role = "user", content = prompt))
                }
                val response = api.chatCompletion(
                    authorization = "Bearer $key",
                    body = OpenRouterRequest(
                        model = model,
                        messages = messages,
                        maxTokens = DEFAULT_MAX_TOKENS,
                        temperature = DEFAULT_TEMPERATURE,
                        topP = DEFAULT_TOP_P,
                    )
                )
                if (response.error != null) {
                    throw mapBodyError(response.error)
                }
                response.choices
                    ?.firstOrNull()
                    ?.message
                    ?.content
                    .orEmpty()
            }

            if (text.isBlank()) {
                Result.failure(
                    ProviderRecoverableError.ServiceUnavailable("OpenRouter returned empty content")
                )
            } else {
                Result.success(text)
            }
        } catch (recoverable: ProviderRecoverableError) {
            Log.w(TAG, "recoverable: ${recoverable.javaClass.simpleName} ${recoverable.message}")
            Result.failure(recoverable)
        } catch (fatal: ProviderFatalError) {
            Log.e(TAG, "fatal: ${fatal.javaClass.simpleName} ${fatal.message}")
            Result.failure(fatal)
        } catch (http: HttpException) {
            Result.failure(mapHttpStatus(http.code(), http.message().orEmpty()))
        } catch (timeout: SocketTimeoutException) {
            Result.failure(
                ProviderRecoverableError.Timeout(timeout.message ?: "OpenRouter request timed out")
            )
        } catch (io: IOException) {
            Result.failure(
                ProviderRecoverableError.ServiceUnavailable(io.message ?: "OpenRouter network error")
            )
        } catch (t: Throwable) {
            // Catch-all: never throw to the caller. The chain will treat this
            // as recoverable and try the next provider.
            Log.w(TAG, "unexpected: ${t.javaClass.simpleName} ${t.message}")
            Result.failure(
                ProviderRecoverableError.ServiceUnavailable(
                    "OpenRouter unexpected error: ${t.javaClass.simpleName}"
                )
            )
        }
    }

    /**
     * Mirrors `GeminiClient.inspirationPrompts(count)`: returns a list of short
     * Arabic design-idea strings, one per line, with leading bullets stripped.
     * Returns an empty list on any failure — never throws.
     *
     * Delegates to [chatCompletion] so the typed error handling is shared with
     * the gateway adapter; the legacy emptyList-on-failure contract is then
     * applied at this surface only.
     */
    suspend fun inspirationPrompts(
        count: Int = 5,
        model: String = DEFAULT_MODEL,
    ): List<String> {
        val prompt = INSPIRATION_PROMPT_TEMPLATE.format(count)
        return chatCompletion(prompt = prompt, model = model).fold(
            onSuccess = { content ->
                content.lineSequence()
                    .map { it.trim().trimStart('-', '*', '•', '·').trim() }
                    .filter { it.isNotEmpty() }
                    .take(count)
                    .toList()
            },
            onFailure = {
                Log.w(TAG, "inspirationPrompts: ${it.javaClass.simpleName} ${it.message}")
                emptyList()
            }
        )
    }

    // ─── HTTP / body error mapping ───────────────────────────────────────────

    private fun mapHttpStatus(code: Int, statusMessage: String): Throwable = when (code) {
        401, 403 -> ProviderFatalError.InvalidKey("OpenRouter HTTP $code $statusMessage")
        400, 422 -> ProviderFatalError.MalformedRequest("OpenRouter HTTP $code $statusMessage")
        404 -> ProviderRecoverableError.NotFound("OpenRouter HTTP 404 $statusMessage")
        402 -> ProviderRecoverableError.QuotaExhausted("OpenRouter HTTP 402 out of credits")
        408 -> ProviderRecoverableError.Timeout("OpenRouter HTTP 408 $statusMessage")
        429 -> ProviderRecoverableError.RateLimited("OpenRouter HTTP 429 $statusMessage")
        in 500..599 -> ProviderRecoverableError.ServiceUnavailable("OpenRouter HTTP $code $statusMessage")
        else -> ProviderRecoverableError.ServiceUnavailable("OpenRouter HTTP $code $statusMessage")
    }

    /**
     * OpenRouter follows OpenAI's quirk of returning HTTP 200 with an `error`
     * field in some failure modes. Map the most common codes here; everything
     * unknown degrades to a recoverable service-unavailable so the chain
     * advances.
     */
    private fun mapBodyError(error: OpenRouterResponse.ErrorBody): Throwable {
        val msg = error.message ?: "OpenRouter returned an error body"
        val code = error.code?.lowercase().orEmpty()
        return when {
            "invalid_api_key" in code || "invalid_request_error" in code && "api" in msg.lowercase() ->
                ProviderFatalError.InvalidKey(msg)
            "rate_limit" in code -> ProviderRecoverableError.RateLimited(msg)
            "quota" in code || "insufficient" in code ->
                ProviderRecoverableError.QuotaExhausted(msg)
            "content_filter" in code || "safety" in code ->
                ProviderFatalError.SafetyBlock(msg)
            "model_not_found" in code -> ProviderRecoverableError.NotFound(msg)
            else -> ProviderRecoverableError.ServiceUnavailable(msg)
        }
    }

    private companion object {
        const val TAG = "OpenRouterClient"
        const val BASE_URL = "https://openrouter.ai/"
        // `openrouter/auto` auto-routes at request time. Verified in MT-011
        // (2026-05-22) that this resolves to a working model with ~1s latency
        // even when direct Gemini is quota-throttled.
        const val DEFAULT_MODEL = "openrouter/auto"
        const val DEFAULT_MAX_TOKENS = 256
        const val DEFAULT_TEMPERATURE = 0.9f
        const val DEFAULT_TOP_P = 0.95f

        const val INSPIRATION_PROMPT_TEMPLATE = """
            أعطني %d فكرة قصيرة وبسيطة باللغة العربية لرسومات يمكن تحويلها إلى تصاميم احترافية (مثل: وردة بأوراق متموجة، نجمة سداسية بزخارف، عين حورس مبسطة).
            القواعد:
            - كل فكرة في سطر منفصل.
            - بدون أرقام، بدون شرطات، بدون شرح إضافي.
            - كل فكرة ٢ إلى ٥ كلمات فقط.
        """
    }
}
