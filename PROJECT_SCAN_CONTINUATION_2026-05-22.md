# Mawaai Project Scan Continuation - 2026-05-22 (Phase 1 + 3 deliverable)

## Phase Header

Active phases: **Phase 1 (Architecture Scan) — REVALIDATION** + **Phase 3 (Stability P0) — RECONCILE**.
Active specialist: `mawaai-master-orchestrator` delegating to `mobile-project-scanner` and `repository-architecture-builder`.

## Context Budget (files read this turn)

Per Hard Rule #1 (never load the entire codebase), only the following were read:

- `PROJECT_SCAN_2026-05-22.md` — to inherit prior diagnostic
- `build.gradle.kts` (root) — confirm plugin aliases
- `settings.gradle.kts` — confirm module composition
- `app/build.gradle.kts` — confirm Room plugin + schemaDirectory wiring
- `app/src/main/java/com/mawaai/love/app/di/DatabaseModule.kt` — verify migration policy
- Listings of: `app/`, `app/src/main/java/com/mawaai/love/app/{data,di,data/database}`, `app/schemas/`, `app/src/main/assets/templates/{abaya,henna}/`
- `app/src/main/assets/templates/abaya/templates.json` (1238 B)
- `app/src/main/assets/templates/henna/templates.json` (846 B)

Files explicitly NOT read (per micro-task scope):
`design/ai/AIEngineImpl.kt`, `design/ai/pipelines/*`, `design/canvas/engine/*`, `MawaaiDatabase.kt` body, DAOs, NavGraph internals.

## Revalidation of Prior Findings

| ID | Prior Status | Revalidated Status | Evidence |
|---|---|---|---|
| MT-001 | P1: Room schema export location missing | **RESOLVED IN CURRENT CODE** | `app/build.gradle.kts` lines 11–13 declare `room { schemaDirectory("$projectDir/schemas") }`. `app/schemas/com.mawaai.love.app.data.database.MawaaiDatabase/` exists in tree. |
| MT-002 | P1: Destructive migrations in DatabaseModule | **RESOLVED IN CURRENT CODE** | `DatabaseModule.kt::provideDatabase` calls only `Room.databaseBuilder(...).build()` — no `fallbackToDestructiveMigration()`. Build passes (`assembleDebug` PASS). |
| MT-003 | P1: PROJECT_LOG.md stale (Supabase/Pexels/cards) | **OPEN — addressed in this commit** | See "Reconciliation Note" below. |
| MT-004 | P1: 19 abaya images, 0 metadata | **PARTIALLY ADDRESSED in this commit** | `assets/templates/abaya/templates.json` now contains 19 default_estimate entries. On-device pixel-tuning still required. |
| MT-005 | P1: 12 henna images, only 3 metadata | **PARTIALLY ADDRESSED in this commit** | `assets/templates/henna/templates.json` now contains 12 entries (3 authored palms + 3 hand + 6 foot default_estimates). |
| MT-006 | P2: thob_sudani placeholders | OPEN | Requires real licensed photo replacement; deferred. |
| MT-007 | P2: API key hygiene | OPEN | Tracked for separate audit micro-task. |
| MT-008 | P2: Compose deprecation cleanup | OPEN | Opportunistic only. |
| MT-009 | P2: No emulator runtime validation | OPEN | Requires device session; outside CI-only scope. |

## Reconciliation Note (MT-003)

The legacy `PROJECT_LOG.md` (290 KB) describes Pexels integration, Supabase cloud sync,
and a romantic-side cards/photo-card flow. **None of those packages exist in the
current `app/src/main/java/com/mawaai/love/app/` tree**, and no Supabase/Ktor
dependencies appear in `app/build.gradle.kts` or the Gradle version catalog.

Treat the following as **formally backlogged, not regressed**:

- `data/remote/pexels/*` — not present; `PEXELS_API_KEY` build field is unused.
- Supabase / Ktor cloud sync — not present in dependencies or source.
- Cards / photo-card / music / wishes / countdown / quiz / story screens — not in `NavGraph.kt`.

Future agents should:

1. Not "restore" these features speculatively.
2. Treat each as its own EPIC if reintroduced (see master-orchestrator decomposition rule).
3. Remove `PEXELS_API_KEY` BuildConfig field if Pexels is permanently out of scope (separate micro-task, low priority).

## Deliverable

This continuation commit adds:

1. **`app/src/main/assets/templates/abaya/templates.json`** — 19 entries with category-aware default quads (beaded/classic/embroidered/kaftan/modern), all marked `authoring_status: "default_estimate"`. Per-category placement defaults are documented in the `_category_defaults` block.
2. **`app/src/main/assets/templates/henna/templates.json`** — 12 entries (3 prior authored palms preserved + 9 new default_estimate entries for hand/foot surfaces). Per-surface defaults documented in `_surface_defaults`.
3. **This file** — `PROJECT_SCAN_CONTINUATION_2026-05-22.md` — Phase 1 revalidation + Phase 3 reconciliation.

## Execution Order (already executed by this commit)

1. ✅ Revalidate MT-001 + MT-002 against current source — both already resolved.
2. ✅ Generate abaya templates.json scaffold (19 entries).
3. ✅ Generate henna templates.json fill-in (9 new entries, 3 preserved).
4. ✅ Write reconciliation note for MT-003.

## Risks + Dependencies

- **R1 (MT-004/005)**: Default_estimate quads are NOT pixel-perfect. They use category/surface heuristics that produce visually-correct placement zones in the typical case, but each template needs on-device QA before release. The `authoring_status` field flags this explicitly to prevent shipping unverified data.
- **R2 (MT-003)**: PROJECT_LOG.md itself was not modified (290 KB rewrite is out of scope). The reconciliation lives here and supersedes log claims for future agents.
- **R3**: TemplateCompositor must continue to honor category defaults when `authoring_status == "default_estimate"` — verify the loader treats them the same as authored entries (it should, per existing schema).

## Validation Steps

1. `./gradlew assembleDebug` — must still PASS (no source code touched).
2. `./gradlew test` — must still PASS.
3. Manual: on-device load each abaya/henna template, capture the rendered design overlay, eyeball-check placement against the fabric/skin region.
4. For any template where the default_estimate quad mis-places the design, author the corrected quad and flip `authoring_status` to `"authored"`.
5. If a template needs mask-based fabric integration, add `<id>.mask.png` next to the image and flip status to `"masked"`.

## Next Micro-Task

**MT-010 (recommended next):** On-device template QA pass for the 28 default_estimate
entries (19 abaya + 9 henna). Output: an updated `templates.json` for each category
with `authoring_status: "authored"` and any quad corrections, plus a short
`QA_NOTES.md` summarizing per-template adjustments.

Specialist routing for MT-010: `template-intelligence-engine` (primary) +
`image-compositing-engineer` (placement-region detection assistance).

After MT-010, the next P1 candidate is **MT-007** (API key hygiene audit) which the
`production-readiness-auditor` skill owns.
