---
name: ai-provider-gateway
description: Designs and reviews multi-provider AI gateway abstractions for mobile apps. Use whenever wiring more than one AI provider behind a unified interface, building a fallback chain across providers (Gemini, OpenRouter, Groq, Cloudflare, HuggingFace), translating provider-specific errors into typed domain errors, or designing the settings UX for user-driven provider selection. Activates when the user is being burned by single-provider deprecations (e.g. Gemini 1.5 → 404), wants to add a new free provider, or needs a kill-switch to bypass a misbehaving provider in production.
icon: shuffle
color: Teal
---

# AI Provider Gateway

A discipline skill for keeping mobile apps resilient when AI providers deprecate models, rate-limit, or simply disappear.

## When To Use

- Designing or modifying a multi-provider AI abstraction (VisionProvider, TextProvider, ImageEditProvider, AudioProvider, etc.)
- Wiring a NEW AI provider into an existing app
- A user-reported crash is caused by a deprecated provider model (e.g. `HTTP 404 models/gemini-1.5-flash is not found`)
- Building "Auto fallback" or "Pin provider" preference UX
- Reviewing a PR that adds, removes, or changes a provider
- Writing a smoke-test harness against multiple AI providers
- The user asks "how do I switch to Groq / OpenRouter / Cloudflare / ..."

If the task is calling a SINGLE provider directly and there is no fallback intent, do not activate — use `mobile-ai-api-integrator` instead.

## Hard Rules

1. **One typed interface per modality.** `VisionProvider`, `TextProvider`, `ImageEditProvider` are sealed interfaces. Each concrete provider implements exactly one (or several, if they support multiple modalities).

2. **ProviderId is an enum, not a string.** Compile-time exhaustiveness > stringly typed.

3. **Errors are sealed too.** `ProviderRecoverableError` (404, 429, 503, timeout, quota) triggers the next provider in the chain. `ProviderFatalError` (401, 403, 400, content-blocked) stops the chain immediately.

4. **Adapters wrap, never modify.** When wiring an existing client (GeminiVisionClient, OpenRouterClient) into the gateway, write an Adapter class that calls the existing client's public methods. Do NOT edit the existing client.

5. **Configuration via BuildConfig, never hard-coded.** Each provider reads its key from a BuildConfig field populated from `local.properties` (gitignored).

6. **Never log keys, base64 payloads, or response bodies that may contain user content.** Log provider name, HTTP code, latency only.

7. **Fallback order is user-controlled.** The default order is shipped but the user MUST be able to rearrange, disable, or pin a single provider via Settings.

8. **One provider, one file (per modality).** A `GroqVisionProvider` lives in its own file. Do not combine multiple providers in a single Kotlin file — it hurts diff readability and complicates future deprecation cleanup.

## Architecture (Mandatory Shape)

```
design/ai/gateway/
├── AiProvider.kt              sealed VisionProvider + TextProvider + ImageEditProvider
│                              ProviderId enum
│                              ProviderRecoverableError + ProviderFatalError sealed hierarchies
├── FallbackChain.kt           the chain executor
├── ProviderRegistry.kt        DI registry; reads user-configured order from DataStore
└── adapters/
    ├── GeminiVisionProviderAdapter.kt        wraps existing GeminiVisionClient
    ├── OpenRouterVisionProviderAdapter.kt    wraps existing OpenRouterClient
    ├── GroqVisionProviderAdapter.kt          wraps Groq client
    └── CloudflareVisionProviderAdapter.kt    wraps Cloudflare client
```

Concrete provider client packages stay in their own folders:
```
design/ai/gemini/        existing GeminiVisionClient, GeminiClient
design/ai/openrouter/    existing OpenRouterClient
design/ai/groq/          NEW — GroqApi, GroqClient, GroqDtos
design/ai/cloudflare/    existing CloudflareClient (possibly extended)
```

## Adapter Pattern (Canonical)

