package com.mawaai.love.app.design.ai.groq

import com.google.gson.annotations.SerializedName

/**
 * OpenAI-compatible chat completion DTOs as accepted by Groq Cloud
 * (https://console.groq.com/docs/api-reference#chat-create).
 *
 * Designed to be **reusable** for any OpenAI-compatible provider (Groq,
 * OpenRouter, DeepSeek, Together, Fireworks, etc.) — keeping this DTO set
 * in the groq package because Groq is its first consumer. When MT-038 wires
 * Cloudflare LLaVA (which uses a different request shape) it gets its own
 * DTO set in `design/ai/cloudflare/`.
 *
 * The `content` field on a Message is a **list of typed parts** to support
 * multimodal input. Each Content carries `type = "text"` or
 * `type = "image_url"`. Both `text` and `imageUrl` are nullable; exactly one
 * MUST be set per Content. Factory helpers [Content.text] and
 * [Content.imageUrl] enforce this without exposing a sealed-class polymorphism
 * problem to Gson (Gson handles flat data classes well but fights sealed
 * hierarchies without custom adapters).
 */
data class GroqChatRequest(
    val model: String,
    val messages: List<Message>,
    @SerializedName("max_tokens") val maxTokens: Int? = null,
    val temperature: Float? = null,
    @SerializedName("top_p") val topP: Float? = null,
    val stream: Boolean? = false,
) {
    data class Message(
        val role: String,                    // "system" | "user" | "assistant"
        val content: List<Content>,
    )

    /**
     * Multimodal content part. Either `text` is set (when [type] == "text") or
     * `imageUrl` is set (when [type] == "image_url") — never both, never neither.
     */
    data class Content(
        val type: String,
        val text: String? = null,
        @SerializedName("image_url") val imageUrl: ImageUrl? = null,
    ) {
        companion object {
            fun text(value: String): Content =
                Content(type = "text", text = value)

            fun imageUrl(dataUrl: String): Content =
                Content(type = "image_url", imageUrl = ImageUrl(url = dataUrl))
        }

        data class ImageUrl(val url: String)
    }
}

/**
 * Chat completion response envelope. Groq mirrors OpenAI's response shape.
 *
 * Empty `choices` or non-null `error` is treated as a failure by the provider
 * adapter (translated to a typed gateway error before returning).
 */
data class GroqChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<Choice>? = null,
    val usage: Usage? = null,
    val error: ErrorBody? = null,
) {
    data class Choice(
        val index: Int? = null,
        val message: ResponseMessage? = null,
        @SerializedName("finish_reason") val finishReason: String? = null,
    )

    /**
     * Response messages from Groq carry `content` as a plain String, NOT a
     * List<Content> — this is asymmetric with the request shape but matches
     * the OpenAI/Groq contract.
     */
    data class ResponseMessage(
        val role: String? = null,
        val content: String? = null,
    )

    data class Usage(
        @SerializedName("prompt_tokens") val promptTokens: Int? = null,
        @SerializedName("completion_tokens") val completionTokens: Int? = null,
        @SerializedName("total_tokens") val totalTokens: Int? = null,
    )

    data class ErrorBody(
        val message: String? = null,
        val type: String? = null,
        val code: String? = null,
    )
}
