# Mawaai Project Scan - 2026-05-22

Purpose: continuation log for the next agent/developer pass. This scan was bounded to build files, manifest, DI, app entry points, API contracts, template metadata, navigation, and one representative file per major layer.

## Verification

- `./gradlew assembleDebug`: PASS.
- `./gradlew test`: PASS.
- `./gradlew test` warning to fix: Room schema export location is missing while `MawaaiDatabase` has `exportSchema = true`.
- Build warnings seen but not blocking: TensorFlow Lite duplicate namespace warning, several Compose/Material deprecations, Hilt/KSP incremental compilation warnings.

## Current App Shape

- Single Android module: `:app`.
- Kotlin 2.1.0, AGP 8.7.3, compile/target SDK 34, min SDK 26.
- Runtime routes currently wired in `ui/navigation/NavGraph.kt`: splash, intro, onboarding, home, memories, add/detail memory, letters, compose/detail letter, mood, settings, design.
- Design studio exists with catalog-driven categories, canvas, AI processing flow, template gallery, result/customize flow, showcase rendering, OpenCV processors, TFLite processors, Gemini/HuggingFace/Cloudflare/Remove.bg clients.
- OpenCV is eagerly initialized in `MawaaiApp.onCreate()` via `OpenCVBootstrap.init(this)` and guarded in processors.

## API Surface Inventory

Implemented in current source:

- Gemini text + vision clients: `design/ai/gemini/*`.
- HuggingFace client: RMBG, ControlNet Canny, Real-ESRGAN.
- Cloudflare Workers AI: text-to-image models plus SD 1.5 img2img refinement.
- Remove.bg background-removal fallback.
- Aladhan calendar client.
- ZenQuotes quote client.

Configured but missing or stale:

- `PEXELS_API_KEY` is still wired in `app/build.gradle.kts`, but there is no current `data/remote/pexels` package and no cards feature in `ui/navigation/NavGraph.kt`. `PROJECT_LOG.md` still describes Pexels/card work from an older state, so treat that part as stale until the files are restored or the log is corrected.
- `PROJECT_LOG.md` mentions Supabase remote sync and Supabase/Ktor rules, but current Gradle files do not include Supabase/Ktor dependencies and current source search found no Supabase package. Treat Supabase as missing, not implemented.
- `app/google-services.json` is tracked and contains a Firebase API key. That is usually not a secret by itself, but Firebase restrictions should be checked in the Google Cloud console before release.
- `local.properties` contains real Gemini and HuggingFace keys and is ignored by git. Do not print or commit it.

## Template Inventory

Asset folders under `app/src/main/assets/templates`:

| Category | Images | Masks | Metadata entries | Status |
|---|---:|---:|---:|---|
| `abaya` | 19 | 0 | 0 | Needs authored placement quads and optional masks. `templates.json` only has an example and an empty `templates` array. |
| `henna` | 12 | 0 | 3 | Only palm templates have authored quads. Hand and foot assets fall back to category defaults. |
| `thob_sudani` | 5 | 0 | 5 | Metadata exists, but the JSON states these are generated placeholders. Replace with real photos and retune quads. |
| `walls` | 5 | 0 | 5 | Metadata exists for all wall mockups. |

Template engine status:

- `TemplateAssetManager` scans image assets and reads optional `templates.json`.
- `TemplateCompositor` and `GarmentColorEngine` support category defaults, authored quads, blend modes, and optional mask files named `<templateId>.mask.png`.
- No masks are currently present, so fabric/skin integration relies on heuristic/category defaults.

## Findings

| ID | Severity | Finding | Evidence | Next task |
|---|---|---|---|---|
| MT-001 | P1 | Room is configured for schema export but no schema location is configured. | `MawaaiDatabase.kt` has `exportSchema = true`; `./gradlew test` warns. | Add Room Gradle plugin/schema location or set `exportSchema = false` if schema history is intentionally not kept. |
| MT-002 | P1 | Database uses destructive migrations. | `DatabaseModule.kt` calls `fallbackToDestructiveMigration()`. | Replace with real migrations before any release with user data. |
| MT-003 | P1 | Project log is stale around Supabase/Pexels/cards. | Source search found no Supabase, Pexels package, or cards routes; log claims they exist. | Reconcile `PROJECT_LOG.md`: either restore missing features or mark them as backlog. |
| MT-004 | P1 | Abaya templates are visually present but placement metadata is unauthored. | `abaya/templates.json` has `"templates": []`. | Author quads for all 19 abaya images; add masks for realistic fabric placement. |
| MT-005 | P1 | Henna hand/foot templates lack authored quads. | 12 images, 3 metadata entries. | Author hand and foot quads; add skin-region masks if available. |
| MT-006 | P2 | Sudanese thob assets are placeholders. | `thob_sudani/templates.json` says generated placeholders. | Replace with real licensed photos and retune quads. |
| MT-007 | P2 | API key hygiene needs release review. | `local.properties` has real keys; `google-services.json` is tracked. | Confirm `.gitignore`, rotate keys if ever exposed, restrict Firebase/Gemini/HF keys by app/package where supported. |
| MT-008 | P2 | Compose/API deprecation cleanup remains. | `./gradlew test` reported Material text field and `Icons.Filled.Undo` deprecations. | Replace deprecated APIs opportunistically after P1 work. |
| MT-009 | P2 | No emulator/device runtime validation was performed in this scan. | Only JVM/unit build tasks ran. | Run `connectedDebugAndroidTest` and manual AI/template flows on a device/emulator. |

## Missing Feature Backlog

Based on current navigation/source rather than old log claims:

- Romantic-side features not currently wired: music, wishes, countdown screens, quiz, story, cards/photo-card flow.
- Pexels API feature is not present in current source despite `PEXELS_API_KEY` build field and old log entries.
- Supabase/cloud sync is not present in current source despite old log entries and ProGuard keep rules.
- Template authoring remains incomplete for abaya, most henna assets, and thob real-photo replacement.
- Production API health checks are missing: no automated smoke test for Gemini, HuggingFace, Cloudflare, Remove.bg, Aladhan, ZenQuotes.

## Recommended Execution Order

1. MT-003: reconcile stale project log vs current source so future agents do not chase non-existent files.
2. MT-001 and MT-002: fix Room schema export and migration policy before data features expand.
3. MT-004 and MT-005: author template metadata for abaya and henna; this gives immediate user-visible quality gains.
4. MT-007: review API key restrictions and tracked Firebase config.
5. Add API smoke-test harnesses with fake or opt-in real keys.
6. Decide whether to restore or formally backlog Pexels/cards and Supabase sync.
7. Run emulator QA for design flow: draw/upload -> process -> template -> customize -> result save/share.

## Files Intentionally Not Read In Full

- `design/ai/AIEngineImpl.kt`
- `design/ai/pipelines/*.kt`
- `design/ai/DrawingActionEngine.kt`
- `design/ai/LocalDrawingAnalyzer.kt`
- `design/canvas/engine/BrushEngine.kt`
- `design/canvas/engine/CanvasEngine.kt`
- generated outputs under `build/`
- native `.so` binaries

