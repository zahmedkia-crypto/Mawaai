package com.mawaai.love.app.design.ai.groq

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for Groq Cloud's OpenAI-compatible REST API.
 *
 * Auth is Bearer header. There is no per-account path component (unlike
 * Cloudflare); the same base URL serves all Groq accounts.
 *
 * Throws [retrofit2.HttpException] on non-2xx responses; the consuming
 * provider adapter translates these into the typed gateway error hierarchy
 * before returning.
 */
interface GroqApi {

    /**
     * Chat completion endpoint. Used for both vision (with `image_url`
     * content parts) and text-only generation — the model name decides
     * whether multimodal content is accepted.
     */
    @POST("openai/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body body: GroqChatRequest,
    ): GroqChatResponse
}
