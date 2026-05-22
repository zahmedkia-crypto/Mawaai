package com.mawaai.love.app.design.ai.openrouter

import android.util.Log
import com.mawaai.love.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drop-in fallback for [com.mawaai.love.app.design.ai.gemini.GeminiClient].
 *
 * Mirrors the [inspirationPrompts] contract bit-for-bit so callers can wire a
 * transparent fallback when Gemini returns 429/503 or comes back empty.
 *
 * Self-contained Retrofit — does not depend on the existing NetworkModule
 * graph. That keeps this micro-task additive and rollback-safe: deleting the
 * `design/ai/openrouter/` directory removes the feature without touching any
 * other file. The trade-off is a second Retrofit/OkHttp instance for this
 * single endpoint, which is acceptable for a fallback path.
 *
 * Default model is `openrouter/auto` — OpenRouter resolves at request time to
 * the best available model (typically a Gemini variant when the user has a
 * Google account linked). To pin a specific model, pass it to
 * [inspirationPrompts] explicitly.
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
     * Mirrors `GeminiClient.inspirationPrompts(count)` exactly: returns a list of
     * short Arabic design-idea strings, one per line, with leading bullets stripped.
     * Returns an empty list on any failure or when the API key is missing —
     * never throws.
     */
    suspend fun inspirationPrompts(
        count: Int = 5,
        model: String = DEFAULT_MODEL
    ): List<String> {
        val key = BuildConfig.OPENROUTER_API_KEY
        if (key.isBlank()) return emptyList()

        val prompt = """
            أعطني $count فكرة قصيرة وبسيطة باللغة العربية لرسومات يمكن تحويلها إلى تصاميم احترافية (مثل: وردة بأوراق متموجة، نجمة سداسية بزخارف، عين حورس مبسطة).
            القواعد:
            - كل فكرة في سطر منفصل.
            - بدون أرقام، بدون شرطات، بدون شرح إضافي.
            - كل فكرة ٢ إلى ٥ كلمات فقط.
        """.trimIndent()

        return runCatching {
            withContext(Dispatchers.IO) {
                val response = api.chatCompletion(
                    authorization = "Bearer $key",
                    body = OpenRouterRequest(
                        model = model,
                        messages = listOf(
                            OpenRouterRequest.Message(role = "user", content = prompt)
                        ),
                        maxTokens = 256,
                        temperature = 0.9f,
                        topP = 0.95f
                    )
                )
                if (response.error != null) {
                    Log.w(TAG, "OpenRouter returned error: ${response.error.message}")
                    return@withContext emptyList<String>()
                }
                response.choices
                    ?.firstOrNull()
                    ?.message
                    ?.content
                    ?.lineSequence()
                    ?.map { it.trim().trimStart('-', '*', '•', '·').trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.take(count)
                    ?.toList()
                    .orEmpty()
            }
        }.getOrElse {
            Log.w(TAG, "OpenRouter inspiration prompts failed", it)
            emptyList()
        }
    }

    private companion object {
        const val TAG = "OpenRouterClient"
        const val BASE_URL = "https://openrouter.ai/"
        // `openrouter/auto` auto-routes at request time. We verified in
        // MT-011 (2026-05-22) that this resolves to a working model with
        // ~1s latency even when direct Gemini is quota-throttled.
        const val DEFAULT_MODEL = "openrouter/auto"
    }
}
