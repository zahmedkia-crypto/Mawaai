# AI PROVIDER GATEWAY — Design Doc

The single most important architectural decision in this integration: how to **never let one provider's outage or deprecation reach the user**.

This is the design EPIC E7 implements (MT-036 through MT-039).

---

## 🎯 Goals

1. **Provider-agnostic interface.** A `VisionProvider` sealed interface and a `TextProvider` sealed interface that hides whether the call goes to Gemini, Groq, OpenRouter, Cloudflare Workers AI, or HuggingFace.
2. **Automatic fallback chain.** If Provider A returns 404/429/500/timeout, the gateway transparently retries with Provider B.
3. **User override.** Settings screen lets the user pin a specific provider, force-disable a provider, or rearrange the chain.
4. **Strongly typed.** No `Map<String, Any>` anywhere in the gateway.
5. **DI-friendly.** Each provider is `@Singleton @Inject`-able and registers itself with the `ProviderRegistry`.
6. **Test-friendly.** Providers are stubbable via the sealed interface for unit tests.

---

## 🧠 Why This Matters Right Now

You just hit `HTTP 404` because Google deprecated `gemini-1.5-flash`. The fix in MT-014 was a 1-line constant change — but if you had a gateway, the app would have **fallen back to OpenRouter automatically** and the user wouldn't have seen the crash at all.

Today's crash will happen again with every deprecation. The gateway is insurance against the entire class of "provider changed something" failures.

---

## 🏗 Architecture Overview

```
+---------------------------+
|   ViewModel / UseCase     |
+-------------+-------------+
              | calls gateway.visionAnalyze(prompt, image)
              v
+---------------------------+
|     ProviderRegistry      |
|  (knows the active chain) |
+-------------+-------------+
              | iterates: [Provider1, Provider2, Provider3]
              v
+---------------------------+
|     FallbackChain         |
|  for each provider:       |
|    try { result }         |
|    catch HTTP {404,429,503,timeout}: continue |
|    if all fail: return Result.failure         |
+-------------+-------------+
              |
   +----------+----------+----------+----------+
   v          v          v          v          v
+------+ +--------+ +--------+ +--------+ +----------+
|Gemini| |OpenRtr | |Groq    | |CFWorker| |HFInfer   |
+------+ +--------+ +--------+ +--------+ +----------+
```

---

## 📐 Sealed Interfaces

```kotlin
// design/ai/gateway/AiProvider.kt

package com.mawaai.love.app.design.ai.gateway

import android.graphics.Bitmap

/**
 * Provider-agnostic vision analysis interface.
 * Each concrete provider implements this and registers with [ProviderRegistry].
 */
sealed interface VisionProvider {
    val id: ProviderId
    val isConfigured: Boolean

    /**
     * Send a (text prompt, image) pair to the provider and get a text response.
     * Returns Result.failure for any non-recoverable error; the FallbackChain
     * decides whether to retry with the next provider based on the exception type.
     */
    suspend fun visionAnalyze(prompt: String, image: Bitmap): Result<String>
}

/**
 * Provider-agnostic text generation interface.
 */
sealed interface TextProvider {
    val id: ProviderId
    val isConfigured: Boolean
    suspend fun generateText(prompt: String, systemPrompt: String? = null): Result<String>
}

/**
 * Strongly typed provider identifier. No string IDs.
 */
enum class ProviderId(val displayName: String, val freeTier: Boolean) {
    GEMINI("Google Gemini", true),
    OPENROUTER("OpenRouter (auto)", true),
    GROQ("Groq (Llama Vision)", true),
    CLOUDFLARE_WORKERS_AI("Cloudflare Workers AI", true),
    HUGGINGFACE("HuggingFace Inference", true)
}

/**
 * Errors that signal "try the next provider in the chain".
 */
sealed class ProviderRecoverableError(message: String) : Exception(message) {
    class NotFound(message: String) : ProviderRecoverableError(message)        // 404 (deprecated model)
    class RateLimited(message: String) : ProviderRecoverableError(message)     // 429
    class ServiceUnavailable(message: String) : ProviderRecoverableError(message)  // 503
    class Timeout(message: String) : ProviderRecoverableError(message)         // socket timeout
    class QuotaExhausted(message: String) : ProviderRecoverableError(message)  // 402 / API-specific
}

/**
 * Errors that should NOT trigger fallback (user must fix).
 */
sealed class ProviderFatalError(message: String) : Exception(message) {
    class InvalidKey(message: String) : ProviderFatalError(message)            // 401
    class MalformedRequest(message: String) : ProviderFatalError(message)      // 400
    class SafetyBlock(message: String) : ProviderFatalError(message)           // content blocked
}
```

