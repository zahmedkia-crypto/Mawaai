# ADR-0002 — OpenRouter as transparent Gemini fallback

- Status: Accepted
- Date: 2026-05-28
- Tags: ai, reliability
- PR: #8

## Context

`GeminiClient.inspirationPrompts(count)` returned `emptyList()` on any
failure. With Gemini's free-tier quota throttled (per `API_HEALTH_2026-05-22.md`),
the Inspiration panel silently showed nothing. OpenRouter was wired but
behind a `StubTextProvider` ("wired in MT-019").

The gateway abstraction (`AiProvider`, `FallbackChain`, `ProviderRegistry`)
already supported typed fallback. But `GeminiClient.inspirationPrompts` is
called *directly*, not through the chain. Refactoring every caller is
MT-027 — large and mechanical. The user-visible bug needs fixing now.

## Decision

Two complementary changes in PR #8:

1. **Gateway adapter** — `OpenRouterTextProvider` replaces the stub.
   Any caller via `ProviderRegistry.activeTextChain()` picks OpenRouter
   up automatically.
2. **Direct fallback in GeminiClient** — constructor-injects
   `OpenRouterClient`; on any failure (exception OR empty response),
   delegates to `openRouterClient.inspirationPrompts(count)`.

Supporting: `OpenRouterClient.chatCompletion(prompt, systemPrompt,
model)` with typed-error mapping (`401 → InvalidKey`, `429 →
RateLimited`, etc.) so the gateway adapter and direct fallback share
plumbing.

## Consequences

- ✅ Gemini quota exhaustion invisible to the user.
- ✅ Gateway chain has one more real provider.
- ✅ Typed error mapping reusable for the future `GeminiTextProvider`.
- ⚠️ Hilt edge `GeminiClient → OpenRouterClient`. Verified no cycle.
- ⚠️ Cost shifts to OpenRouter on Gemini throttle. Spending cap set
  per `docs/security-runbook.md` § 2.5.
- ❌ Does not migrate the broader codebase to the chain. MT-027 will.
