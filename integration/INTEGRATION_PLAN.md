# INTEGRATION PLAN — Creative Studio → Mawaai Android

The complete EPIC + MT map. Read top-to-bottom. Cross-reference `PROMPTS.md` for paste-ready prompts.

---

## 📊 Backlog Summary

| EPIC | Title | MTs | Stage | Status |
|---|---|---|---|---|
| E7 | Multi-Provider AI Gateway | 4 | 1 | 🟢 Ready |
| E8 | Data Model (Room) | 3 | 2 | 🟢 Ready (after E7) |
| E1 | Surface Intelligence Catalog | 3 | 3 | 🟢 Ready (after E8) |
| E2 | Structured Vision Analysis (Phase 3) | 4 | 4 | 🟢 Ready (after E1) |
| E3 | Suggestions System (Phase 4) | 4 | 5 | 🟢 Ready (after E2) |
| E4 | Render Pipeline (Phase 5 + 7) | 4 | 6 | 🟢 Ready (after E3) |
| E5 | Render Quality Gate (Phase 6) | 3 | 7 | 🟢 Ready (after E4) |
| E9 | Ceramic Category | 2 | 8 | 🟢 Independent |
| E6 | Product Mockup Catalog (Phase 8) | 3 | 9 | 🟢 Ready (after E5) |

**30 micro-tasks total.** Sequential by stage; within a stage MTs can be parallelized only if their file-scopes don't overlap.

---

## EPIC E7 — Multi-Provider AI Gateway

> **Why first:** The user's immediate pain is that Gemini 1.5 deprecation crashed the app with HTTP 404. A single-provider design is brittle. After E7 lands, the next deprecation won't reach the user — the gateway falls back automatically.

### Goals
- Define a `VisionProvider` sealed interface and a `TextProvider` sealed interface
- Implement at least 4 concrete providers (Gemini, OpenRouter, Groq, Cloudflare Workers AI)
- Build an automatic fallback chain (configurable in Settings)
- Expose a user-facing "AI Provider" preference

### Concrete benefits
- HTTP 404 / 429 from any provider triggers fallback to next instead of crashing
- User can pin a specific free provider in Settings (Groq is currently the fastest free vision provider)
- Adding a new provider is a 1-file addition

### Micro-tasks

#### MT-036 — VisionProvider sealed registry + chain executor
- **Files added:** `design/ai/gateway/AiProvider.kt`, `design/ai/gateway/ProviderRegistry.kt`, `design/ai/gateway/FallbackChain.kt`
- **Files modified:** none
- **LOC est:** ~250
- **Specialist:** `mobile-ai-api-integrator` + new `ai-provider-gateway` skill
- **Acceptance:** `ProviderRegistry.vision(prompt, image): Result<String>` succeeds even when 2/4 providers are down.

#### MT-037 — GroqClient (Llama 3.2 Vision + Llama 3.1 70B text)
- **Files added:** `design/ai/groq/GroqApi.kt`, `design/ai/groq/GroqClient.kt`, `design/ai/groq/GroqDtos.kt`
- **Files modified:** `app/build.gradle.kts` (add `GROQ_API_KEY` BuildConfig field)
- **LOC est:** ~180
- **Specialist:** `mobile-ai-api-integrator`
- **Acceptance:** GroqClient is `@Singleton @Inject`, mirrors VisionProvider interface, returns valid response.

#### MT-038 — Cloudflare Workers AI vision support (LLaVA)
- **Files modified:** existing CloudflareClient (extend with vision method) OR new CloudflareVisionClient.kt
- **LOC est:** ~120
- **Specialist:** `mobile-ai-api-integrator`
- **Acceptance:** `cloudflareVisionClient.analyze(prompt, image)` returns valid String from `@cf/llava-hf/llava-1.5-7b-hf`.

#### MT-039 — Provider switcher UI in Settings
- **Files added:** `ui/settings/AiProviderSettings.kt`
- **Files modified:** existing Settings screen Composable + DataStore preferences
- **LOC est:** ~150
- **Specialist:** `jetpack-compose-architect`
- **Acceptance:** User can pick: "Auto fallback" | "Gemini only" | "OpenRouter only" | "Groq only" | "Cloudflare only". Selection persists across launches.

---

## EPIC E8 — Data Model (Room)

> **Why second:** The structured analysis + suggestions + render outputs all need persisted entities. Building this before STAGE 4 means we don't have to refactor when the schema lands.

### Goals
- Room entities mirroring Supabase tables (Project, Template, Analysis, Suggestion, RenderQuality, ProductMockup)
- Non-destructive migrations
- Repositories + DAOs

