# Security Runbook

Last updated: **MT-007** (2026-05-28).

This is the operational guide for managing Mawaai's API keys, Firebase configuration, and key-vault migration. The codebase introduces a `KeyVault` abstraction (`app/src/main/java/com/mawaai/love/app/core/security/KeyVault.kt`) — but the highest-leverage hardening lives at the **provider console**, not in the codebase. Read this file end-to-end before the next release.

---

## 1. What is — and is not — a secret

| Artifact | Secret? | Why |
|---|---|---|
| `local.properties` | **YES** — never commit. | Holds raw bearer tokens (`GEMINI_API_KEY`, `HUGGINGFACE_API_KEY`, `REMOVE_BG_API_KEY`, `OPENROUTER_API_KEY`, `GROQ_API_KEY`, `CLOUDFLARE_API_TOKEN`, `PEXELS_API_KEY`). Already in `.gitignore` (verified). |
| `BuildConfig.*_API_KEY` fields | **Treat as compromised on every release.** | The values get baked into the released APK as string constants. `apktool` and `strings` extract them in seconds. The hardening here is **never** "hide them better in the APK" — it is **always** "restrict what they can do at the provider console". |
| `app/google-services.json` | **NOT** a secret per Firebase docs, but only when the Firebase project is properly restricted. | The Firebase Web API key is restricted by package name + SHA-1 fingerprint at the Google Cloud console. If restrictions are configured, an extracted key is useless on any other app. **See Firebase section below.** |
| `app/schemas/*.json` | Not secret. | Schema definitions only. |
| `*.kt`, `*.xml`, `*.gradle.kts` | Not secret. | Source. |

---

## 2. Per-provider console restrictions (highest priority)

For **every** key, apply restrictions at the provider's dashboard so an extracted key cannot be re-used on a different package, IP, or origin. Restriction is what actually protects you — not encryption.

### Google Gemini (`GEMINI_API_KEY`)

1. https://aistudio.google.com → API keys → select the Mawaai key.
2. **Application restrictions** → "Android apps" → add:
   - Package name: `com.mawaai.love.app`
   - SHA-1 fingerprint: your release-signing SHA-1 (`./gradlew :app:signingReport`).
3. **API restrictions** → restrict to "Generative Language API" only.
4. Verify the same key fails on a different package / debug SHA.

### Hugging Face (`HUGGINGFACE_API_KEY`)

Hugging Face tokens cannot be Android-package-restricted. Mitigations:

