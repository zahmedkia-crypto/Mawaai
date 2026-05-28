# Android 15 (API 35) Compliance Checklist

Last updated: MT-016 (2026-05-28) — `compileSdk` and `targetSdk` raised to **35** to keep the app eligible for Google Play submissions.

Google Play deadlines that motivate this bump:

- **New app submissions** since August 2025 require `targetSdk ≥ 35`.
- **Existing-app updates** require `targetSdk ≥ 35` from August 2026 onwards.

This document tracks what's already addressed by MT-016 and what still needs manual / on-device verification.

## ✅ Already handled in MT-016

| Topic | Status | Where |
|---|---|---|
| `compileSdk = 35` | Done | `app/build.gradle.kts` |
| `targetSdk = 35` | Done | `app/build.gradle.kts` |
| `tools:targetApi` bumped | Done | `AndroidManifest.xml` `<application>` |
| Predictive back gesture | Opt-in declared via `android:enableOnBackInvokedCallback="true"` | `AndroidManifest.xml` `<application>` |
| Edge-to-edge by default | App already calls `enableEdgeToEdge(...)` in `MainActivity.onCreate` with explicit `SystemBarStyle.dark` for status + navigation bars. Required by Android 15 for `targetSdk ≥ 35`. | `MainActivity.kt` |
| Foreground service types | Not applicable — the app has **no** Service classes. `FOREGROUND_SERVICE` permission is declared but unused (left in place for a possible future FGS). All background work uses `WorkManager` without `setForegroundAsync`. | n/a |

## ⚠️ Follow-up items to verify on a real Android 15 device

The bump itself doesn't break anything we've found by static analysis, but the items below ship behavior changes on Android 15 that need an emulator / device QA pass.

### 1. Edge-to-edge and the splash theme

The activity theme is `@style/Theme.Mawaai.Splash`. Compose now provides all chrome via `MawaaiTheme` and `ThemedBackground`, but the splash theme should be reviewed to ensure it doesn't paint an opaque status / navigation bar bg that would clash with the dark `MawaaiColorScheme`. Test:

```bash
./gradlew :app:installDebug
adb shell am start -n com.mawaai.love.app/.MainActivity
```

Visually verify the status bar is transparent during the splash screen.

### 2. Partial photo access (`READ_MEDIA_VISUAL_USER_SELECTED`)

Apps targeting 35+ that request `READ_MEDIA_IMAGES` get the **"Select photos"** UX automatically. The user can grant access to a subset of photos. The selected-photos UI affordance shows next to the photo picker when partial access is granted.

The app currently declares only `READ_MEDIA_IMAGES`. With target 35+, the system will:
- On a fresh install, prompt with "Allow access to all photos" / "Select photos" / "Don't allow".
- Apps using the system Photo Picker (`ActivityResultContracts.PickVisualMedia`) work transparently with any choice.
- Apps using `MediaStore` queries with the legacy `READ_MEDIA_IMAGES` permission will only see the photos the user selected.

**Action**: audit every `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` query and every photo-list UI — confirm they handle the "user selected 3 photos and the rest are invisible" case gracefully. If we never `MediaStore`-query (only use the photo picker), no change needed.

Tracking: **MT-024** (separate micro-task).

### 3. 16 KB page-size compatibility

Android 15 devices may ship with 16 KB memory pages. Native `.so` files compiled for 4 KB-only alignment may fail to load. The project ships:

- **OpenCV 4.9.0** — released before the 16 KB requirement was widely deployed. Run `zipalign -c -p 16 app-release.aab` and check the OpenCV `.so` files. If they fail, bump to OpenCV ≥ 4.10 (which ships 16 KB-aligned binaries) — tracking as **MT-025**.
- **TensorFlow Lite 2.16.1** — already 16 KB-compatible.
- **ML Kit Subject Segmentation 16.0.0-beta1** — verify with the same `zipalign` check.

Validation script:

```bash
unzip -p app-release.aab base/lib/arm64-v8a/libopencv_java4.so | \
    od -A x -t x1z -v | head
# Look at the program-header alignment in the ELF
readelf -lW libopencv_java4.so | grep LOAD
# Each LOAD segment's alignment value must be >= 0x4000 (16 KB)
```

### 4. `PROCESS_TEXT` intent removal and other deprecated features

Audit any custom intents the app declares. Currently the only deep link is `mawaai://memory/...` which is unaffected.

### 5. `JobScheduler` quotas

Apps targeting 35+ get tighter `JobScheduler` quotas. The app uses `WorkManager`, which wraps `JobScheduler` since API 23. Periodic `DailyQuoteWorker` runs once per day — well within any quota. No action needed.

### 6. Notification trampolines

Apps targeting 35+ cannot use notification "trampolines" (notification → BroadcastReceiver → Activity) for default-launcher behavior. `MawaaiNotificationManager` opens activities directly, so this is already compliant.

## Validation gates before merging this PR

- [ ] `./gradlew :app:assembleDebug` succeeds locally on at least one developer machine running JDK 21.
- [ ] `./gradlew test` passes.
- [ ] Install on an Android 15 (API 35) emulator. App launches, splash → intro → home screen renders without status-bar overlap or back-gesture jank.
- [ ] Run through the design flow (sketch → AI processing → template → result) and verify nothing crashes.
- [ ] Open the photo picker and verify it works under the new partial-access UX.

## Related micro-tasks

- MT-013 — drop `accompanist-systemuicontroller` once we confirm `enableEdgeToEdge` covers everything (it likely already does after this PR).
- MT-015 — coordinated Compose BOM / lifecycle / nav bump; closes the `StateFlowValueCalledInComposition` lint disable.
- MT-024 — partial photo access UX audit (new).
- MT-025 — OpenCV 16 KB-aligned binary verification / bump to 4.10 if needed (new).
