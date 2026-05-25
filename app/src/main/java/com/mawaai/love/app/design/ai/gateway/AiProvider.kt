package com.mawaai.love.app.design.ai.gateway

import android.graphics.Bitmap

/**
 * Provider-agnostic vision-analysis interface.
 *
 * Concrete implementations live in `design/ai/gateway/providers/`. The gateway
 * registry assembles configured providers into a [VisionFallbackChain] so a 404
 * / 429 from one provider transparently falls through to the next.
 *
 * Hard rules:
 * - Return [Result.failure] with a typed [ProviderRecoverableError] when the
 *   chain should try the next provider (404 model deprecated, 429 rate limit,
 *   timeout, 5xx, provider-specific quota exhaustion).
 * - Return [Result.failure] with a typed [ProviderFatalError] when the user
 *   must intervene (401 invalid key, 400 malformed request, content-safety
 *   block). The chain STOPS on a fatal error.
 * - Never throw; always return [Result]. The chain branches on the sealed
 *   error hierarchy, not on string substring of `e.message`.
 */
interface VisionProvider {
    val id: ProviderId

    /** True when the API key (or equivalent credential) is present. */
    val isConfigured: Boolean

    /**
     * Send a text prompt + image and receive a text response.
     *
     * Implementations MUST translate every HTTP/network failure into either a
     * [ProviderRecoverableError] or a [ProviderFatalError] before returning.
     */
    suspend fun visionAnalyze(prompt: String, image: Bitmap): Result<String>
}

/**
 * Provider-agnostic text-generation interface. Same contract as
 * [VisionProvider] but without the image input.
 */
interface TextProvider {
    val id: ProviderId
    val isConfigured: Boolean

    suspend fun generateText(prompt: String, systemPrompt: String? = null): Result<String>
}

/**
 * Strongly-typed provider identifier. Compile-time exhaustive when checks
 * prevent silent omissions when a new provider is added.
 *
 * @property displayName User-facing label rendered in the Settings switcher.
 * @property freeTier `true` when the provider currently offers a free tier
 *   capable of running the default vision / text model. Kept as documentation
 *   only — the chain does not branch on this flag.
 */
enum class ProviderId(val displayName: String, val freeTier: Boolean) {
    GEMINI("Google Gemini", true),
    OPENROUTER("OpenRouter (auto)", true),
    GROQ("Groq (Llama Vision)", true),
    CLOUDFLARE_WORKERS_AI("Cloudflare Workers AI", true),
    HUGGINGFACE("HuggingFace Inference", true);

    companion object {
        /** Default fallback order tuned for vision quality + Arabic fluency. */
        val DEFAULT_VISION_ORDER: List<ProviderId> = listOf(
            GEMINI, OPENROUTER, GROQ, CLOUDFLARE_WORKERS_AI, HUGGINGFACE
        )

        /** Default fallback order tuned for text latency. */
        val DEFAULT_TEXT_ORDER: List<ProviderId> = listOf(
            GROQ, CLOUDFLARE_WORKERS_AI, OPENROUTER, GEMINI, HUGGINGFACE
        )
    }
}

/**
 * Errors that signal "try the next provider in the chain".
 *
 * The fallback executor catches these per-provider and continues iterating.
 */
sealed class ProviderRecoverableError(message: String) : Exception(message) {
    /** Model name no longer exists or endpoint was moved (HTTP 404). */
    class NotFound(message: String) : ProviderRecoverableError(message)

    /** Provider-side throttling (HTTP 429). */
    class RateLimited(message: String) : ProviderRecoverableError(message)

    /** Provider is temporarily down (HTTP 5xx, network errors, DNS). */
    class ServiceUnavailable(message: String) : ProviderRecoverableError(message)

    /** Request exceeded the socket / read timeout. */
    class Timeout(message: String) : ProviderRecoverableError(message)

    /** Provider account is out of credits / quota (HTTP 402 etc.). */
    class QuotaExhausted(message: String) : ProviderRecoverableError(message)
}

/**
 * Errors that should NOT trigger fallback. The user has to act.
 */
sealed class ProviderFatalError(message: String) : Exception(message) {
    /** API key missing, invalid, or revoked (HTTP 401 / 403). */
    class InvalidKey(message: String) : ProviderFatalError(message)

    /**
     * Request body is malformed in a way that all providers would reject
     * (HTTP 400 / 422 outside of safety blocks; bad image format; etc.).
     */
    class MalformedRequest(message: String) : ProviderFatalError(message)

    /** Content-policy block by the provider. Other providers may also reject. */
    class SafetyBlock(message: String) : ProviderFatalError(message)

    /**
     * Provider isn't wired up yet (used by stub adapters that ship before
     * their concrete clients). Treated as fatal so the chain skips them
     * cleanly via `isConfigured = false` rather than attempting a call.
     */
    class NotImplemented(message: String) : ProviderFatalError(message)
}