### Micro-tasks

#### MT-040 — Room entities + DAOs
- **Files added:** `data/database/entities/{Template,Project,Analysis,Suggestion,RenderQuality,ProductMockup}Entity.kt` + `data/dao/{Template,Project,Analysis,Suggestion,ProductMockup}Dao.kt`
- **Files modified:** `MawaaiDatabase.kt` (bump version, add new entities + DAOs), `DatabaseModule.kt` (provide new DAOs)
- **LOC est:** ~400
- **Specialist:** `repository-architecture-builder`
- **Acceptance:** All 6 entities have @Entity, @PrimaryKey, type converters where needed; build PASS; @Query DAOs cover CRUD.

#### MT-041 — Non-destructive migrations
- **Files modified:** `data/database/Migrations.kt` (new file), `MawaaiDatabase.kt` (register migrations)
- **LOC est:** ~120
- **Specialist:** `repository-architecture-builder`
- **Acceptance:** v1→v2 migration runs; existing user data preserved; schemas/ directory has new schema JSON.

#### MT-042 — Repositories
- **Files added:** `data/repository/{Project,Analysis,Suggestion,RenderQuality,ProductMockup}Repository.kt`
- **Files modified:** Hilt module providing repositories
- **LOC est:** ~280
- **Specialist:** `repository-architecture-builder`
- **Acceptance:** Each repository exposes a Flow-based observer + suspend mutators; no raw SQLite access outside DAOs.

---

## EPIC E1 — Surface Intelligence Catalog

> **Why third:** This is the foundational design knowledge that all subsequent phases reference. Without this, analysis prompts have no template constraints to compare against.

### Goals
- Port the 12 surface profiles from `template-intelligence.ts` to a Kotlin sealed class hierarchy
- Define render direction strings for each surface
- Wire the resolver: `Template → SurfaceProfile`

### The 12 surfaces
- Henna: `skin_palm`, `skin_hand_full`, `skin_foot`
- Garments: `fabric_abaya`, `fabric_thobe`, `fabric_toub`
- Walls: `wall_stone`, `wall_plaster`, `wall_arch`
- Ceramics: `ceramic_plate`, `ceramic_tile`, `ceramic_mug`

### Micro-tasks

#### MT-015 — SurfaceProfile sealed class hierarchy
- **Files added:** `design/ai/intelligence/SurfaceProfile.kt`, `design/ai/intelligence/SurfaceCatalog.kt`
- **LOC est:** ~350 (all 12 profiles, full constraints/masking/perspective/material)
- **Specialist:** new `surface-intelligence` skill (created in this commit)
- **Acceptance:** `SurfaceCatalog.resolve("skin_palm")` returns `SurfaceProfile.SkinPalm` with all 4 fields populated; round-trips via `Template.surfaceType`.

#### MT-016 — SURFACE_DIRECTION render prompt strings
- **Files added:** `design/ai/intelligence/SurfaceDirections.kt`
- **LOC est:** ~120 (12 directional render-prompt strings)
- **Specialist:** `stable-diffusion-pipeline-builder`
- **Acceptance:** `SurfaceDirections.forSurface("skin_palm")` returns the henna-on-palm direction string (verbatim port from Lovable, Arabic-compatible).

#### MT-017 — TemplateAssetManager → SurfaceProfile resolution
- **Files modified:** existing `TemplateAssetManager.kt` (add `surfaceProfile(template): SurfaceProfile` method)
- **LOC est:** ~80
- **Specialist:** `template-intelligence-engine`
- **Acceptance:** Existing template JSON entries resolve to the correct SurfaceProfile via name + category heuristics matching the TypeScript `resolveTemplateSurface()`.

---

## EPIC E2 — Structured Vision Analysis (Phase 3)

> **Why fourth:** This is where you finally get the "AI explains what it sees in the sketch and how to map it to the template" capability — the heart of the Lovable feature set.

### Goals
- Define `SketchAnalysis` Kotlin data class hierarchy mirroring the Zod schema
- Implement `StructuredAnalysisClient` that posts the schema in the prompt and validates the response
- Build heuristic fallback when AI returns invalid JSON
- Persist analysis to Room