1. Use a **fine-grained token** (https://huggingface.co/settings/tokens?type=fine_grained) scoped to **only** the specific models we call: `briaai/RMBG-1.4`, `lllyasviel/sd-controlnet-canny`, `ai-forever/Real-ESRGAN`.
2. Set a per-token rate limit (free tier inherits account-level limits; pay tier lets you cap).
3. **Rotate quarterly** — set a calendar reminder. There's a follow-up to add a runtime-fetched token from a server endpoint (deferred until a backend exists).

### Cloudflare Workers AI (`CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_API_TOKEN`)

1. https://dash.cloudflare.com → My Profile → API Tokens.
2. **Create token** → custom token with **only** the permission `Account.Workers AI:Read`.
3. **Account resources** → restrict to the single account that holds the Worker (no all-accounts).
4. **IP filtering** → leave unrestricted (mobile users hit from arbitrary IPs).
5. **Client IP TTL** → 90 days, rotate then.

The `CLOUDFLARE_ACCOUNT_ID` itself is not secret. Only the token is.

### Remove.bg (`REMOVE_BG_API_KEY`)

Remove.bg does not support per-app restrictions. Mitigations:

1. Use a separate key for Mawaai (one key per app); revoke if extracted.
2. The **MT-011** pre-flight already short-circuits calls when quota is zero.
3. Set a `request_referer` check from the dashboard if available — mobile apps cannot send one reliably, so leave unchecked but note in the dashboard.
4. **Top up to a known credit cap**, not unlimited PAYG, so a leaked key can do at most `<cap>` worth of damage.

### OpenRouter (`OPENROUTER_API_KEY`)

1. https://openrouter.ai/settings/keys → select the Mawaai key.
2. **Allowed origins** → `https://mawaai.love` (matches the `HTTP-Referer` header `OpenRouterClient` already sends).
3. **Spending limit** → cap at $5/month while in development, $50/month for production. Alerts at 50% / 80% / 100%.

### Groq (`GROQ_API_KEY`)

Groq doesn't expose Android-package restrictions today (2026-05). Mitigations:

1. One key per app.
2. **Rate limits** are managed at the account level — use the lowest tier that fits.
3. **Rotate quarterly.**

### Pexels (`PEXELS_API_KEY`)

Currently unused (see PEXELS-001). Remove the `buildConfigField` in `app/build.gradle.kts` if not restoring the feature; otherwise restrict to `https://mawaai.love` at https://www.pexels.com/api/.

### Firebase (`google-services.json`)

1. https://console.cloud.google.com → APIs & Services → Credentials.
2. For each auto-created Firebase Web API key, set **Application restrictions** → Android apps with package + SHA-1 (release **and** debug).
3. **API restrictions** → enable only the Firebase services you actually use (today: Firebase Installations + Crashlytics if wired). Disable Maps, Places, etc.
4. Verify with `curl` to one of the restricted endpoints from an unauthorised SHA → expect HTTP 403.

---

## 3. The `KeyVault` migration plan (MT-007 → MT-027)

### What MT-007 ships

- `KeyVault` interface + `EncryptedKeyVault` impl backed by `EncryptedSharedPreferences` (AES-256-GCM, master key in Android Keystore).
- Hilt `SecurityModule` binding.
- Instrumented test suite covering put/get/clear and Keystore round-trip.

### What MT-007 does **not** do

- It does **not** refactor existing clients (`GeminiClient`, `OpenRouterClient`, `HuggingFaceClient`, `RemoveBgClient`, `CloudflareWorkersAiClient`, `GroqClient`) off direct `BuildConfig.*` reads. The seam is in place; the migration is incremental.

### How to migrate a single client (template)

When migrating `GeminiClient` (example) under **MT-027**:

```kotlin
// Before
@Singleton
class GeminiClient @Inject constructor(
    private val api: GeminiApi,
    private val openRouterClient: OpenRouterClient,
) {
    val isConfigured: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    suspend fun inspirationPrompts(count: Int): List<String> {
        val key = BuildConfig.GEMINI_API_KEY
        // …
    }
}

// After
@Singleton
class GeminiClient @Inject constructor(
    private val api: GeminiApi,
    private val openRouterClient: OpenRouterClient,
    private val keyVault: KeyVault,
) {
    val isConfigured: Boolean get() = keyVault.get(ApiKeyId.GEMINI).isNotBlank()

    suspend fun inspirationPrompts(count: Int): List<String> {
        val key = keyVault.get(ApiKeyId.GEMINI)
        // …
    }
}
```

That's the entire change per client. The fallback to `BuildConfig.GEMINI_API_KEY` continues to work transparently — `KeyVault.get` returns the override if a Settings UI has written one, else the compiled-in value.

### Future Settings UI

A "Bring your own key" Settings screen can call `keyVault.put(ApiKeyId.GEMINI, userEnteredValue)` after pasting from the user's Gemini dashboard. The override survives process death and is invisible to anyone without root + ADB + the Keystore.

### Future server-handed-out keys

When the backend exists, swap the `SecurityModule` binding for an implementation that:

1. On first launch: calls `POST /v1/install` with the app installation id.
2. Receives a JSON blob of per-key tokens.
3. Calls `keyVault.put(...)` for each.
4. Schedules a `WorkManager` periodic refresh.

This change touches **only** `SecurityModule` + the new initialiser — every client is already on `KeyVault`.

---

## 4. Key rotation

Per the API-key restrictions above, rotation cadences:

| Key | Cadence | Owner |
|---|---|---|
| `GEMINI_API_KEY` | On compromise only (package+SHA restriction makes leakage low-impact). | Maintainer |
| `HUGGINGFACE_API_KEY` | Quarterly. | Maintainer |
| `CLOUDFLARE_API_TOKEN` | 90 days. | Maintainer |
| `REMOVE_BG_API_KEY` | On compromise; otherwise yearly. Set top-up cap as the real protection. | Maintainer |
| `OPENROUTER_API_KEY` | On compromise. Spending cap is the protection. | Maintainer |
| `GROQ_API_KEY` | Quarterly. | Maintainer |
| Firebase Web API key | Never — it's restricted by package+SHA. Revoke + recreate if the restrictions ever become misconfigured. | Maintainer |

### Rotation procedure

1. Create the new key at the provider console **with the same restrictions** as the old one.
2. Update `local.properties` on every developer machine. (Consider distributing via 1Password / Bitwarden Send.)
3. Run `./gradlew :app:assembleDebug` and `./gradlew :app:assembleRelease`. Verify each call site succeeds.
4. **Wait 24 h** — both keys remain valid during this window so a release in flight does not break.
5. Revoke the old key at the provider console.
6. Tag the release that "owns" the new key (`git tag rotation-2026-Q2`) so any future incident-response can identify when the change shipped.

---

## 5. Incident response — extracted key

If an attacker extracts a key from a leaked APK or a public fork:

1. **Revoke** at the provider console **first**. Restrictions limit damage, but revocation stops it.
2. Audit the provider's usage log for the past 24 h. Look for unusual model selection, large request volumes, or non-Mawaai user agents.
3. Generate a fresh key with the **same restrictions**.
4. Patch `local.properties`. Ship a hotfix build to Play / TestFlight.
5. File a post-mortem in `docs/postmortems/<YYYY-MM-DD>-key-leak.md`.

The fact that we're going to leak keys is a planning assumption, not a failure mode — design every key so a leak is annoying, not catastrophic.

---

## 6. Verifying `.gitignore` hygiene

These files **must** be in `.gitignore` (verified for this PR):

- `local.properties` ✓
- `/local.properties` ✓
- `*.iml`, `.gradle`, `/build`, `.cxx`, `.externalNativeBuild` ✓

These IDE / AI-tool directories are currently **committed** and should be removed in **MT-020**:

- `.aiassistant`, `.cursor`, `.devin`, `.idea`, `.vscode`, `.cursorrules`

That's hygiene, not security — none of those folders contain raw keys today. But they leak the team's tooling choices and editor history into the repo, which is its own avoidable disclosure.

---

## 7. Further reading

- [Android Keystore](https://developer.android.com/training/articles/keystore)
- [EncryptedSharedPreferences](https://developer.android.com/topic/security/data) (deprecation note: the library is in maintenance, but still the de-facto standard for Android API ≤ 35 secret persistence. A future migration to JetPack DataStore + Tink or to platform-native [`CredentialManager`] is tracked as **MT-028**.)
- [Google Cloud API restrictions](https://cloud.google.com/docs/authentication/api-keys#api_key_restrictions)
- [Firebase security checklist](https://firebase.google.com/support/guides/security-checklist)
