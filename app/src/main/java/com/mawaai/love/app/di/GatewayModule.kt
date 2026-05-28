package com.mawaai.love.app.di

import com.mawaai.love.app.design.ai.gateway.ProviderId
import com.mawaai.love.app.design.ai.gateway.TextProvider
import com.mawaai.love.app.design.ai.gateway.VisionProvider
import com.mawaai.love.app.design.ai.gateway.providers.CloudflareTextProvider
import com.mawaai.love.app.design.ai.gateway.providers.OpenRouterTextProvider
import com.mawaai.love.app.design.ai.gateway.providers.CloudflareVisionProvider
import com.mawaai.love.app.design.ai.gateway.providers.StubTextProvider
import com.mawaai.love.app.design.ai.gateway.providers.StubVisionProvider
import com.mawaai.love.app.design.ai.groq.GroqTextProvider
import com.mawaai.love.app.design.ai.groq.GroqVisionProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * Hilt multibinding module that contributes individual [VisionProvider] and
 * [TextProvider] instances into the sets consumed by
 * [com.mawaai.love.app.design.ai.gateway.ProviderRegistry].
 *
 * Each provider lives behind its own `@IntoSet` binding so swapping a stub
 * for a real implementation is a localized 2-line edit (delete the @Provides
 * stub, add an @Binds real adapter) — no other module is touched.
 *
 * Provider matrix (post E7.MT-037):
 *
 * | ProviderId               | Vision impl                      | Text impl                      |
 * |--------------------------|----------------------------------|--------------------------------|
 * | GEMINI                   | StubVisionProvider (MT-019)      | StubTextProvider (MT-019)      |
 * | OPENROUTER               | StubVisionProvider (deferred)    | OpenRouterTextProvider (MT-012)|
 * | GROQ                     | GroqVisionProvider               | GroqTextProvider               |
 * | CLOUDFLARE_WORKERS_AI    | StubVisionProvider (MT-038)      | StubTextProvider (MT-038)      |
 * | HUGGINGFACE              | StubVisionProvider (deferred)    | StubTextProvider (deferred)    |
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GatewayModule {

    // ───── Real providers (Groq — E7.MT-037) ─────

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindGroqVision(impl: GroqVisionProvider): VisionProvider

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindGroqText(impl: GroqTextProvider): TextProvider

    // ───── Real providers (Cloudflare — E7.MT-038) ─────

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindCloudflareVision(impl: CloudflareVisionProvider): VisionProvider

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindCloudflareText(impl: CloudflareTextProvider): TextProvider

    // ───── Real providers (OpenRouter — MT-012) ─────

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindOpenRouterText(impl: OpenRouterTextProvider): TextProvider

    /**
     * Static `@Provides` lives in a companion object so it coexists with the
     * abstract `@Binds` above in the same module.
     */
    companion object {

        // ───── Vision stubs (replaced in MT-019 / MT-038) ─────

        @Provides
        @IntoSet
        @Singleton
        fun provideGeminiVisionStub(): VisionProvider =
            StubVisionProvider(ProviderId.GEMINI, "wired in MT-019")

        @Provides
        @IntoSet
        @Singleton
        fun provideOpenRouterVisionStub(): VisionProvider =
            StubVisionProvider(ProviderId.OPENROUTER, "wired in MT-019")

        @Provides
        @IntoSet
        @Singleton
        fun provideHuggingFaceVisionStub(): VisionProvider =
            StubVisionProvider(ProviderId.HUGGINGFACE, "deferred (slow batch only)")

        // ───── Text stubs (replaced in MT-019 / MT-038) ─────

        @Provides
        @IntoSet
        @Singleton
        fun provideGeminiTextStub(): TextProvider =
            StubTextProvider(ProviderId.GEMINI, "wired alongside MT-019")

        @Provides
        @IntoSet
        @Singleton
        fun provideHuggingFaceTextStub(): TextProvider =
            StubTextProvider(ProviderId.HUGGINGFACE, "deferred")
    }
}
