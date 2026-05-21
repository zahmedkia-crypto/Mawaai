package com.mawaai.love.app.design.ai.huggingface

import com.google.gson.annotations.SerializedName

/**
 * JSON request envelope for diffusion models (ControlNet, Stable
 * Diffusion variants). HuggingFace expects `{ inputs: "<prompt>",
 * parameters: { ... } }` for these endpoints; the conditioning image is
 * threaded into [HuggingFaceJsonParameters.image] as a base64 string.
 */
data class HuggingFaceJsonRequest(
    @SerializedName("inputs") val inputs: String,
    @SerializedName("parameters") val parameters: HuggingFaceJsonParameters? = null
)

data class HuggingFaceJsonParameters(
    /** Base64-encoded conditioning image (Canny edges, depth map, etc.). */
    @SerializedName("image") val image: String? = null,
    /** Negative prompt — what NOT to generate. Optional. */
    @SerializedName("negative_prompt") val negativePrompt: String? = null,
    /** Number of denoising steps. Higher = better quality but slower. */
    @SerializedName("num_inference_steps") val numInferenceSteps: Int? = null,
    /** Classifier-free guidance scale. Higher = follows prompt more strictly. */
    @SerializedName("guidance_scale") val guidanceScale: Double? = null
)

/**
 * Cold-start retry payload. When a HuggingFace model isn't loaded into
 * GPU memory yet, the API responds with 503 + JSON
 * `{ "error": "Model is currently loading", "estimated_time": <seconds> }`.
 * We parse `estimated_time` from a failed response, sleep for that
 * duration (capped at [HuggingFaceClient.MAX_COLD_START_SLEEP_MS]), and
 * retry once.
 */
data class HuggingFaceErrorPayload(
    @SerializedName("error") val error: String?,
    @SerializedName("estimated_time") val estimatedTime: Double?
)
