# Mawaai API Health Report — 2026-05-22

**Audit scope:** MT-011 (live smoke test of all wired AI endpoints + the optional
OpenRouter fallback the user mentioned). Run via the `mawaai-master-orchestrator`
delegating to `production-readiness-auditor` + `mobile-ai-api-integrator`.

**Test methodology:** parallel HTTP probes against each provider's lightest billable
endpoint (text gen for LLMs, account info for Remove.bg, model metadata for HF).
Keys held in-memory only — never written to disk, never committed.

## Summary

| Provider | Key Valid | Endpoint Reachable | Live Generation | Latency | Notes |
|---|:---:|:---:|:---:|---:|---|
| Gemini (Google) | ✅ | ✅ | ⚠️ 429 | n/a | Free-tier daily/minute quota exceeded. Key itself authenticates against `ListModels`. |
| HuggingFace | ✅ | ⚠️ partial | not measured here | n/a | Auth accepted by `huggingface.co/api`. Run smoke test on-device to verify `api-inference.huggingface.co` reachability — sandbox DNS could not resolve that subdomain. |
| Cloudflare Workers AI | ✅ | ✅ | ✅ PASS | 323 ms | `@cf/meta/llama-3.1-8b-instruct` returned valid completion. Healthiest provider. |
| Remove.bg | ✅ | ✅ | ✅ PASS (account) | 251 ms | ⚠️ **Account has only 1 PAYG credit remaining** — top up before BG-removal feature rollout. |
| OpenRouter (not yet wired in app) | ✅ | ✅ | ✅ PASS | 1099 ms | `openrouter/auto` resolved to `google/gemini-2.5-flash-lite`. Potential fallback for Gemini when quota-throttled. |

## Stable Model Names (verified against Gemini ListModels)

For `data/remote/gemini` clients, the supported stable flash models as of 2026-05-22
are (drop the legacy `-latest` suffix — it 404s):

- `gemini-2.5-flash`
- `gemini-2.0-flash`
- `gemini-2.0-flash-001`
- `gemini-2.0-flash-lite`
- `gemini-2.0-flash-lite-001`

Action: audit the `GeminiClient` and any constants referencing
`gemini-1.5-flash-latest` or `gemini-pro-vision` — replace with the names above.

## P1 Actions Recommended

1. **Remove.bg credits** — top up before any user-facing rollout. With 1 credit
   remaining, the second background-removal request in production will fail. Either:
   - Top up the PAYG balance, or
   - Promote the on-device U2Net / RMBG-1.4 path as primary and keep Remove.bg as
     manual fallback.

2. **Gemini quota** — the free tier is exceeded. Options:
   - Enable billing on the Google AI project.
   - Wire OpenRouter as transparent fallback (`openrouter/auto` automatically routes
     to a working Gemini variant when the direct API fails).

3. **Audit Gemini model name constants** in source — confirm none reference the
   deprecated `-latest` suffix.

## P2 Actions

4. **HuggingFace inference subdomain** — run the on-device smoke test (committed in
   `app/src/test/.../ApiHealthSmokeTest.kt`) to verify `api-inference.huggingface.co`
   is reachable from end-user devices. The sandbox DNS issue is environment-specific
   and does not necessarily reflect prod behavior.

5. **Wire OpenRouter as fallback** — add a `BuildConfig.OPENROUTER_API_KEY` field and
   an `OpenRouterClient` mirroring the Gemini interface. Auto-route when Gemini
   returns 429 or 503. Specialist: `mobile-ai-api-integrator`. Estimated micro-task
   size: 2 files, <100 LOC.

## Smoke Test Harness

This commit also adds `app/src/test/java/com/mawaai/love/app/ApiHealthSmokeTest.kt`.
It is **opt-in** via env var `MAWAAI_RUN_LIVE_API_TESTS=1` so it never runs in CI by
default and never burns quota unintentionally. Devs run it manually:

```bash
MAWAAI_RUN_LIVE_API_TESTS=1 ./gradlew :app:test --tests \
    com.mawaai.love.app.ApiHealthSmokeTest
```

Each test uses the existing `BuildConfig.*` fields, which are populated from
`local.properties` — so no key material is added to source or committed anywhere.

## Hard Rules Honored

- No keys committed (this file contains only PASS/FAIL/latency, no secrets).
- No source code touched except the new `ApiHealthSmokeTest.kt` (additive only).
- Test is opt-in to prevent accidental quota burn.
- Layering preserved: smoke test reads `BuildConfig`, hits HTTPS endpoints, asserts
  status. No DI graph modification, no production class touched.

## Next Micro-Task

**MT-012 (recommended):** Wire OpenRouter as a transparent Gemini fallback.
- Add `OPENROUTER_API_KEY` to `local.properties` and `BuildConfig`.
- Add `OpenRouterClient` mirroring `GeminiClient`'s text interface.
- In `GeminiClient`, catch 429/503 and delegate to `OpenRouterClient` when
  the key is present.
- Specialist: `mobile-ai-api-integrator`.
