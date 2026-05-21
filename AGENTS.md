# AGENTS.md — Mawaai

Repo conventions for AI agents (Devin, Claude Code, Cursor, others). Cursor users also get `.cursor/rules/context.md`; this file is the canonical source.

---

## What this project is

**Mawaai** — single-module Android app. Kotlin 2.1, Jetpack Compose, Hilt, Room, Retrofit, OpenCV, ML Kit, TensorFlow Lite. Arabic-first RTL. Romance/heritage aesthetic.

Build system: Gradle Kotlin DSL with Version Catalogs (`gradle/libs.versions.toml`).

> **This is NOT a JS/TS / Cloud Functions / monorepo project.** Do not introduce Turborepo, Nx, npm, pnpm, or any JS-tooling. The "cloud" sub-packages under `design/ai/` are HTTP **clients**, not a backend.

## Build / Verify

| Command | Purpose |
|---|---|
| `./gradlew assembleDebug` | Build debug APK |
| `./gradlew lint` | Run Android lint |
| `./gradlew test` | Unit tests |
| `./gradlew connectedDebugAndroidTest` | Instrumented tests (needs device/emulator) |

Always run `./gradlew assembleDebug` after Gradle / DI / Room schema changes.

## Context Budget Rule (≤40% per turn)

This is **advisory**, not enforced by tooling. Use these tactics:

1. **Read order:** `.cursorrules` → this file → `.cursor/rules/context.md` → package map → targeted source.
2. **Grep before read.** Locate symbols first, then read narrowed sections.
3. **Files ≥200 LOC:** read with `offset`/`limit`. Files ≥400 LOC: signature + needed function only.
4. **Never read** generated artifacts (`build/`, `*.dex`, ProGuard mappings, `.so` binaries).
5. **Never read** more than 3 large (>300 LOC) files in one turn.
6. **At 30%+ context:** stop, state what you know, ask one focused question.

## Package map

```
app/src/main/java/com/mawaai/love/app/
├── MainActivity.kt              ← entry
├── MawaaiApp.kt                 ← Hilt application class
├── core/
│   ├── components/              ← shared Compose UI
│   ├── theme/                   ← Color, Type, Theme, Motion, ThemedBackground
│   ├── utils/                   ← Date, File, Haptic, Quote
│   ├── opencv/                  ← OpenCVBootstrap
│   ├── lifecycle/               ← ForegroundResumeTracker
│   ├── notifications/           ← MawaaiNotificationManager
│   └── responsive/              ← WindowSizeProvider
├── data/
│   ├── dao/, database/, model/  ← Room
│   ├── remote/{aladhan,zenquotes}/  ← Retrofit clients
│   └── repository/              ← repos
├── design/
│   ├── ai/                      ← AIEngine + pipelines/ + sub-clients
│   │   ├── pipelines/           ← per-pipeline extension functions on AIEngineImpl
│   │   ├── cloudflare/, gemini/, huggingface/, removebg/  ← provider clients
│   │   └── processors/          ← OpenCV/MLKit/TFLite ops
│   ├── canvas/{engine,model,ui}/
│   └── showcase/
├── di/                          ← Hilt modules
└── ui/                          ← Compose screens (home, intro, letters, memories, mood, …)
```

## Common task → entry file

| Task | Read first |
|---|---|
| New Compose screen | `ui/navigation/NavGraph.kt` + sibling in `ui/<feature>/` |
| Drawing canvas | `design/canvas/ui/DesignCanvasViewModel.kt` |
| Add AI provider | `design/ai/AIEngine.kt` (interface) + existing provider as template |
| Room change | `data/model/X.kt` + `data/dao/XDao.kt` + `data/database/MawaaiDatabase.kt` (bump `version` + add `Migration`) |
| Theme/colors | `core/theme/Color.kt`, `core/theme/Theme.kt` |
| Dependency | `gradle/libs.versions.toml` first, then `app/build.gradle.kts` |

## Architectural conventions

- **MVVM + Clean + Repository pattern.**
- **Offline-first**: Room is the source of truth; remote calls are best-effort.
- **DI**: Hilt for everything. Heavy clients (Cloudflare, RemoveBg, HuggingFace) are injected via `dagger.Lazy<T>` so the AI graph isn't fully instantiated at app start.
- **Coroutines**: all I/O on `Dispatchers.IO`. Use `viewModelScope`.
- **State**: `StateFlow` + `collectAsStateWithLifecycle`.
- **RTL-first**: layouts use `start`/`end`, not `left`/`right`.
- **Compose**: every screen-level Composable has a `@Preview`.

## Build hygiene

- Convert dashes to dots when referencing libs from `libs.versions.toml` in `.gradle.kts`: `libs.work-runtime` → `libs.work.runtime`.
- Before adding a dependency, check `gradle/libs.versions.toml` first; add the alias there, then reference it.
- `local.properties` holds API keys (`GEMINI_API_KEY`, `PEXELS_API_KEY`, `HUGGINGFACE_API_KEY`, `REMOVE_BG_API_KEY`, `CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_API_TOKEN`). **Never** commit these.

## Surgical-changes contract (from `rules/CLAUDE.md`)

When editing existing code:
- Touch only what you must. Match existing style even if you'd do it differently.
- Don't refactor unrelated code, comments, or formatting.
- If you notice unrelated dead code, mention it; don't delete it unless asked.
- Remove imports/symbols *your changes* orphaned. Don't remove pre-existing dead code unless asked.

Every diff line must trace directly to the user's request.

## Files NOT to read in full

- `design/ai/AIEngineImpl.kt` and `design/ai/pipelines/*.kt`
- `design/ai/DrawingActionEngine.kt`, `design/ai/LocalDrawingAnalyzer.kt`
- `design/canvas/engine/BrushEngine.kt`, `design/canvas/engine/CanvasEngine.kt`

Use grep + targeted read.
