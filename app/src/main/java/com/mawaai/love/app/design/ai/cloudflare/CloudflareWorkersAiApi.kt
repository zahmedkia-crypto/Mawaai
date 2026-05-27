package com.mawaai.love.app.design.ai.cloudflare

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface for Cloudflare Workers AI image-generation models.
 *
 * Base URL: `https://api.cloudflare.com/client/v4/accounts/{ACCOUNT_ID}/ai/run/`.
 * The account ID is path-injected by the client at call time (read from
 * `BuildConfig.CLOUDFLARE_ACCOUNT_ID`) because the Retrofit base URL
 * cannot be parameterised per-request without rebuilding the client.
 *
 * Auth: `Authorization: Bearer <token>` header. Model paths start with
 * `@cf/...` and contain slashes, so all path segments are passed with
 * `encoded = true` to prevent Retrofit's URL encoder from mangling them.
 *
 * Image-generation models return raw PNG/JPEG bytes via `application/
 * octet-stream` — not the standard JSON envelope used by text models.
 * The client decodes via `BitmapFactory`.
 */
interface CloudflareWorkersAiApi {

    /**
     * Generic text-to-image inference endpoint. Used for SDXL, SDXL
     * Lightning, FLUX, Dreamshaper LCM, and any other CF-hosted T2I
     * model that follows the standard image-stream contract.
     */
    @POST("client/v4/accounts/{account}/ai/run/{model}")
    @Headers("Content-Type: application/json")
    suspend fun generateImage(
        @Path("account") accountId: String,
        @Path("model", encoded = true) model: String,
        @Header("Authorization") authorization: String,
        @Body body: CloudflareGenerateRequest
    ): Response<ResponseBody>

    /**
     * Text generation (LLM) endpoint.
     */
    @POST("client/v4/accounts/{account}/ai/run/{model}")
    @Headers("Content-Type: application/json")
    suspend fun generateText(
        @Path("account") accountId: String,
        @Path("model", encoded = true) model: String,
        @Header("Authorization") authorization: String,
        @Body body: CloudflareTextRequest
    ): Response<CloudflareAiResponse>

    /**
     * Vision (LLaVA) endpoint.
     */
    @POST("client/v4/accounts/{account}/ai/run/{model}")
    @Headers("Content-Type: application/json")
    suspend fun analyzeVision(
        @Path("account") accountId: String,
        @Path("model", encoded = true) model: String,
        @Header("Authorization") authorization: String,
        @Body body: CloudflareVisionRequest
    ): Response<CloudflareAiResponse>
}