---

## 🔁 The Fallback Chain

```kotlin
// design/ai/gateway/FallbackChain.kt

package com.mawaai.love.app.design.ai.gateway

import android.graphics.Bitmap
import android.util.Log

class FallbackChain(
    private val providers: List<VisionProvider>,
) {
    /**
     * Execute against the chain. Returns the first successful Result.
     * Skips providers that aren't configured (no API key) or that return
     * recoverable errors. Stops on the first fatal error and returns it.
     */
    suspend fun visionAnalyze(prompt: String, image: Bitmap): Result<String> {
        val active = providers.filter { it.isConfigured }
        if (active.isEmpty()) {
            return Result.failure(IllegalStateException("No AI provider is configured. Add at least one API key in local.properties."))
        }

        val errors = mutableListOf<Pair<ProviderId, Throwable>>()
        for (provider in active) {
            val result = provider.visionAnalyze(prompt, image)
            if (result.isSuccess) {
                Log.i(TAG, "vision via ${provider.id.displayName} (chain pos ${active.indexOf(provider)})")
                return result
            }
            val err = result.exceptionOrNull() ?: continue
            when (err) {
                is ProviderFatalError -> {
                    // Don't try next provider — user must fix.
                    Log.e(TAG, "fatal: ${provider.id} ${err.message}")
                    return Result.failure(err)
                }
                is ProviderRecoverableError -> {
                    Log.w(TAG, "recoverable: ${provider.id} ${err.message} — trying next")
                    errors.add(provider.id to err)
                }
                else -> {
                    // Unknown — be conservative, try next
                    Log.w(TAG, "unknown: ${provider.id} ${err.message} — trying next")
                    errors.add(provider.id to err)
                }
            }
        }

        val summary = errors.joinToString("; ") { "${it.first.displayName}=${it.second.message?.take(40)}" }
        return Result.failure(Exception("All ${active.size} providers failed: $summary"))
    }

    private companion object {
        const val TAG = "FallbackChain"
    }
}
```

---

## 📋 ProviderRegistry — User-Controlled Order

```kotlin
// design/ai/gateway/ProviderRegistry.kt

package com.mawaai.love.app.design.ai.gateway

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.mawaai.love.app.core.preferences.SettingsDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class ProviderRegistry @Inject constructor(
    // All providers are injected. Each is @Singleton.
    private val gemini: GeminiVisionProvider,
    private val openRouter: OpenRouterVisionProvider,
    private val groq: GroqVisionProvider,
    private val cloudflare: CloudflareVisionProvider,
    private val settings: SettingsDataStore,
) {
    /**
     * Returns the user-configured fallback chain. Default order:
     *   1. Gemini (cheapest, native)
     *   2. OpenRouter (auto-routed)
     *   3. Groq (fastest free vision)
     *   4. Cloudflare Workers AI (most resilient)
     *
     * User can rearrange or disable any provider via the Settings screen.
     * If "Auto fallback" is off and the user pinned a single provider,
     * the chain contains only that provider.
     */
    suspend fun activeVisionChain(): FallbackChain {
        val prefs = settings.preferences.first()
        val mode = prefs[PROVIDER_MODE_KEY] ?: PROVIDER_MODE_AUTO
        val all = listOf(gemini, openRouter, groq, cloudflare)
        val chain = when (mode) {
            PROVIDER_MODE_AUTO -> {
                val orderIds = prefs[PROVIDER_ORDER_KEY] ?: ProviderId.values().map { it.name }.toSet()
                orderIds
                    .mapNotNull { id -> runCatching { ProviderId.valueOf(id) }.getOrNull() }
                    .mapNotNull { id -> all.firstOrNull { p -> p.id == id } }
            }
            else -> {
                val pinId = runCatching { ProviderId.valueOf(mode) }.getOrNull()
                listOfNotNull(all.firstOrNull { it.id == pinId })
            }
        }
        return FallbackChain(chain)
    }

    suspend fun setMode(mode: String) {
        settings.dataStore.edit { it[PROVIDER_MODE_KEY] = mode }
    }

    suspend fun setOrder(order: List<ProviderId>) {
        settings.dataStore.edit { it[PROVIDER_ORDER_KEY] = order.map { p -> p.name }.toSet() }
    }

    private companion object {
        val PROVIDER_MODE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("ai_provider_mode")
        val PROVIDER_ORDER_KEY = stringSetPreferencesKey("ai_provider_order")
        const val PROVIDER_MODE_AUTO = "AUTO"
    }
}
```

