package com.mawaai.love.app.design.ai.cloudflare

import com.google.gson.annotations.SerializedName

/**
 * JSON body for Cloudflare Workers AI image-generation models.
 * Supports both text-to-image (T2I) and image-to-image (I2I) flows.
 *
 * Field reference:
 *  - `prompt` (required) — what to draw. Up to ~2048 chars.
 *  - `negative_prompt` — what to avoid. Optional but recommended.
 *  - `num_steps` — 1..20. Defaults to 20; lower = faster, less detailed.
 *  - `guidance` — 0..15. Higher follows the prompt more rigidly.
 *  - `width` / `height` — 256..2048, multiples of 8. SDXL native is
 *    1024×1024; lighter models default to 512.
 *  - `seed` — integer for reproducible generation.
 *  - `image_b64` — base64-encoded input for img2img / inpainting models.
 *  - `strength` — 0..1, how much to deviate from the input image.
 *    0.0 = identical to input, 1.0 = ignore input. Used by img2img
 *    only; ignored by pure text-to-image models. The compose-refine
 *    flow uses ~0.30 to PRESERVE the spatial layout of the simple
 *    composite while letting the AI refine fabric folds, lighting,
 *    and edges so the design feels naturally integrated.
 *
 * Unknown fields are stripped by Gson on serialisation when the value
 * is null, so callers can pass only what they care about.
 */
data class CloudflareGenerateRequest(
    @SerializedName("prompt") val prompt: String,
    @SerializedName("negative_prompt") val negativePrompt: String? = null,
    @SerializedName("num_steps") val numSteps: Int? = null,
    @SerializedName("guidance") val guidance: Double? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("seed") val seed: Long? = null,
    @SerializedName("image_b64") val imageB64: String? = null,
    @SerializedName("strength") val strength: Double? = null
)
