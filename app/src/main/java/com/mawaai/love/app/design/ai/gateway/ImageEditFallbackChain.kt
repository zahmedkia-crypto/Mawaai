package com.mawaai.love.app.design.ai.gateway

import android.graphics.Bitmap
import android.util.Log
import com.mawaai.love.app.design.ai.huggingface.HuggingFaceClient
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider-agnostic image-edit (sketch -> rendered design) chain.
 *
 * Mirrors the design of [VisionFallbackChain] / [TextFallbackChain]: callers
 * never know which concrete provider produced the final bitmap, so deprecations
 * or quota exhaustion in one provider don't reach the renderer.
 *
 * Today the chain has a single concrete adapter -- HuggingFace ControlNet Canny
 * + Stable Diffusion -- because it is the only completely free img2img path
 * among the configured providers. Additional providers (Cloudflare
 * `@cf/bytedance/stable-diffusion-xl-lightning` img2img, OpenRouter image-edit
 * models when GA) can be appended without changing the [ImageEditRenderer]
 * call site.
 *
 * Error contract matches [VisionProvider]:
 * - [ProviderFatalError.InvalidKey] when no provider has credentials configured.
 * - [ProviderRecoverableError.ServiceUnavailable] when every provider returned
 *   no usable image after its own retry loop.
 *
 * The chain never throws; it always returns a [Result].
 */
@Singleton
class ImageEditFallbackChain @Inject constructor(
    private val huggingFace: Lazy<HuggingFaceClient>,
) {

    /**
     * Render [sketch] guided by [prompt] through the first configured provider.
     *
     * @return [Result.success] with the rendered bitmap, or [Result.failure]
     * with a typed sealed error describing why every provider in the chain
     * declined.
     */
    suspend fun renderFromSketch(sketch: Bitmap, prompt: String): Result<Bitmap> {
        val hf = huggingFace.get()
        if (!hf.isConfigured) {
            Log.w(TAG, "HuggingFace not configured; image-edit chain has no usable provider")
            return Result.failure(
                ProviderFatalError.InvalidKey(
                    "No image-edit provider configured. Add HUGGINGFACE_API_KEY to local.properties."
                )
            )
        }

        val rendered = hf.controlNetFromSketch(edges = sketch, prompt = prompt)
        return if (rendered != null) {
            Result.success(rendered)
        } else {
            Result.failure(
                ProviderRecoverableError.ServiceUnavailable(
                    "HuggingFace ControlNet returned no image after its retry loop."
                )
            )
        }
    }

    private companion object {
        const val TAG = "ImageEditFallbackChain"
    }
}
