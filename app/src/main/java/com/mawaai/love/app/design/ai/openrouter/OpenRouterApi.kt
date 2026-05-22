package com.mawaai.love.app.design.ai.openrouter

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterApi {
    /**
     * OpenRouter chat completions endpoint. Bearer auth via the Authorization header.
     * Optional `HTTP-Referer` and `X-Title` headers identify the calling app in
     * the OpenRouter dashboard.
     */
    @POST("api/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "https://mawaai.love",
        @Header("X-Title") title: String = "Mawaai",
        @Body body: OpenRouterRequest
    ): OpenRouterResponse
}
