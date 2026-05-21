package com.mawaai.love.app.design.ai.gemini

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    val contents: List<Content>,
    @SerializedName("generationConfig") val generationConfig: GenerationConfig? = null
) {
    data class Content(val parts: List<Part>)

    /**
     * Either [text] or [inlineData] should be set on a given part. The Gemini
     * 1.5 endpoint accepts multimodal contents in the same `parts` array.
     */
    data class Part(
        val text: String? = null,
        @SerializedName("inline_data") val inlineData: InlineData? = null
    )

    data class InlineData(
        @SerializedName("mime_type") val mimeType: String,
        val data: String // base64-encoded
    )

    data class GenerationConfig(
        val temperature: Float = 0.9f,
        @SerializedName("maxOutputTokens") val maxOutputTokens: Int = 256,
        @SerializedName("topP") val topP: Float = 0.95f
    )
}

data class GeminiResponse(
    val candidates: List<Candidate>?
) {
    data class Candidate(val content: Content?)
    data class Content(val parts: List<Part>?)
    data class Part(val text: String?)
}