```kotlin
@Singleton
class GroqVisionProviderAdapter @Inject constructor(
    private val client: GroqClient
) : VisionProvider {
    override val id = ProviderId.GROQ
    override val isConfigured: Boolean get() = client.isConfigured

    override suspend fun visionAnalyze(prompt: String, image: Bitmap): Result<String> =
        client.visionAnalyze(prompt, image)
            .recoverCatching { e -> throw translateError(e) }
}

private fun translateError(e: Throwable): Throwable {
    val httpCode = (e as? retrofit2.HttpException)?.code()
    return when (httpCode) {
        404 -> ProviderRecoverableError.NotFound("Model deprecated: ${e.message}")
        429 -> ProviderRecoverableError.RateLimited("Rate limited")
        503 -> ProviderRecoverableError.ServiceUnavailable("Provider unavailable")
        in 500..599 -> ProviderRecoverableError.ServiceUnavailable("Server error $httpCode")
        401, 403 -> ProviderFatalError.InvalidKey("Auth failed")
        in 400..499 -> ProviderFatalError.MalformedRequest("HTTP $httpCode")
        null -> when (e) {
            is java.net.SocketTimeoutException -> ProviderRecoverableError.Timeout("Socket timeout")
            is java.io.IOException -> ProviderRecoverableError.ServiceUnavailable("Network: ${e.message}")
            else -> ProviderRecoverableError.ServiceUnavailable("Unknown: ${e.message}")
        }
        else -> ProviderRecoverableError.ServiceUnavailable("HTTP $httpCode")
    }
}
```

## Settings UX Pattern

The user MUST be able to:
- Toggle between "Auto fallback" and "Pin a single provider".
- See which providers are configured (has API key) vs unconfigured.
- Reorder the auto-fallback chain via drag.
- Run a live health check that reports each provider's HTTP status + latency without storing the result.

Do NOT show API key values in the UI. Show only "configured ✓" or "key missing".

## Anti-Patterns (Refuse)

| Smell | Refusal |
|---|---|
| `Map<String, Any>` for provider config | "Each provider gets a `BuildConfig.X_API_KEY` field; settings are typed Kotlin data classes." |
| String-typed provider IDs | "Use the ProviderId enum so a new provider becomes a compile-time addition." |
| One adapter modifying multiple clients | "One adapter per provider per modality. Split." |
| Adapter editing the wrapped client | "Adapters call public methods. If the client needs new public methods, that's a separate MT." |
| FallbackChain that swallows ProviderFatalError | "Fatal errors must propagate so the user sees a real failure they can fix." |
| Adding HuggingFace as a vision provider for production | "HF is too slow (~5-15s) for interactive flows. Use it for batch only." |
| Adding a provider without smoke-test coverage | "Every new provider must have one entry in the ApiHealthSmokeTest opt-in suite." |
| Hard-coding model names inline | "Model names live in a `private companion object { const val MODEL = … }` so deprecations are 1-line fixes." |

## Output Discipline

When generating diffs for any provider addition:

1. **Phase header** — Which provider, which modality
2. **Files added** — explicit list, must follow the architecture shape above
3. **Files modified** — explicit list; in 99% of cases this is just `app/build.gradle.kts` + one adapter file
4. **Verification** — exact commands the human runs:
   - `./gradlew assembleDebug && ./gradlew test`
   - `grep "<PROVIDER>_API_KEY" app/build.gradle.kts` (must show exactly 2 occurrences)
   - `grep "<ProviderClass>" design/ai/gateway/adapters/<Provider>VisionProviderAdapter.kt`
5. **Rollback** — `git revert <sha>` plus delete the provider's package + adapter file

## References (read on demand)

- `references/provider-quick-reference.md` — verified models, latency, free-tier limits for 5 providers as of 2026-05
- `references/error-translation-table.md` — full HTTP code → typed error mapping
- `references/smoke-test-recipes.md` — copy-paste curl + Kotlin smoke tests per provider

## Assets

- `assets/provider-adapter-template.kt.md` — boilerplate for adding a new provider
- `assets/fallback-chain-test-template.kt.md` — boilerplate test cases

## Activation Checklist

When this skill activates, immediately verify:

- [ ] Does the gateway package exist (`design/ai/gateway/AiProvider.kt`)? If no, this is MT-036 territory — build the foundation FIRST.
- [ ] Is the provider already wrapped by an existing client class? If yes, write an Adapter; do NOT rewrite the client.
- [ ] Is the provider's API key already a BuildConfig field? If no, add it as a one-line change in `app/build.gradle.kts`.
- [ ] Is the model name pinned to a canonical (non `-latest`, non-deprecated) string? Check the provider's ListModels equivalent.
- [ ] Is there a smoke test for the provider in `ApiHealthSmokeTest`? If no, add one in the same commit.

If any check fails, STOP and produce a corrective micro-task before doing any new work.
