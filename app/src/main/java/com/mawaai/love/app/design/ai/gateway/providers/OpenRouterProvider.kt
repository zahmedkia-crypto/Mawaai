package com.mawaai.love.app.design.ai.gateway.providers

import com.mawaai.love.app.design.ai.gateway.ProviderId
import com.mawaai.love.app.design.ai.gateway.TextProvider
import com.mawaai.love.app.design.ai.openrouter.OpenRouterClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TextProvider] adapter for [OpenRouterClient].
 *
 * Wires the OpenRouter chat-completions endpoint into the
 * [com.mawaai.love.app.design.ai.gateway.TextFallbackChain] so any caller that
 * goes through [com.mawaai.love.app.design.ai.gateway.ProviderRegistry] picks
 * OpenRouter up automatically.
 *
 * Provider position: third in [ProviderId.DEFAULT_TEXT_ORDER] (after Groq and
 * Cloudflare Workers AI, ahead of Gemini and HuggingFace). The order is
 * user-configurable via Settings.
 *
 * Closes MT-012 part A — the legacy [OpenRouterClient.inspirationPrompts]
 * convenience continues to exist for direct callers (notably GeminiClient's
 * transparent fallback in MT-012 part B).
 */
@Singleton
class OpenRouterTextProvider @Inject constructor(
    private val client: OpenRouterClient,
) : TextProvider {

    override val id: ProviderId = ProviderId.OPENROUTER

    override val isConfigured: Boolean get() = client.isConfigured

    override suspend fun generateText(
        prompt: String,
        systemPrompt: String?,
    ): Result<String> = client.chatCompletion(prompt = prompt, systemPrompt = systemPrompt)
}
