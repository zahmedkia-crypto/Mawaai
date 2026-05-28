package com.mawaai.love.app.core.security

import com.mawaai.love.app.BuildConfig

/**
 * Strongly-typed identifier for every API key the app reads. Each id has a
 * fallback to a corresponding [BuildConfig] field — keys are *always* readable
 * as long as `local.properties` is populated, regardless of whether a runtime
 * override has been set.
 *
 * Adding a new provider:
 *   1. Append the id below.
 *   2. Map it to the matching `BuildConfig.*` field in [buildConfigFallback].
 *   3. Declare the `buildConfigField` in `app/build.gradle.kts`.
 *   4. Document the console-side restriction in `docs/security-runbook.md`.
 *
 * The id name doubles as the storage key inside [KeyVault] so renames are
 * disruptive — treat the enum constant names as part of the persistent
 * schema.
 */
enum class ApiKeyId {
    GEMINI,
    OPENROUTER,
    GROQ,
    CLOUDFLARE_ACCOUNT_ID,
    CLOUDFLARE_API_TOKEN,
    HUGGINGFACE,
    REMOVE_BG,
    PEXELS,
    ;

    /**
     * The compiled-in fallback value for this id. Read at call time so a
     * rebuild with different `local.properties` is picked up without
     * invalidating the runtime store.
     */
    internal fun buildConfigFallback(): String = when (this) {
        GEMINI                -> BuildConfig.GEMINI_API_KEY
        OPENROUTER            -> BuildConfig.OPENROUTER_API_KEY
        GROQ                  -> BuildConfig.GROQ_API_KEY
        CLOUDFLARE_ACCOUNT_ID -> BuildConfig.CLOUDFLARE_ACCOUNT_ID
        CLOUDFLARE_API_TOKEN  -> BuildConfig.CLOUDFLARE_API_TOKEN
        HUGGINGFACE           -> BuildConfig.HUGGINGFACE_API_KEY
        REMOVE_BG             -> BuildConfig.REMOVE_BG_API_KEY
        PEXELS                -> BuildConfig.PEXELS_API_KEY
    }
}

/**
 * Single read/write surface for every API key the app uses.
 *
 * MT-007 (2026-05-28) introduces this seam so:
 *   - Future flows (server-handed-out keys, in-app key entry, MDM-provisioned
 *     keys) can replace the source without touching every client.
 *   - Keys are not held in plain `SharedPreferences` once a caller writes one;
 *     [EncryptedKeyVault] persists via `EncryptedSharedPreferences` backed by
 *     the Android Keystore.
 *   - `BuildConfig` values remain a fallback so existing clients keep working
 *     without an immediate migration. Migration of each client is tracked
 *     under MT-027.
 *
 * Contract:
 *   - [get] returns the override when present and non-blank, otherwise the
 *     compiled-in [ApiKeyId.buildConfigFallback]. Returns "" if both are
 *     blank — callers MUST treat empty-string the same as "not configured".
 *   - [put] persists the override. Passing `null` or "" clears the override
 *     (next [get] falls back to BuildConfig).
 *   - [isOverridden] is a debugging affordance — useful for the future
 *     Settings → "AI providers" screen.
 *   - All operations are synchronous. Implementations must NOT block on
 *     network or perform key-derivation on the main thread; the production
 *     [EncryptedKeyVault] amortises the master-key load via Hilt
 *     `@Singleton`.
 */
interface KeyVault {
    fun get(id: ApiKeyId): String
    fun put(id: ApiKeyId, value: String?)
    fun isOverridden(id: ApiKeyId): Boolean
    fun clearAll()

    companion object {
        /** Storage-key prefix inside [EncryptedSharedPreferences]. */
        internal const val STORAGE_KEY_PREFIX = "api_key/"
    }
}