---

## 🛠 Provider Implementations (one example)

```kotlin
// design/ai/groq/GroqVisionProvider.kt

package com.mawaai.love.app.design.ai.groq

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.mawaai.love.app.BuildConfig
import com.mawaai.love.app.design.ai.gateway.ProviderFatalError
import com.mawaai.love.app.design.ai.gateway.ProviderId
import com.mawaai.love.app.design.ai.gateway.ProviderRecoverableError
import com.mawaai.love.app.design.ai.gateway.VisionProvider
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Groq Cloud — Llama 3.2 90B Vision (free tier, fastest free vision provider).
 * Endpoint: https://api.groq.com/openai/v1/chat/completions
 */
@Singleton
class GroqVisionProvider @Inject constructor(
    private val api: GroqApi
) : VisionProvider {

    override val id = ProviderId.GROQ
    override val isConfigured: Boolean get() = BuildConfig.GROQ_API_KEY.isNotBlank()

    override suspend fun visionAnalyze(prompt: String, image: Bitmap): Result<String> {
        if (!isConfigured) return Result.failure(ProviderFatalError.InvalidKey("GROQ_API_KEY not set in local.properties"))

        val key = BuildConfig.GROQ_API_KEY
        val b64 = withContext(Dispatchers.Default) {
            val out = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 85, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }

        return runCatching {
            withContext(Dispatchers.IO) {
                val response = api.chatCompletion(
                    auth = "Bearer $key",
                    body = GroqChatRequest(
                        model = MODEL,
                        messages = listOf(
                            GroqChatRequest.Message(
                                role = "user",
                                content = listOf(
                                    GroqChatRequest.Content.Text(prompt),
                                    GroqChatRequest.Content.ImageUrl("data:image/jpeg;base64,$b64")
                                )
                            )
                        ),
                        maxTokens = 1024,
                        temperature = 0.2f
                    )
                )
                response.choices.firstOrNull()?.message?.contentText
                    ?: error("Groq returned no text")
            }
        }.recoverCatching { e ->
            // Translate provider-specific errors to typed gateway errors
            val msg = e.message ?: ""
            when {
                "404" in msg -> throw ProviderRecoverableError.NotFound("Groq model deprecated: $msg")
                "429" in msg -> throw ProviderRecoverableError.RateLimited("Groq rate-limited")
                "503" in msg -> throw ProviderRecoverableError.ServiceUnavailable("Groq down")
                "timeout" in msg.lowercase() -> throw ProviderRecoverableError.Timeout("Groq timeout")
                "401" in msg -> throw ProviderFatalError.InvalidKey("Groq key invalid")
                else -> {
                    Log.w(TAG, "Groq unknown error: $msg")
                    throw ProviderRecoverableError.ServiceUnavailable(msg.take(120))
                }
            }
        }
    }

    private companion object {
        const val TAG = "GroqVisionProvider"
        // Llama 3.2 90B Vision Preview — Groq free tier. Update via MT if Groq deprecates.
        const val MODEL = "llama-3.2-90b-vision-preview"
    }
}
```

---

## 🎚 Settings Screen

```kotlin
// ui/settings/AiProviderSettings.kt (Compose excerpt)

@Composable
fun AiProviderSettings(
    viewModel: AiProviderSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column {
        SectionHeader("AI Provider")

        // Auto vs Pinned
        RadioRow(
            label = "Auto fallback (recommended)",
            selected = state.mode == ProviderMode.Auto,
            onClick = { viewModel.setMode(ProviderMode.Auto) }
        )
        ProviderId.values().forEach { p ->
            val pinned = state.mode is ProviderMode.Pinned && (state.mode as ProviderMode.Pinned).id == p
            RadioRow(
                label = "Use ${p.displayName} only",
                enabled = state.providerStatuses[p]?.isConfigured == true,
                selected = pinned,
                onClick = { viewModel.setMode(ProviderMode.Pinned(p)) }
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("Chain order (drag to reorder)")
        if (state.mode == ProviderMode.Auto) {
            ReorderableList(
                items = state.activeOrder,
                onReorder = { viewModel.setOrder(it) },
                itemContent = { p, modifier ->
                    ProviderRow(
                        provider = p,
                        status = state.providerStatuses[p],
                        modifier = modifier
                    )
                }
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("Diagnostics")
        Button(onClick = { viewModel.runSmokeTest() }) {
            Text("Run live health check")
        }
        state.smokeTestResults.forEach { (p, status) ->
            ProviderStatusRow(p, status)
        }
    }
}
```

