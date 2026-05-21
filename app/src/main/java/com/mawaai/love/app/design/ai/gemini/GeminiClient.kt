package com.mawaai.love.app.design.ai.gemini

import android.util.Log
import com.mawaai.love.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiClient @Inject constructor(
    private val api: GeminiApi
) {

    val isConfigured: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    suspend fun inspirationPrompts(count: Int = 5): List<String> {
        val key = BuildConfig.GEMINI_API_KEY
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
        }.getOrElse {
            Log.w(TAG, "Gemini inspiration prompts failed", it)
            emptyList()
        }
    }

    private companion object {
        const val TAG = "GeminiClient"
        // Same `-latest` alias deprecation as GeminiVisionClient — see
        // the inline note there. Canonical model name keeps free-tier
        // quota + the same generateContent contract.
        const val MODEL = "gemini-1.5-flash"
    }
}
