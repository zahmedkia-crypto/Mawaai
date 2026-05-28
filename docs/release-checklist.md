# Release Checklist

Go / no-go gate for tagging a Mawaai release. Walk top-to-bottom; do not skip steps.

Last reviewed: 2026-05-28.

---

## 0 — Pre-flight (any time before a planned release)

- [ ] Every PR in the open stack is merged or explicitly deferred. Check `gh pr list` against the queue in `docs/follow-ups.md`.
- [ ] `master` builds cleanly on a fresh checkout: `./gradlew clean :app:assembleDebug`.
- [ ] Unit tests green: `./gradlew test`.
- [ ] Lint green: `./gradlew :app:lintDebug`.

If any of the above fails, do not start the release.

---

## 1 — Database safety net

- [ ] If a `@Database(version = …)` bump landed in this cycle, the matching `app/schemas/<n>.json` is committed.
  - Verify with: `scripts/verify-room-schemas.sh`
  - The CI `schema-guard` job (PR #13) catches this on every PR, but double-check here in case CI was offline.
- [ ] Instrumented `MigrationTest` passes on an API 35 emulator:
  ```bash
  ./gradlew :app:connectedDebugAndroidTest --tests com.mawaai.love.app.data.database.MigrationTest
  ```

---

## 2 — Key hygiene (every release)

Walk through `docs/security-runbook.md` § 2 and confirm restrictions are in place at each provider's console:

- [ ] **Gemini** — Android-apps restriction: package `com.mawaai.love.app` + release SHA-1.
- [ ] **HuggingFace** — fine-grained token scoped to `briaai/RMBG-1.4`, `lllyasviel/sd-controlnet-canny`, `ai-forever/Real-ESRGAN`. Rotated within the last 90 days.
- [ ] **Cloudflare** — token scope: `Account.Workers AI:Read` only. Account-restricted.
- [ ] **Remove.bg** — PAYG cap set. Verify remaining balance is non-zero:
  ```bash
  scripts/api-health.sh --only removebg
  ```
- [ ] **OpenRouter** — allowed-origins includes `https://mawaai.love`. Spending cap set.
- [ ] **Groq** — rotated within the last 90 days.
- [ ] **Firebase** — package + SHA-1 restriction at https://console.cloud.google.com/apis/credentials. API restriction enables only the services in use.

---

## 3 — Live API smoke test

- [ ] Run the smoke test and confirm every configured provider returns 2xx:
  ```bash
  scripts/api-health.sh
  ```

Configured providers that 4xx are usually an auth / quota issue — fix at the dashboard, do not ship around them.

---

## 4 — On-device QA on an Android 15 (API 35) device

This is the hardware-gated section. Use a real Pixel if available; otherwise the API 35 Pixel emulator profile.

- [ ] **Cold start**: app reaches the home screen within 2 s on a mid-range device.
- [ ] **Splash → intro → home** transitions render edge-to-edge with no status-bar overlap and a transparent navigation bar.
- [ ] **Predictive back gesture** animates correctly (MT-016).
- [ ] **Photo picker**:
  - [ ] First tap shows the system "Allow access to all photos / Select photos / Don't allow" sheet (Android 15 partial-photo UX, MT-024).
  - [ ] Selecting "Select photos" and granting 3 photos still loads them.
  - [ ] No UI assumes the full library is reachable.
- [ ] **Biometric lock** (if `profile.biometricEnabled`): prompt shows; cancel → app finishes; auth → app proceeds.
- [ ] **Design flow**: sketch → AI processing → template → result → save → share. No crash; no obvious quality regression on a known-good test sketch.
- [ ] **Cutout**: trigger BG-removal with `REMOVE_BG_API_KEY` set, observe the `RemoveBgClient` pre-flight log line in logcat showing the credit balance.
- [ ] **Gemini fallback**: temporarily blank `GEMINI_API_KEY` in `local.properties`, rebuild, confirm Inspiration prompts still appear (served by OpenRouter via MT-012).

---

## 5 — Native ABI compatibility (Android 15 hardware)

- [ ] 16 KB-page page-alignment check on the release artifact:
  ```bash
  ./gradlew :app:bundleRelease
  scripts/check-16kb-alignment.sh app/build/outputs/bundle/release/app-release.aab
  ```
  - If OpenCV reports 4 KB-only: bump to OpenCV 4.10+ (MT-025) before tagging.

---

## 6 — Release artifacts

- [ ] `versionName` and `versionCode` in `app/build.gradle.kts` bumped (semver + monotonic increment).
- [ ] Release signing config populated in `local.properties`:
  - `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.
- [ ] Build and sign:
  ```bash
  ./gradlew :app:bundleRelease
  ```
- [ ] AAB lives at `app/build/outputs/bundle/release/app-release.aab` and is signed with the release keystore (not debug).

---

## 7 — Play Console upload

- [ ] Upload AAB to the closed testing track first.
- [ ] Confirm `targetSdk = 35` shows in the Play Console "App content" tab (required since Aug 2025; MT-016).
- [ ] Trigger the Play "Pre-launch report" — review crashes, ANRs, security warnings.
- [ ] Promote to internal → closed → open testing as the test signal arrives.

---

## 8 — Post-release

- [ ] Tag the merge commit: `git tag release-vN.N.N && git push origin release-vN.N.N`.
- [ ] Add a short release note to `docs/releases/` (create the directory on first release; one Markdown file per version).
- [ ] If any key restriction was tightened in step 2, set a 24-hour calendar reminder to revoke the old key once the rollout is complete (per `docs/security-runbook.md` § 4).

---

## Aborting a release

If any checkbox in § 1–§ 5 fails:

1. Stop. Do not work around it.
2. File the issue with the failing checkbox quoted in the title.
3. Address the issue in a fresh PR off `master`.
4. Restart this checklist from § 0.

This list will grow as the team discovers new release-time gotchas; append to the relevant section rather than burying them in a PR description.
