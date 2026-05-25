package com.mawaai.love.app.design.ai.gateway.providers

import android.graphics.Bitmap
import com.mawaai.love.app.design.ai.gateway.ProviderFatalError
import com.mawaai.love.app.design.ai.gateway.ProviderId
import com.mawaai.love.app.design.ai.gateway.TextProvider
import com.mawaai.love.app.design.ai.gateway.VisionProvider

/**
 * Reusable stub provider for endpoints that are not yet wired up.
 *
 * Used during STAGE 1 of the integration plan (E7.MT-036) to register
 * placeholder providers for Groq, Cloudflare vision, and HuggingFace before
 * their concrete clients ship in MT-037 / MT-038.
 *
 * Each stub:
 * - Returns `isConfigured = false` so the [com.mawaai.love.app.design.ai.gateway.VisionFallbackChain]
 *   silently skips it.
 * - If called anyway, returns [ProviderFatalError.NotImplemented] so the chain
 *   stops cleanly with an actionable error.
 *
 * Stub providers are intentionally `data class`-free so they can be `internal`
 * to this package and trivially replaced by concrete implementations later
 * without touching any other gateway file.
 */
internal class StubVisionProvider(
    override val id: ProviderId,
    private val nextMtNote: String,
) : VisionProvider {
    override val isConfigured: Boolean = false

    override suspend fun visionAnalyze(prompt: String, image: Bitmap): Result<String> =
        Result.failure(
            ProviderFatalError.NotImplemented(
                "${id.displayName} vision is not wired yet ($nextMtNote)."
            )
        )
}

internal class StubTextProvider(
    override val id: ProviderId,
    private val nextMtNote: String,
) : TextProvider {
    override val isConfigured: Boolean = false

    override suspend fun generateText(prompt: String, systemPrompt: String?): Result<String> =
        Result.failure(
            ProviderFatalError.NotImplemented(
                "${id.displayName} text is not wired yet ($nextMtNote)."
            )
        )
}