### Schema fields (from `analysis.functions.ts`)
- `art_style`, `cultural_origin`
- `symmetry { type, accuracy_pct, weaker_side, notes }`
- `line_quality { confidence_0_10, consistency_0_10, shakiness_0_10, weight_variance_notes }`
- `composition { visual_center_x_0_1, visual_center_y_0_1, balance_score_0_10, negative_space_pct, hierarchy_notes }`
- `sketch_structure { primary_motifs[], stroke_flow, proportion_notes, must_preserve[] }`
- `template_mapping { surface_type, primary_zone, safe_zones[], lighting_direction, masking_notes, surface_fit_notes }`
- `template_fit { scale_match_0_10, density_match_0_10, style_compat_0_10, blockers[] }`
- `findings[] (max 12)` — each with id, severity, region (4 normalized floats), what, why, principle, cultural_context

### Micro-tasks

#### MT-018 — SketchAnalysis data class hierarchy
- **Files added:** `design/ai/analysis/SketchAnalysis.kt` (nested data classes)
- **LOC est:** ~250
- **Specialist:** `prompt-system-architect` + `vision-analysis-engineer`
- **Acceptance:** Gson can round-trip the full schema; Kotlin types match the Zod schema bit-for-bit.

#### MT-019 — StructuredAnalysisClient with JSON-mode prompt
- **Files added:** `design/ai/analysis/StructuredAnalysisClient.kt`
- **Files modified:** none
- **LOC est:** ~200
- **Specialist:** `vision-analysis-engineer` + `multimodal-ai-orchestrator`
- **Acceptance:** `analyze(sketchBitmap, template): Result<SketchAnalysis>` returns valid SketchAnalysis; falls back to heuristic on schema-validation failure.

#### MT-020 — Heuristic fallback analysis
- **Files added:** `design/ai/analysis/FallbackAnalysis.kt`
- **LOC est:** ~180
- **Specialist:** `vision-analysis-engineer`
- **Acceptance:** Given a template, returns a deterministic `SketchAnalysis` that the renderer can consume safely.

#### MT-021 — Persist analysis to Room
- **Files modified:** `AnalysisRepository.kt`, `ProjectRepository.kt` (link analysis to project)
- **LOC est:** ~100
- **Specialist:** `repository-architecture-builder`
- **Acceptance:** `projectRepository.observeWithAnalysis(projectId)` Flow emits when analysis is saved.

---

## EPIC E3 — Suggestions System (Phase 4)

### Goals
- Define `Suggestion` data class (category enum, region, title, explanation, principle, cultural_context, impact 0-100, auto_fixable, preview_hint)
- Implement `SuggestionsClient` that produces 4-8 cards per analysis
- Build heuristic fallback
- Compose UI: SuggestionCardsScreen with accept/skip flow

### Micro-tasks

#### MT-022 — Suggestion + SuggestionsResponse data classes
- **Files added:** `design/ai/suggestions/Suggestion.kt`
- **LOC est:** ~80
- **Specialist:** `prompt-system-architect`

#### MT-023 — SuggestionsClient
- **Files added:** `design/ai/suggestions/SuggestionsClient.kt`
- **LOC est:** ~200
- **Specialist:** `multimodal-ai-orchestrator`

#### MT-024 — Heuristic fallback
- **Files added:** `design/ai/suggestions/FallbackSuggestions.kt`
- **LOC est:** ~120
- **Specialist:** `prompt-system-architect`

#### MT-025 — Compose SuggestionCardsScreen
- **Files added:** `ui/design/suggestions/SuggestionCardsScreen.kt`, `ui/design/suggestions/SuggestionCardsViewModel.kt`
- **LOC est:** ~300
- **Specialist:** `jetpack-compose-architect`

---

## EPIC E4 — Render Pipeline (Phase 5 + 7)

### Goals
- Build the structure-preservation prompt builder (combines: structure rule, template intelligence, base direction, palette, color override, refinements, "no annotations")
- Wire image-edit rendering via Gemini 2.5 Flash Image (with provider fallback)
- Propagate color override from project → render prompt
- Persist render result to local storage + Room

### Micro-tasks

#### MT-026 — Render prompt builder
- **Files added:** `design/ai/render/RenderPromptBuilder.kt`
- **LOC est:** ~150
- **Specialist:** `stable-diffusion-pipeline-builder` + `prompt-system-architect`

#### MT-027 — Image-edit renderer via gateway
- **Files added:** `design/ai/render/ImageEditRenderer.kt`
- **LOC est:** ~250
- **Specialist:** `mobile-ai-api-integrator` + `image-compositing-engineer`

#### MT-028 — Color override propagation
- **Files modified:** `Project` entity + repository, `RenderPromptBuilder`
- **LOC est:** ~80
- **Specialist:** `repository-architecture-builder`

#### MT-029 — Render persistence
- **Files modified:** `ProjectRepository`, file storage path resolver
- **LOC est:** ~100
- **Specialist:** `repository-architecture-builder`

