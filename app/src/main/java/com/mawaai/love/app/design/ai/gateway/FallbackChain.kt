package com.mawaai.love.app.design.ai.gateway

import android.graphics.Bitmap
import android.util.Log

/**
 * Executes a vision call across a configured list of [VisionProvider]s,
 * falling through on [ProviderRecoverableError] and stopping on
 * [ProviderFatalError] or success.
 *
 * The chain order is supplied by [ProviderRegistry] which reads it from
 * DataStore (user-configurable in Settings).
 *
 * Behaviour:
 * - Unconfigured providers (`isConfigured == false`) are skipped silently.
 * - The first [Result.success] short-circuits the chain.
 * - A [ProviderFatalError] short-circuits the chain (user must fix).
 * - All-recoverable-failure returns an aggregated [Result.failure] listing
 *   each provider's error for debugging.
 *
 * The executor logs each attempt at INFO with `tag="VisionFallbackChain"`
 * containing only the provider name, HTTP-equivalent code, and latency —
 * never the prompt, image bytes, response body, or any API key.
 */
class VisionFallbackChain(
    private val providers: List<VisionProvider>
) {

    suspend fun analyze(prompt: String, image: Bitmap): Result<String> {
        val active = providers.filter { it.isConfigured }
        if (active.isEmpty()) {
            return Result.failure(
                IllegalStateException(
                    "No AI vision provider is configured. Add at least one " +
                        "API key in local.properties (GEMINI_API_KEY, " +
                        "OPENROUTER_API_KEY, GROQ_API_KEY, CLOUDFLARE_*, or " +
                        "HUGGINGFACE_API_KEY)."
                )
            )
        }

        val errors = mutableListOf<Pair<ProviderId, Throwable>>()
        for ((index, provider) in active.withIndex()) {
            val started = System.currentTimeMillis()
            val result = provider.visionAnalyze(prompt, image)
            val elapsed = System.currentTimeMillis() - started

            if (result.isSuccess) {
                Log.i(
                    TAG,
                    "vision via ${provider.id.displayName} " +
                        "(chain pos $index) latency=${elapsed}ms"
                )
                return result
            }

            val err = result.exceptionOrNull()
                ?: ProviderRecoverableError.ServiceUnavailable(
                    "${provider.id.displayName} returned failure with no exception"
                )

            when (err) {
                is ProviderFatalError -> {
                    Log.e(
                        TAG,
                        "fatal: ${provider.id.displayName} ${err.javaClass.simpleName} " +
                            "latency=${elapsed}ms — stopping chain"
                    )
                    return Result.failure(err)
                }
                is ProviderRecoverableError -> {
                    Log.w(
                        TAG,
                        "recoverable: ${provider.id.displayName} " +
                            "${err.javaClass.simpleName} latency=${elapsed}ms — trying next"
                    )
                    errors += provider.id to err
                }
                else -> {
                    // Untyped exception (provider violated contract). Treat as
                    // recoverable to be conservative.
                    Log.w(
                        TAG,
                        "untyped: ${provider.id.displayName} " +
                            "${err.javaClass.simpleName} — trying next"
                    )
                    errors += provider.id to err
                }
            }
        }

        val summary = errors.joinToString("; ") { (id, e) ->
            "${id.displayName}=${e.javaClass.simpleName}"
        }
        return Result.failure(
            Exception("All ${active.size} vision providers failed: $summary")
        )
    }

    private companion object {
        const val TAG = "VisionFallbackChain"
    }
}

/**
 * Text-generation counterpart to [VisionFallbackChain].
 */
class TextFallbackChain(
    private val providers: List<TextProvider>
) {
    suspend fun generate(
        prompt: String,
        systemPrompt: String? = null
    ): Result<String> {
        val active = providers.filter { it.isConfigured }
        if (active.isEmpty()) {
            return Result.failure(
                IllegalStateException("No AI text provider is configured.")
            )
        }

        val errors = mutableListOf<Pair<ProviderId, Throwable>>()
        for ((index, provider) in active.withIndex()) {
            val started = System.currentTimeMillis()
            val result = provider.generateText(prompt, systemPrompt)
            val elapsed = System.currentTimeMillis() - started

            if (result.isSuccess) {
                Log.i(
                    TAG,
                    "text via ${provider.id.displayName} (chain pos $index) " +
                        "latency=${elapsed}ms"
                )
                return result
            }

            val err = result.exceptionOrNull() ?: continue
            when (err) {
                is ProviderFatalError -> {
                    Log.e(TAG, "fatal: ${provider.id.displayName} — stopping chain")
                    return Result.failure(err)
                }
                else -> errors += provider.id to err
            }
        }

        val summary = errors.joinToString("; ") { (id, e) ->
            "${id.displayName}=${e.javaClass.simpleName}"
        }
        return Result.failure(
            Exception("All ${active.size} text providers failed: $summary")
        )
    }

    private companion object {
        const val TAG = "TextFallbackChain"
    }
}
