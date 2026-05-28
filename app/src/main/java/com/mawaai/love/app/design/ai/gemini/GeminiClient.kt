package com.mawaai.love.app.design.ai.gemini

import android.util.Log
import com.mawaai.love.app.BuildConfig
import com.mawaai.love.app.design.ai.openrouter.OpenRouterClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiClient @Inject constructor(
    private val api: GeminiApi,
    /**
     * MT-012: transparent OpenRouter fallback. When Gemini 429s (free-tier
     * quota, see API_HEALTH_2026-05-22.md), or any other failure occurs, we
     * call `openrouter/auto` instead so the user still gets prompts.
     *
     * OpenRouterClient is itself fail-safe — its `inspirationPrompts` returns
     * an empty list if its key is also missing, preserving the previous
     * "empty list on failure" contract callers already handle.
     */
    private val openRouterClient: OpenRouterClient,
) {

    val isConfigured: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    suspend fun inspirationPrompts(count: Int = 5): List<String> {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank()) {
            Log.i(TAG, "Gemini key not configured — delegating to OpenRouter fallback")
            return fallback(count, reason = "missing_key")
        }

        val prompt = """
            أعطني $count فكرة قصيرة وبسيطة باللغة العربية لرسومات يمكن تحويلها إلى تصاميم احترافية (مثل: وردة بأوراق متموجة، نجمة سداسية بزخارف، عين حورس مبسطة).
            القواعد:
            - كل فكرة في سطر منفصل.
            - بدون أرقام، بدون شرطات، بدون شرح إضافي.
            - كل فكرة ٢ إلى ٥ كلمات فقط.
        """.trimIndent()

        val geminiResult = runCatching {
            withContext(Dispatchers.IO) {
                val response = api.generateContent(
                    model = MODEL,
                    apiKey = key,
                    body = GeminiRequest(
                        contents = listOf(
                            GeminiRequest.Content(
                                parts = listOf(GeminiRequest.Part(text = prompt))
                            )
                        ),
                        generationConfig = GeminiRequest.GenerationConfig()
                    )
                )
                response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.mapNotNull { it.text }
                    ?.joinToString(separator = "\n")
                    ?.lineSequence()
                    ?.map { it.trim().trimStart('-', '*', '•', '·').trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.take(count)
                    ?.toList()
                    .orEmpty()
            }
        }

        val prompts = geminiResult.getOrElse { err ->
            Log.w(TAG, "Gemini inspiration prompts failed — falling back to OpenRouter", err)
            return fallback(count, reason = err.javaClass.simpleName)
        }

        // Gemini also "succeeds" with an empty list (e.g. content-safety block
        // that the API silently drops). Treat that as a recoverable failure so
        // the user still sees something.
        return if (prompts.isEmpty()) {
            Log.w(TAG, "Gemini returned no prompts — falling back to OpenRouter")
            fallback(count, reason = "empty_response")
        } else {
            prompts
        }
    }

    private suspend fun fallback(count: Int, reason: String): List<String> {
        if (!openRouterClient.isConfigured) {
            Log.w(TAG, "Cannot fall back ($reason): OpenRouter key not configured either")
            return emptyList()
        }
        val openRouterPrompts = openRouterClient.inspirationPrompts(count)
        if (openRouterPrompts.isEmpty()) {
            Log.w(TAG, "OpenRouter fallback also returned empty list")
        } else {
            Log.i(TAG, "OpenRouter fallback returned ${openRouterPrompts.size} prompts ($reason)")
        }
        return openRouterPrompts
    }

    private companion object {
        const val TAG = "GeminiClient"
        // MT-014 (2026-05-23): Gemini 1.5 family is fully deprecated. Google's
        // ListModels API now returns only the 2.0 / 2.5 flash family. The 1.5
        // alias started returning HTTP 404 for generateContent in production,
        // which previously surfaced as silent emptyList() because of the
        // runCatching block above. Pin to canonical 2.0-flash (same
        // generateContent contract, no -latest alias, free-tier accessible).
        const val MODEL = "gemini-2.0-flash"
    }
}
