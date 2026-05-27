package com.mawaai.love.app.design.ai.gateway.providers

import android.graphics.Bitmap
import com.mawaai.love.app.design.ai.cloudflare.CloudflareWorkersAiClient
import com.mawaai.love.app.design.ai.gateway.ProviderId
import com.mawaai.love.app.design.ai.gateway.ProviderRecoverableError
import com.mawaai.love.app.design.ai.gateway.TextProvider
import com.mawaai.love.app.design.ai.gateway.VisionProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudflareVisionProvider @Inject constructor(
    private val client: CloudflareWorkersAiClient
) : VisionProvider {
    override val id: ProviderId = ProviderId.CLOUDFLARE_WORKERS_AI
    override val isConfigured: Boolean get() = client.isConfigured

    override suspend fun visionAnalyze(prompt: String, image: Bitmap): Result<String> {
        val response = client.analyzeVision(prompt, image)
        return if (response != null) {
            Result.success(response)
        } else {
            // Cloudflare client currently returns null on any error.
            // In a real app, we'd map HTTP 429 to RateLimited, 5xx to ServiceUnavailable, etc.
            Result.failure(ProviderRecoverableError.ServiceUnavailable("Cloudflare Workers AI vision failed"))
        }
    }
}

@Singleton
class CloudflareTextProvider @Inject constructor(
    private val client: CloudflareWorkersAiClient
) : TextProvider {
    override val id: ProviderId = ProviderId.CLOUDFLARE_WORKERS_AI
    override val isConfigured: Boolean get() = client.isConfigured

    override suspend fun generateText(prompt: String, systemPrompt: String?): Result<String> {
        val response = client.generateText(prompt, systemPrompt)
        return if (response != null) {
            Result.success(response)
        } else {
            Result.failure(ProviderRecoverableError.ServiceUnavailable("Cloudflare Workers AI text failed"))
        }
    }
}
