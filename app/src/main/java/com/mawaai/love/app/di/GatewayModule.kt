package com.mawaai.love.app.di

import com.mawaai.love.app.design.ai.gateway.ProviderId
import com.mawaai.love.app.design.ai.gateway.TextProvider
import com.mawaai.love.app.design.ai.gateway.VisionProvider
import com.mawaai.love.app.design.ai.gateway.providers.StubTextProvider
import com.mawaai.love.app.design.ai.gateway.providers.StubVisionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import javax.inject.Singleton

/**
 * Hilt module that registers concrete [VisionProvider] and [TextProvider]
 * instances into a multibinding `Set<...>` consumed by
 * [com.mawaai.love.app.design.ai.gateway.ProviderRegistry].
 *
 * E7.MT-036 (this commit) registers ONLY the stub providers so the registry
 * compiles end-to-end without touching any existing AI client.
 *
 * Subsequent micro-tasks add real providers by appending to the sets:
 * - MT-037: GroqVisionProvider + GroqTextProvider replace StubVisionProvider(GROQ)
 * - MT-038: CloudflareVisionProvider replaces StubVisionProvider(CLOUDFLARE_WORKERS_AI)
 * - MT-019: GeminiVisionProviderAdapter wraps the existing GeminiVisionClient
 * - (later): OpenRouterTextProviderAdapter wraps the existing OpenRouterClient
 *
 * Each replacement is a 1-file change here: delete the stub element binding
 * and add the real provider binding. No existing AI client is modified.
 */
@Module
@InstallIn(SingletonComponent::class)
object GatewayModule {

    @Provides
    @Singleton
    @ElementsIntoSet
    fun provideVisionProviders(): Set<@JvmSuppressWildcards VisionProvider> = setOf(
        StubVisionProvider(ProviderId.GEMINI, "wired in MT-019"),
        StubVisionProvider(ProviderId.OPENROUTER, "wired in MT-019"),
        StubVisionProvider(ProviderId.GROQ, "wired in MT-037"),
        StubVisionProvider(ProviderId.CLOUDFLARE_WORKERS_AI, "wired in MT-038"),
        StubVisionProvider(ProviderId.HUGGINGFACE, "deferred (slow batch only)"),
    )

    @Provides
    @Singleton
    @ElementsIntoSet
    fun provideTextProviders(): Set<@JvmSuppressWildcards TextProvider> = setOf(
        StubTextProvider(ProviderId.GEMINI, "wired alongside MT-019"),
        StubTextProvider(ProviderId.OPENROUTER, "wired alongside MT-019"),
        StubTextProvider(ProviderId.GROQ, "wired in MT-037"),
        StubTextProvider(ProviderId.CLOUDFLARE_WORKERS_AI, "wired in MT-038"),
        StubTextProvider(ProviderId.HUGGINGFACE, "deferred"),
    )
}
