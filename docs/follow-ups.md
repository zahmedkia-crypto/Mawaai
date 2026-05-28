# Follow-ups

Single source of truth for every action that must happen **after** the 12 PRs in this stack start merging. Each item is ordered to its blocker so you can work through this top-to-bottom without thinking.

Last updated: 2026-05-28.

---

## After **PR #6 (MT-021 portable JDK)** merges

Nothing to do. Linux/macOS developers and CI can now build for the first time.

---

## After **PR #7 (P0-A/B Room migration)** merges

Run the regen once on a developer machine so the auto-generated `6.json` lands on `master`:

```bash
git fetch origin master && git checkout master && git pull
scripts/verify-room-schemas.sh
# That command runs `./gradlew :app:kspDebugKotlin` and tells you whether
# `app/schemas/<...>/6.json` needs to be committed. If so:
git add app/schemas/
git commit -m "db: commit auto-generated Room schema v6 (post-PR #7)"
git push origin master
```

After this, CI's `schema-guard` job (from PR #13) keeps schemas honest forever.

---

## After **PR #11 (MT-007 KeyVault)** merges

### 1. Apply provider-console restrictions (highest leverage)

Open `docs/security-runbook.md` § 2 and walk through each provider's console:

- Gemini: package + SHA-1 restriction at https://aistudio.google.com.
- HuggingFace: fine-grained token scoped to `briaai/RMBG-1.4`, `lllyasviel/sd-controlnet-canny`, `ai-forever/Real-ESRGAN`.
- Cloudflare: token with only `Account.Workers AI:Read`.
- Remove.bg: cap PAYG credits so a leaked key is bounded.
- OpenRouter: spending limit + allowed-origins.
- Groq: rotate.
- Firebase: package + SHA-1 + API restriction in Google Cloud console.

This step is what actually protects you. The encryption inside the APK is defence in depth.

### 2. Open **PEXELS-001**

The Pexels API key is wired into `BuildConfig` + the `ApiKeyId` enum but no `data/remote/pexels` package exists in source. Remove the dead references:

#### Patch 1 — `app/build.gradle.kts`

```diff
-        buildConfigField("String", "PEXELS_API_KEY", "\"${localProps.getProperty("PEXELS_API_KEY") ?: ""}\"")
```

#### Patch 2 — `app/src/main/java/com/mawaai/love/app/core/security/KeyVault.kt`

```diff
 enum class ApiKeyId {
     GEMINI,
     OPENROUTER,
     GROQ,
     CLOUDFLARE_ACCOUNT_ID,
     CLOUDFLARE_API_TOKEN,
     HUGGINGFACE,
     REMOVE_BG,
-    PEXELS,
     ;

     internal fun buildConfigFallback(): String = when (this) {
         GEMINI                -> BuildConfig.GEMINI_API_KEY
         OPENROUTER            -> BuildConfig.OPENROUTER_API_KEY
         GROQ                  -> BuildConfig.GROQ_API_KEY
         CLOUDFLARE_ACCOUNT_ID -> BuildConfig.CLOUDFLARE_ACCOUNT_ID
         CLOUDFLARE_API_TOKEN  -> BuildConfig.CLOUDFLARE_API_TOKEN
         HUGGINGFACE           -> BuildConfig.HUGGINGFACE_API_KEY
         REMOVE_BG             -> BuildConfig.REMOVE_BG_API_KEY
-        PEXELS                -> BuildConfig.PEXELS_API_KEY
     }
 }
```

#### Patch 3 — `docs/security-runbook.md`

Delete the `### Pexels (PEXELS_API_KEY)` subsection from § 2.

Ship as a small PR titled `chore(security): remove dead PEXELS_API_KEY (PEXELS-001)`.

### 3. Open **MT-027** (migrate every client to KeyVault)

Run the audit:

```bash
scripts/audit-buildconfig-keys.sh
```

That lists every `BuildConfig.<key>` read in source. For each, apply the template from `docs/security-runbook.md` § 3:

```diff
 @Singleton
 class GeminiClient @Inject constructor(
     private val api: GeminiApi,
     private val openRouterClient: OpenRouterClient,
+    private val keyVault: KeyVault,
 ) {

-    val isConfigured: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()
+    val isConfigured: Boolean get() = keyVault.get(ApiKeyId.GEMINI).isNotBlank()

     suspend fun inspirationPrompts(count: Int = 5): List<String> {
-        val key = BuildConfig.GEMINI_API_KEY
+        val key = keyVault.get(ApiKeyId.GEMINI)
         …
     }
 }
```