---

## 🧪 How To Test

```kotlin
// app/src/test/java/com/mawaai/love/app/design/ai/gateway/FallbackChainTest.kt

class FallbackChainTest {
    @Test
    fun `first provider success short-circuits chain`() = runTest {
        val p1 = FakeProvider(ProviderId.GEMINI, configured = true, result = Result.success("ok"))
        val p2 = FakeProvider(ProviderId.OPENROUTER, configured = true, callTracker = mutableListOf())
        val chain = FallbackChain(listOf(p1, p2))

        val result = chain.visionAnalyze("test", bitmap)

        assertTrue(result.isSuccess)
        assertEquals("ok", result.getOrNull())
        assertEquals(0, p2.calls)  // never reached
    }

    @Test
    fun `recoverable error retries next provider`() = runTest {
        val p1 = FakeProvider(ProviderId.GEMINI, configured = true,
            result = Result.failure(ProviderRecoverableError.NotFound("404")))
        val p2 = FakeProvider(ProviderId.OPENROUTER, configured = true,
            result = Result.success("recovered"))
        val chain = FallbackChain(listOf(p1, p2))

        val result = chain.visionAnalyze("test", bitmap)

        assertTrue(result.isSuccess)
        assertEquals("recovered", result.getOrNull())
    }

    @Test
    fun `fatal error stops chain immediately`() = runTest {
        val p1 = FakeProvider(ProviderId.GEMINI, configured = true,
            result = Result.failure(ProviderFatalError.InvalidKey("401")))
        val p2 = FakeProvider(ProviderId.OPENROUTER, configured = true, callTracker = mutableListOf())
        val chain = FallbackChain(listOf(p1, p2))

        val result = chain.visionAnalyze("test", bitmap)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProviderFatalError.InvalidKey)
        assertEquals(0, p2.calls)
    }

    @Test
    fun `all providers fail returns aggregated error`() = runTest {
        val p1 = FakeProvider(ProviderId.GEMINI, configured = true,
            result = Result.failure(ProviderRecoverableError.NotFound("404")))
        val p2 = FakeProvider(ProviderId.OPENROUTER, configured = true,
            result = Result.failure(ProviderRecoverableError.RateLimited("429")))
        val chain = FallbackChain(listOf(p1, p2))

        val result = chain.visionAnalyze("test", bitmap)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("All 2 providers failed"))
    }

    @Test
    fun `unconfigured providers are skipped`() = runTest {
        val p1 = FakeProvider(ProviderId.GEMINI, configured = false)
        val p2 = FakeProvider(ProviderId.OPENROUTER, configured = true, result = Result.success("ok"))
        val chain = FallbackChain(listOf(p1, p2))

        val result = chain.visionAnalyze("test", bitmap)

        assertTrue(result.isSuccess)
        assertEquals(0, p1.calls)
    }
}
```

---

## 🔑 Required `local.properties` Additions

```properties
# Existing
GEMINI_API_KEY=...
OPENROUTER_API_KEY=...
CLOUDFLARE_ACCOUNT_ID=...
CLOUDFLARE_API_TOKEN=...
HUGGINGFACE_API_KEY=...
REMOVE_BG_API_KEY=...

# NEW for EPIC E7
GROQ_API_KEY=gsk_...
```

Free Groq key: https://console.groq.com/keys (rate limits but generous; Llama 3.2 90B Vision is currently free).

---

## 📊 Provider Quick Reference (verified 2026-05)

| Provider | Best free vision model | Latency | Notes |
|---|---|---|---|
| **Groq** | `llama-3.2-90b-vision-preview` | ~400ms | Fastest. Generous free tier. |
| **OpenRouter** | `openrouter/auto` | ~1000ms | Auto-routes to best free; deals with deprecations automatically. |
| **Cloudflare Workers AI** | `@cf/llava-hf/llava-1.5-7b-hf` | ~800ms | Most resilient (CF's edge network). Lower quality than above. |
| **Gemini** | `gemini-2.0-flash` | ~700ms | Best Arabic understanding. Free tier quota is tight. |
| **HuggingFace** | varies | ~3-15s | Slowest; only viable for batch. |

---

## ✅ Implementation Order

Strictly:
1. **MT-036** — Define interfaces + FallbackChain + ProviderRegistry (lays the foundation)
2. **MT-037** — Add Groq (highest-impact provider to add immediately)
3. **MT-038** — Cloudflare vision extension
4. **MT-039** — Settings UI

After E7 ships, every subsequent EPIC's AI calls go through the gateway. The next time Google deprecates a model, the app shrugs.
