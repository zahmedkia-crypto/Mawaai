# Architecture Diagnostics (Phase 1 Playbook)

Goal: produce a complete diagnostic with the minimum file reads. Never load feature implementations during diagnostics.

## Allowed Reads (Phase 1 Only)

| Purpose | File |
|---|---|
| Manifest, permissions, components | `app/src/main/AndroidManifest.xml` |
| Build config, deps, ABI, NDK | `app/build.gradle(.kts)` + root `build.gradle(.kts)` |
| Module map | `settings.gradle(.kts)` |
| Proguard / R8 | `app/proguard-rules.pro` |
| DI graph | one Hilt/Koin module file |
| Layering sample | one Repository, one ViewModel, one UseCase, one Screen |
| Native lib init | `Application` class + `OpenCVLoader` init site |

If a file is not in this list and is not explicitly required by the active micro-task, do not read it.

## Diagnostic Checklist

### Build System
- [ ] AGP, Kotlin, Compose BOM versions consistent
- [ ] `minSdk` / `targetSdk` / `compileSdk` correct for MAWAAI (target 34+)
- [ ] `abiFilters` present if native libs are bundled
- [ ] NDK version pinned and compatible with OpenCV build
- [ ] KSP version matches Kotlin version
- [ ] Hilt / Dagger versions aligned

### Architecture Smells
- [ ] ViewModels reaching into Android framework (Context leaks)
- [ ] Repositories returning UI models (layering violation)
- [ ] Direct Retrofit calls from ViewModels (missing UseCase)
- [ ] Composables holding mutable business state
- [ ] God-objects in `util/` or `helper/`
- [ ] Untyped `Map<String, Any>` flowing through layers

### Native Library Risks
- [ ] OpenCV init pattern (`initDebug` vs `initLocal` vs `OpenCVLoader.initAsync`)
- [ ] `System.loadLibrary` order
- [ ] ABI mismatch between bundled `.so` and `abiFilters`
- [ ] Missing fallback for unsupported devices

### AI Integration Risks
- [ ] API keys in source (must be in BuildConfig from local.properties or secure storage)
- [ ] Synchronous network on main thread
- [ ] No retry/backoff for SD/ControlNet endpoints
- [ ] Bitmaps passed without lifecycle ownership

### UI Risks
- [ ] `setDecorFitsSystemWindows` not configured
- [ ] Padding applied to background layers
- [ ] Hardcoded status bar height (forbidden)
- [ ] No insets handling on bottom sheets / keyboards

## Output

Fill `assets/diagnostic_report_template.md` with findings, severity (P0/P1/P2), and proposed micro-task IDs.

## Stop Conditions

Stop diagnostics and escalate if:

- Project structure is unrecognizable (no clear `app/` module, no manifest)
- Critical files missing (manifest, gradle)
- More than 5 P0 issues detected — pause and have user confirm priority order
