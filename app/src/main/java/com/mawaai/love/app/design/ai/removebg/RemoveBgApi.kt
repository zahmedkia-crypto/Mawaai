package com.mawaai.love.app.design.ai.removebg

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Retrofit interface for the remove.bg HTTP API.
 *
 * Free tier: 50 calls/month at preview resolution (~0.25 MP, 612×408 px).
 * Higher resolutions consume credit balance instead of the monthly quota.
 *
 * Auth: `X-Api-Key: <key>` header. Errors return JSON; success returns
 * raw PNG bytes with the alpha channel encoded by the model — far cleaner
 * cuts than HuggingFace's `briaai/RMBG-1.4` on cluttered backgrounds, at
 * the cost of a much smaller free quota.
 */
interface RemoveBgApi {

    /**
     * Sends [imagePart] to remove.bg's `removebg` endpoint and returns
     * the PNG bytes via `ResponseBody`. `size=preview` keeps the call
     * within the free-tier quota (anything bigger debits credits).
     *
     * `Response<ResponseBody>` lets the client inspect the HTTP status —
     * remove.bg returns 402 (Payment Required) when the monthly quota
     * is exhausted, which the client surfaces by returning null so the
     * AIEngine can fall back to ML Kit / HuggingFace RMBG.
     */
    @Multipart
    @POST("v1.0/removebg")
    suspend fun removeBg(
        @Header("X-Api-Key") apiKey: String,
        @Part imagePart: MultipartBody.Part,
        @Part("size") size: RequestBody
    ): Response<ResponseBody>

    /**
     * Returns the account's remaining quota. The client uses this for the
     * MT-011 pre-flight check so we never burn upload bandwidth on a call
     * that we already know will 402.
     *
     * Endpoint reference:
     *   GET https://api.remove.bg/v1.0/account
     *
     * Free tier: 50 preview-resolution calls per calendar month. Higher
     * sizes consume credit balance. The response separates the two so the
     * client can decide which is relevant for the requested size.
     */
    @GET("v1.0/account")
    suspend fun getAccount(
        @Header("X-Api-Key") apiKey: String
    ): Response<RemoveBgAccountResponse>
}
