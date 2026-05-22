---
name: mobile-project-scanner
description: Safely scans Android/Flutter/React Native projects to produce architecture diagnostics without loading the full codebase. Use when the user says "analyze my project", "scan architecture", "understand this app", or before any large refactor. Reads only manifests, gradle configs, DI graph, and one representative file per layer. Produces a structured diagnostic report with P0/P1/P2 findings, dependency map, layering check, native-lib risks, and a proposed micro-task list. Refuses to read feature implementations during scan phase.
icon: search
color: Blue
---

# Mobile Project Scanner

Phase 1 specialist. Produces an architecture diagnostic with minimum file reads. Never loads feature implementations during scanning.

## When to Use

- "analyze my project" / "scan architecture" / "understand this app"
- Before any large refactor or feature build
- Before activating `microtask-orchestrator` or any implementation specialist
- After a build failure when root cause is unclear

## Allowed Reads (Scan Phase Only)

| Purpose | File |
|---|---|
| Manifest, permissions, components | `app/src/main/AndroidManifest.xml` |
| Build config, deps, ABI, NDK | `app/build.gradle(.kts)` + root `build.gradle(.kts)` |
| Module map | `settings.gradle(.kts)` |
| Proguard / R8 | `app/proguard-rules.pro` |
| DI graph | one Hilt/Koin module file |
| Layering sample | one Repository, one ViewModel, one UseCase, one Screen/Composable |
| Native lib init | `Application` class + `OpenCVLoader` init site |

For Flutter: `pubspec.yaml`, `analysis_options.yaml`, `lib/main.dart`, one widget + one provider/bloc.
For RN: `package.json`, `metro.config.js`, `app.json`, `App.tsx`, one screen + one slice/store.

If a file is not in this list and not explicitly required, **do not read it**.

## Diagnostic Checklist

### Build System
- AGP, Kotlin, Compose BOM versions consistent
- `minSdk` / `targetSdk` / `compileSdk` correct (target 34+)
- `abiFilters` present when native libs are bundled
- NDK version pinned and OpenCV-compatible
- KSP version matches Kotlin version
- Hilt/Dagger versions aligned

### Architecture Smells
- ViewModels reaching into Android framework (Context leaks)
- Repositories returning UI models (layering violation)
- Direct Retrofit calls from ViewModels (missing UseCase)
- Composables holding mutable business state
- God-objects in `util/` or `helper/`
- Untyped `Map<String, Any>` flowing through layers

### Native Library Risks
- OpenCV init pattern (`initLocal` vs deprecated `initAsync`)
- `System.loadLibrary` order
- ABI mismatch between bundled `.so` and `abiFilters`
- Missing fallback for unsupported devices

### AI Integration Risks
- API keys in source (must be `BuildConfig` from `local.properties` or secure storage)
- Synchronous network on main thread
- No retry/backoff for SD/ControlNet endpoints
- Bitmaps passed without lifecycle ownership

### UI Risks
- `setDecorFitsSystemWindows` not configured
- Padding applied to background layers
- Hardcoded status bar height (forbidden)
- No insets handling on bottom sheets / keyboards

## Output

Fill the master orchestrator's `diagnostic_report_template.md` with:
- Findings table (P0 / P1 / P2)
- Architecture snapshot (layering, DI, AI pipeline presence, template engine presence, edge-to-edge state)
- Recommended phase order
- **List of files intentionally NOT read** (proves context discipline)

Each finding gets a proposed micro-task ID (e.g., `MT-001 Fix abiFilters`) so downstream skills can pick up the work.

## Stop Conditions

Escalate to user if:
- Project structure unrecognizable (no clear `app/` module, no manifest)
- Critical files missing (manifest, gradle)
- More than 5 P0 issues — pause and confirm priority order before continuing
