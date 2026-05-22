package com.mawaai.love.app.design.ai.openrouter

import com.google.gson.annotations.SerializedName

/**
 * OpenAI-compatible chat completions DTOs as accepted by OpenRouter.
 * Reference: https://openrouter.ai/docs/api-reference/chat-completion
 */
data class OpenRouterRequest(
    val model: String,
    val messages: List<Message>,
    @SerializedName("max_tokens") val maxTokens: Int? = null,
    val temperature: Float? = null,
    @SerializedName("top_p") val topP: Float? = null
) {
    data class Message(
        val role: String,    // "system" | "user" | "assistant"
        val content: String
    )
}

data class OpenRouterResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<Choice>? = null,
    val usage: Usage? = null,
    val error: ErrorBody? = null
) {
    data class Choice(
        val index: Int? = null,
        val message: OpenRouterRequest.Message? = null,
        @SerializedName("finish_reason") val finishReason: String? = null
    )

    data class Usage(
        @SerializedName("prompt_tokens") val promptTokens: Int? = null,
        @SerializedName("completion_tokens") val completionTokens: Int? = null,
        @SerializedName("total_tokens") val totalTokens: Int? = null
    )

    data class ErrorBody(
        val message: String? = null,
        val code: String? = null
    )
}
