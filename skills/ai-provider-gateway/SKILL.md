---
name: ai-provider-gateway
description: Designs and maintains multi-provider AI client abstractions on Android — typed VisionProvider/TextProvider sealed interfaces, automatic fallback chains across Gemini, OpenRouter, Groq, Cloudflare Workers AI, HuggingFace, user-configurable provider order, deprecation-resilient model registries, and strongly-typed recoverable/fatal error translation. Use whenever adding a new AI provider, debugging an HTTP 404/429 from a single provider, building a Settings screen for AI selection, designing a fallback chain executor, or refactoring code that hardcodes a single Gemini/OpenAI client. Pairs with mobile-ai-api-integrator (for individual provider Retrofit clients) and production-readiness-auditor (for hygiene/audit reports).
icon: shuffle
color: Teal
related_server_ids: [github]
---

# AI Provider Gateway

A specialist skill for the **multi-provider AI client abstraction** discipline on Android. Born from MT-014 in the MAWAAI project (where Google's deprecation of `gemini-1.5-flash` crashed the production app with HTTP 404). The lesson: **never let one provider's outage or deprecation reach the user**.

## When to Use

Activate this skill whenever any of these are true:

- Adding a new AI provider (Groq, OpenRouter, Cloudflare Workers AI, HuggingFace, OpenAI, Anthropic, etc.)
- Debugging an HTTP 404 from a provider that previously worked (model deprecation)
- Designing a Settings screen that lets the user pick or reorder AI providers
- Refactoring code that hardcodes a single Gemini/OpenAI/Claude client
- Building or testing the FallbackChain executor
- Reviewing AI client code for hygiene (no leaked keys, no `Map<String, Any>`, typed errors)

If the task is just "add one more endpoint to an existing client and don't change anything else", do not activate — use `mobile-ai-api-integrator` instead.

## Hard Rules

1. **Provider-agnostic interface first.** Define `sealed interface VisionProvider` and `sealed interface TextProvider` BEFORE writing any concrete client. New providers register against the interface, not the other way around.

2. **Typed errors, not strings.** Every recoverable failure (404 / 429 / 503 / timeout / quota) becomes a `ProviderRecoverableError` subclass. Every fatal failure (401 / 400 / safety block) becomes a `ProviderFatalError` subclass. The chain executor branches on `when` over the sealed hierarchy — never on `e.message.contains("404")`.

3. **No `Map<String, Any>`.** Every chat/vision request body is a `data class` with `@SerializedName` mappings. OpenAI-compatible providers (Groq, OpenRouter, etc.) all reuse the same `ChatCompletionRequest` shape — define it once.

4. **Fallback continues on recoverable, stops on fatal.** A `ProviderFatalError.InvalidKey` means the user needs to fix their config; do NOT try the next provider (it would mask the real problem). A `ProviderRecoverableError.NotFound` (provider deprecated a model) means "try next" — that's exactly what saves the app from the next deprecation.

5. **User-controlled order via DataStore.** The provider chain order is a user preference, not a constant. Default order is documented (Gemini → OpenRouter → Groq → Cloudflare → HF) but the user can rearrange or pin.

6. **Skip unconfigured providers.** Each provider has `isConfigured: Boolean` reading from `BuildConfig`. The chain skips providers whose `isConfigured == false`. If all are unconfigured, return a `Result.failure` explaining the user must add at least one key.

7. **No API key in any UI string.** Settings screen shows only configured/not, never the key value itself. Health-check diagnostics show PASS/FAIL + latency, never the response body that may echo headers.

8. **Each provider in its own package.** `design/ai/groq/`, `design/ai/cloudflare/`, etc. Pure additive — adding a new provider does not touch any existing provider's files.

## Workflow

For every "add provider X" or "build a switcher" task:

### Phase 0 — Confirm scope

Echo back the user's intent in 3 bullets:
- Which provider(s) are being added?
- Is this just a new provider, or does the gateway abstraction itself need work?
- Is there an immediate user-facing crash (like HTTP 404) we're fixing, or is this pre-emptive resilience work?

### Phase 1 — Verify the gateway foundation exists

Read these files (only these):
- `design/ai/gateway/AiProvider.kt`
- `design/ai/gateway/FallbackChain.kt`
- `design/ai/gateway/ProviderRegistry.kt`

If they don't exist, building them is the first MT. See `references/gateway-bootstrap.md` for the foundation template.

### Phase 2 — Define the new provider's contract

For each new provider:
- Verify the auth method (Bearer header? `?key=` query? `X-Api-Key` header?)
- Verify the request shape (OpenAI-compatible chat completions? Anthropic messages format? Google generateContent?)
- Verify the response shape (where's the text? where's the image data URL?)
- Identify which HTTP codes map to recoverable vs fatal errors
- Confirm the free-tier model name + when it last worked (audit date)

Document all of this in a `references/<provider>-protocol.md` if multiple providers share the family (e.g. one doc for the OpenAI-compatible family covering Groq, OpenRouter, etc.).

### Phase 3 — Write the provider

One file per concrete provider, in its own package. Pattern:

```kotlin
@Singleton
class GroqVisionProvider @Inject constructor(
    private val api: GroqApi
) : VisionProvider {

    override val id = ProviderId.GROQ
    override val isConfigured get() = BuildConfig.GROQ_API_KEY.isNotBlank()

    override suspend fun visionAnalyze(prompt: String, image: Bitmap): Result<String> {
        if (!isConfigured) return Result.failure(
            ProviderFatalError.InvalidKey("GROQ_API_KEY not set")
        )
        // Encode bitmap on Dispatchers.Default, call API on Dispatchers.IO.
        // Translate HTTP errors to typed gateway errors.
        // Return Result with the model's text content.
    }

    private companion object {
        const val TAG = "GroqVisionProvider"
        // Pin the model with audit date so deprecations are tracked.
        const val MODEL = "llama-3.2-90b-vision-preview" // verified 2026-05-25
    }
}
```

Anti-pattern to avoid: a provider that throws raw `retrofit2.HttpException`. Always translate to the gateway's typed errors so the chain executor can branch correctly.

### Phase 4 — Register with the chain

Update `ProviderRegistry` to include the new provider in the default chain. Update the Settings UI to expose it as a pickable option.

### Phase 5 — Health-check the live API

Run a smoke test (gated by `MAWAAI_RUN_LIVE_API_TESTS=1` env var) hitting the provider's lightest billable endpoint. Document the latency baseline so the user knows what "healthy" looks like.

### Phase 6 — Document the deprecation policy

Add a note to the provider's KDoc:
- Model name + audit date
- Where to check for deprecation announcements (provider's release notes URL)
- Replacement model if/when this one is deprecated (next-best free option)

## Output Format

Every response from this skill must include:

1. **Provider scope**: which provider(s) are being added or modified
2. **Auth + request shape verification**: copy-pasteable curl command that proves the API works
3. **Files plan**: every file to create/modify with one-line purpose
4. **Diff/Files**: the code (Required Output Format from MASTER_PLAN.md)
5. **Audit dates**: every model name pinned with the date it was last verified working
6. **Fallback impact**: how the new provider fits into the existing chain (position, role)
7. **Settings UI impact**: what the user sees in Settings (or "no UI change" if pre-existing)
8. **Verification plan**: smoke test + unit tests + manual checks

## Output Templates

See `assets/` for:
- `provider_skeleton.kt.template` — boilerplate for a new VisionProvider/TextProvider
- `fallback_chain_test_cases.kt.template` — the 5 canonical test cases every gateway must pass
- `settings_row_template.kt` — Compose row pattern for the Settings screen

## References

- `references/openai-compatible-protocol.md` — Groq, OpenRouter, DeepSeek, Together share this shape
- `references/google-generate-content.md` — Gemini's idiosyncratic `models/{id}:generateContent` path
- `references/anthropic-messages.md` — Claude's distinct envelope
- `references/cloudflare-workers-ai.md` — CF's `/accounts/{id}/ai/run/@cf/{model}` shape
- `references/error-translation-matrix.md` — HTTP code → ProviderRecoverable/FatalError mapping

## Anti-Patterns (Refuse)

| Anti-pattern | Refusal phrase |
|---|---|
| "Just add this one provider, no need for an abstraction" | "Single-provider designs broke us at MT-014. I'll add it as a `VisionProvider` instance — same file count, deprecation-resilient." |
| "Catch the HTTPException and retry inside the provider" | "Retry is the chain executor's job, not the provider's. The provider translates to a typed error and returns." |
| "Hardcode the API key for testing" | "BuildConfig field only. The test reads from local.properties just like production." |
| "Use Map<String, Any> for the chat content list" | "Sealed `Content { Text, ImageUrl }` interface preserves type safety and matches the OpenAI-compatible vision shape." |
| "Catch on `e.message.contains('404')`" | "Translate to `ProviderRecoverableError.NotFound` so the chain branches on the sealed type, not on string substring." |
| "Add the new provider to GeminiClient as a fallback method" | "That couples the providers. Add a new sibling provider and let `FallbackChain` orchestrate." |

## How To Handle Ambiguity

If the user request is unclear:

- **Unknown auth shape**: ask which header / query param the provider uses. Do not guess.
- **Unknown model name**: search the provider's release notes for free-tier models. Document the audit date in the model constant comment.
- **Unknown fallback order**: ask the user, defaulting to fastest-first (Groq → Gemini → OpenRouter → Cloudflare → HF based on latency observations).

Never silently invent a provider endpoint URL. Use only URLs that the user provided, that appeared in tool output, or that are well-known canonical addresses for the provider.

## Cross-References

- Master orchestrator: `skills/mawaai-master-orchestrator/SKILL.md`
- Individual provider integration: `skills/mobile-ai-api-integrator/SKILL.md`
- Pre-release audit: `skills/production-readiness-auditor/SKILL.md`
- Strict JSON contracts (for response parsing): `skills/prompt-system-architect/SKILL.md`