Files to touch:

- `app/src/main/java/com/mawaai/love/app/design/ai/gemini/GeminiClient.kt`
- `app/src/main/java/com/mawaai/love/app/design/ai/openrouter/OpenRouterClient.kt`
- `app/src/main/java/com/mawaai/love/app/design/ai/groq/GroqTextProvider.kt` (and `GroqVisionProvider.kt` if it exists)
- `app/src/main/java/com/mawaai/love/app/design/ai/cloudflare/CloudflareWorkersAiClient.kt`
- `app/src/main/java/com/mawaai/love/app/design/ai/huggingface/HuggingFaceClient.kt`
- `app/src/main/java/com/mawaai/love/app/design/ai/removebg/RemoveBgClient.kt`

Ship as one PR titled `refactor(ai): migrate every client off direct BuildConfig reads (MT-027)`. Mechanical and reviewable.

---

## After **PR #10 (MT-016 targetSdk 35)** + **PR #12 (MT-015 version bumps)** both merge

Open a small bump PR to pull `core-ktx` to 1.15.0 (currently held back because it requires `compileSdk = 35`):

```diff
-coreKtx = "1.13.1"
+coreKtx = "1.15.0"
```

Then on the next build, walk through any Compose / lifecycle deprecation warnings introduced by the bump and clean them up. Once the warning count is zero, the `lint { disable += "StateFlowValueCalledInComposition" }` workaround in `app/build.gradle.kts` can be removed.

---

## After **PR #13 (MT-009 CI workflow)** merges

Install the workflow itself — the integration token used for PR #13 lacks GitHub's `workflow` scope, so the YAML committed to `docs/ci-workflow.yml.template`. Land it locally with a PAT that has `workflow` scope:

```bash
mkdir -p .github/workflows
cp docs/ci-workflow.yml.template .github/workflows/ci.yml
git add .github/workflows/ci.yml
git commit -m "ci: install workflow from docs template (MT-009 part 2)"
git push origin master
```

Then verify on the next PR that all three jobs (`build-and-test`, `schema-guard`, opt-in `instrumented-tests`) appear.

---

## After **PR #15 (MT-020/003 hygiene)** merges

Untrack the IDE / AI config directories that `.gitignore` now blocks but that are still in the git index:

```bash
scripts/untrack-ide-configs.sh           # dry-run; confirm the list
scripts/untrack-ide-configs.sh --apply
git commit -m "chore: untrack IDE/AI config dirs (MT-020 part 2)"
git push origin master
```

Workspaces are not deleted from anyone's local filesystem — `--cached` only removes them from the index.

---

## Hardware-gated items (any time, requires Android 15 emulator or device)

### MT-024 — partial-photo-access UX audit

1. Install the latest `master` on an Android 15 (API 35) emulator.
2. Trigger any feature that reads photos (memory creation, design upload).
3. Confirm the system shows "Allow access to all photos / Select photos / Don't allow".
4. Select **Select photos**, grant access to 3 photos.
5. Reload the photo picker — confirm the chosen 3 are shown.
6. If any UI assumes the full library is available (e.g., `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` queries returning extra results), file each failure as a sub-issue under MT-024.

### MT-025 — 16 KB native page alignment

```bash
./gradlew :app:bundleRelease
scripts/check-16kb-alignment.sh app/build/outputs/bundle/release/app-release.aab
```

If any `.so` is reported as 4 KB-aligned only:

- For OpenCV → bump to 4.10 (released 2024-09 with 16 KB support).
- For ML Kit `subject-segmentation` → no public 16 KB-aligned beta as of 2026-05; open a bug with Google and document the device exclusion in `docs/android-15-checklist.md` § 3 until a fix ships.

---

## Strategic / not urgent

- **MT-028** — eventually migrate off `androidx.security-crypto` (maintenance mode) to Tink + DataStore or `CredentialManager`. Open-ended; no concrete trigger required.
- **MT-029** — once every client is on `KeyVault.get(...)` (MT-027), decide whether to remove the `BuildConfig.*_API_KEY` fields entirely and rely on a first-launch server-handed-out flow. Requires a backend that does not yet exist; safe to defer indefinitely.

---

## Re-running this index

Generated by the agent in PR #17. If new follow-ups appear later, append to this file rather than burying them in a PR description.
