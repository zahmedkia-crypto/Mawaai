# ADR-0001 — KeyVault storage via EncryptedSharedPreferences

- Status: Accepted
- Date: 2026-05-28
- Tags: security, persistence
- PR: #11

## Context

API keys for Gemini, HuggingFace, Cloudflare Workers AI, Remove.bg,
OpenRouter, Groq, and Pexels were read directly from
`BuildConfig.<NAME>_API_KEY` at every call site. The values are
populated from `local.properties` at build time. This had three problems:

1. **Compiled-in keys are extractable** from the released APK in seconds
   via `apktool` + `strings`. Anyone can pull them, and the only thing
   stopping abuse is the per-provider console restriction (which had not
   been audited).
2. **No future override path**. A "BYO key" setting in the app, or
   server-handed-out per-install tokens, would each require touching
   every client.
3. **No defence in depth**. A `SharedPreferences` cache of any key
   (which the Settings UI might write later) would land in plaintext
   under `/data/data/<pkg>/shared_prefs/`. On a rooted device, trivially
   readable.

## Decision

Adopt a `KeyVault` interface backed by `EncryptedKeyVault` using
`androidx.security:security-crypto:1.1.0-alpha06`. Master key is
`AES256_GCM` in the Android Keystore; per-entry values are `AES256_GCM`
ciphertext. `KeyVault.get(ApiKeyId.X)` returns the runtime override when
present, otherwise the compiled-in `BuildConfig.X_API_KEY` value.
Backwards compatibility preserved.

## Consequences

- ✅ Single typed source of truth (`ApiKeyId` enum is exhaustive).
- ✅ Defence-in-depth for any future cached override.
- ✅ Tests can swap `KeyVault` via Hilt `@TestInstallIn`.
- ⚠️ `androidx.security:security-crypto` is in maintenance mode.
  Long-term replacement tracked as MT-028 (Tink + DataStore or platform
  `CredentialManager`).
- ⚠️ First-use construction blocks 50–150 ms while the Keystore
  provisions the master key. Mitigated by `@Singleton` scope.
- ❌ Mere extraction of the `BuildConfig` string is still possible from
  the released APK. Accepted: the actual defence lives at the provider
  console (`docs/security-runbook.md` § 2). `SECURITY.md` makes this
  explicit to outside researchers.
