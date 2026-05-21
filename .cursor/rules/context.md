# Context Budget Rule

**Goal:** keep AI context-window usage under 40% per turn.

This is **advisory**, not enforced. Use these tactics to stay under budget.

---

## 1. Read order (do not skip)

1. **`AGENTS.md`** (root) — same heuristics for non-Cursor agents.
2. **`.cursorrules`** — Karpathy guidelines (think before coding, simplicity, surgical changes, goal-driven).
3. **This file** — context budget tactics.
4. **Package map** below — pick the right entry point.
5. Only then read source files, **targeted sections only**.

## 2. Grep before read

- Use `grep`/search to locate symbols. Never `read` a file you haven't first narrowed.
- For files ≥200 LOC, use `read` with `offset`/`limit`, not the whole file.
- Files ≥400 LOC: read interface/class signature lines only, then jump to the function you need.

## 3. Package map (entry points)

```
app/src/main/java/com/mawaai/love/app/
├── MainActivity.kt              ← app entry
├── MawaaiApp.kt                 ← Hilt application
├── core/
│   ├── components/              ← shared Compose UI (HeartButton, RoseGlassCard, …)
│   ├── theme/                   ← Color, Type, Theme, Motion
│   ├── utils/                   ← Date, File, Haptic, Quote
│   ├── opencv/                  ← OpenCVBootstrap (lazy native init)
│   ├── notifications/           ← MawaaiNotificationManager
│   └── responsive/              ← WindowSizeProvider
├── data/
│   ├── dao/ + database/         ← Room
│   ├── model/                   ← entities + DrawingStroke
│   ├── remote/{aladhan,zenquotes}/  ← Retrofit clients
│   └── repository/              ← repos for Artwork, Countdown, Letter, Memory, Mood, Profile
├── design/
│   ├── ai/                      ← AIEngine + pipelines/ + provider sub-clients
│   │   ├── cloudflare/  ← Workers AI (text→image, lazy)
│   │   ├── gemini/      ← vision + chat
│   │   ├── huggingface/ ← img→img, segmentation
│   │   ├── removebg/    ← bg removal fallback (lazy)
│   │   └── processors/  ← OpenCV/MLKit/TFLite ops
│   └── canvas/                  ← engine/ + model/ + ui/
├── di/                          ← Hilt modules (CoroutineScopes, DataStore, Database)
└── ui/                          ← Compose screens (home, intro, letters, memories, mood, …)
```

## 4. Common task → file map

| Task | Read first |
|---|---|
| Add a new Compose screen | `ui/navigation/NavGraph.kt` + a sibling screen in `ui/<feature>/` |
| Modify drawing canvas | `design/canvas/ui/DesignCanvasViewModel.kt` (then engine/ as needed) |
| Add an AI provider | `design/ai/AIEngine.kt` (interface) + an existing provider sub-package as template |
| Touch a Room model | `data/model/<X>.kt` + `data/dao/<X>Dao.kt` + `data/database/MawaaiDatabase.kt` |
| Edit theme/colors | `core/theme/Color.kt`, `core/theme/Theme.kt` |
| Build/dependency change | `gradle/libs.versions.toml` first, then `app/build.gradle.kts` |

## 5. Files NOT to read in full (skim only)

- `design/ai/AIEngineImpl.kt` and `design/ai/pipelines/*.kt` — read only the function you need.
- `design/ai/DrawingActionEngine.kt`, `design/ai/LocalDrawingAnalyzer.kt` — start with class signature + method list.
- `design/canvas/engine/BrushEngine.kt`, `design/canvas/engine/CanvasEngine.kt` — same.
- Any generated file (`build/`, `*.dex`, ProGuard mappings, OpenCV `.so` binaries).

## 6. Hard rules

- **Never** `read` an entire `build/` or `.gradle/` artifact.
- **Never** `read` more than 3 large (>300 LOC) files in one turn.
- **Always** prefer `grep -l` then targeted `read` with `offset`/`limit`.
- **Always** check `gradle/libs.versions.toml` before adding any dependency.
- Convert dashes to dots when referencing version-catalog libs in `.gradle.kts`: `libs.work-runtime` → `libs.work.runtime`.

## 7. When you're at 30%+ context

- Stop exploring. State what you know. Ask the user one focused question.
- Do not proactively read additional files "just in case".
