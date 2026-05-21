package com.mawaai.love.app.design.ai.huggingface

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface for the HuggingFace Inference API.
 *
 * Two model paths are surfaced:
 *  - `briaai/RMBG-1.4` — accepts a JPEG/PNG image, returns a PNG with the
 *    background removed (alpha channel encoded). Used by
 *    [HuggingFaceClient.removeBackground].
 *  - `lllyasviel/sd-controlnet-canny` — accepts a Canny-edge image plus a
 *    JSON `{ inputs, parameters }` payload, returns a PNG. Used by
 *    [HuggingFaceClient.controlNetFromSketch] which constructs the JSON
 *    body around a base64-encoded image.
 *
 * Both endpoints take the same `Authorization: Bearer <key>` header and
 * return raw bytes via `ResponseBody`. We use `Response<ResponseBody>` so
 * the client can read the HTTP status — HuggingFace returns 503 with a
 * JSON body containing `estimated_time` while a model warms up; the
 * client retries once after a sleep for that duration.
 */
interface HuggingFaceApi {

    /**
     * Generic image-in / image-out inference. The body is sent as
     * `application/octet-stream` raw bytes (HF's preferred format for
     * single-image models like RMBG). Returns the rendered PNG bytes
     * via `ResponseBody`; the caller decodes via `BitmapFactory`.
     */
    @POST("models/{model}")
    @Headers("Content-Type: application/octet-stream")
    suspend fun inferImage(
        @Path("model", encoded = true) model: String,
        @Header("Authorization") authorization: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * JSON-payload inference for diffusion models. ControlNet on HF takes
     * `{ inputs: "<prompt>", parameters: { ... } }` plus a base64-encoded
     * conditioning image embedded in `parameters.image`. Returns the
     * rendered PNG bytes.
     */
    @POST("models/{model}")
    @Headers("Content-Type: application/json")
    suspend fun inferJson(
        @Path("model", encoded = true) model: String,
        @Header("Authorization") authorization: String,
        @Body body: HuggingFaceJsonRequest
    ): Response<ResponseBody>
}