---

## EPIC E5 — Render Quality Gate (Phase 6)

### Goals
- `RenderQuality` data class (composition_preservation, surface_fit, lighting_realism, passed, issues[], notes)
- 2-tier validation: heuristic pre-check + AI visual QA review of (sketch ↔ render) pair
- Auto-block render below threshold

### Micro-tasks

#### MT-030 — RenderQuality schema + heuristic pre-check
- **Files added:** `design/ai/quality/RenderQuality.kt`, `design/ai/quality/HeuristicQualityCheck.kt`
- **LOC est:** ~150
- **Specialist:** `production-readiness-auditor`

#### MT-031 — AI visual QA review (multimodal: sketch + render)
- **Files added:** `design/ai/quality/AiQualityReviewer.kt`
- **LOC est:** ~200
- **Specialist:** `vision-analysis-engineer`

#### MT-032 — Auto-block + persistence
- **Files modified:** `ImageEditRenderer` (call quality gate before persisting), `ProjectRepository`
- **LOC est:** ~80
- **Specialist:** `production-readiness-auditor`

---

## EPIC E9 — Ceramic Category

> **Why independent:** New category, doesn't touch existing logic. Can run any time after E1.

### Goals
- Add ceramic template assets (mug, tile, plate)
- Wire ceramic into TemplateAssetManager

### Micro-tasks

#### MT-043 — Ceramic template assets + templates.json
- **Files added:** `app/src/main/assets/templates/ceramic/templates.json`, 3-9 image assets
- **LOC est:** data only
- **Specialist:** `template-intelligence-engine`

#### MT-044 — TemplateAssetManager ceramic wiring
- **Files modified:** `TemplateAssetManager.kt` (add ceramic category to scan)
- **LOC est:** ~30
- **Specialist:** `template-intelligence-engine`

---

## EPIC E6 — Product Mockup Catalog (Phase 8)

### Goals
- ProductMockup entity + 12 seeded mockups (matching the Lovable Supabase seed)
- Mockup compositor — place final render onto product scene
- Export pipeline (save composited image to gallery)

### Micro-tasks

#### MT-033 — ProductMockup entity + seed migration
- **Files added:** entity (already in E8) + seed values, `data/seed/MockupSeed.kt`
- **LOC est:** ~150
- **Specialist:** `repository-architecture-builder`

#### MT-034 — Mockup compositor
- **Files added:** `design/ai/mockup/MockupCompositor.kt`
- **LOC est:** ~250
- **Specialist:** `image-compositing-engineer` + `opencv-mobile-engineer`

#### MT-035 — Export pipeline
- **Files added:** `design/export/ExportPipeline.kt`
- **LOC est:** ~150
- **Specialist:** `mobile-performance-guardian`

---

## ⏱ Timeline Estimate

| Stage | EPICs | Agent time (focused) | Verification time | Calendar (1 MT/day) |
|---|---|---|---|---|
| 1 | E7 | 6-8 hrs | 1 hr | 4 days |
| 2 | E8 | 4-6 hrs | 1 hr | 3 days |
| 3 | E1 | 3-4 hrs | 30 min | 3 days |
| 4 | E2 | 6-8 hrs | 1.5 hrs | 4 days |
| 5 | E3 | 6-8 hrs | 1 hr | 4 days |
| 6 | E4 | 6-8 hrs | 1.5 hrs | 4 days |
| 7 | E5 | 4-5 hrs | 1 hr | 3 days |
| 8 | E9 | 1-2 hrs | 30 min | 2 days |
| 9 | E6 | 5-6 hrs | 1 hr | 3 days |
| **Total** | 9 EPICs / 30 MTs | **~50 hrs** | **~9 hrs** | **~30 days** |

Realistic shipping path: **3-4 calendar weeks** if you run 1-2 MTs per day with full verification between.

---

## 🔗 Cross-References

| Resource | Location |
|---|---|
| Hard rules for downstream agent | `ai_handoff/MASTER_PLAN.md` |
| Per-MT prompts | `integration/PROMPTS.md` |
| Per-MT verification | `integration/VERIFICATION.md` |
| 12-surface catalog | `integration/SURFACE_PROFILES.md` |
| Gateway design | `integration/AI_PROVIDER_GATEWAY.md` |
| Data model mapping | `integration/DATA_MODEL.md` |
| TS → Kotlin patterns | `integration/MIGRATION_BLUEPRINT.md` |
| Phase architecture | `integration/PIPELINE_ARCHITECTURE.md` |
| New skill | `skills/ai-provider-gateway/SKILL.md` |
