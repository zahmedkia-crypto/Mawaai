# MAWAAI Creative Studio → Android Integration Package

This folder is the **complete blueprint** for porting the Lovable.dev Creative Studio web app's AI rendering, building, and pipeline architecture into your existing Mawaai Android Kotlin app — with the explicit requirement that the user be able to **switch between free AI providers** (Gemini, Groq, Cloudflare Workers AI, OpenRouter, HuggingFace) at runtime.

The drawing board is intentionally NOT ported — your existing canvas engine stays.

---

## 📖 What's In This Folder

| File | Purpose |
|---|---|
| [`README.md`](./README.md) | This file — entry point + execution order |
| [`INTEGRATION_PLAN.md`](./INTEGRATION_PLAN.md) | The 9 EPICs / 35 micro-tasks with scope, files, time |
| [`PIPELINE_ARCHITECTURE.md`](./PIPELINE_ARCHITECTURE.md) | The 7-phase pipeline contract (Phase 2/3/4/5/6/7/8) |
| [`SURFACE_PROFILES.md`](./SURFACE_PROFILES.md) | 12-surface catalog ported as Kotlin sealed class hierarchy |
| [`AI_PROVIDER_GATEWAY.md`](./AI_PROVIDER_GATEWAY.md) | Multi-provider switcher design (Groq + CF + OpenRouter + Gemini) |
| [`DATA_MODEL.md`](./DATA_MODEL.md) | Supabase schema → Room entities mapping |
| [`PROMPTS.md`](./PROMPTS.md) | Per-MT ready-to-paste prompts for downstream agent |
| [`VERIFICATION.md`](./VERIFICATION.md) | Per-MT verification checklists |
| [`MIGRATION_BLUEPRINT.md`](./MIGRATION_BLUEPRINT.md) | TypeScript → Kotlin porting patterns + idioms |

---

## 🎯 What You Get From This Integration

| Before (current Android app) | After (post-integration) |
|---|---|
| GeminiClient returns Arabic prompt strings only | Structured `SketchAnalysis` with 12 region-anchored findings + culturally aware suggestions |
| Single AI provider (Gemini), crashes on 404 / quota | Multi-provider chain: Gemini → OpenRouter → Groq → Cloudflare, automatic fallback + user-selectable in Settings |
| Templates loaded from JSON, basic compositing | Full `SurfaceProfile` catalog: 12 surfaces × {constraints, masking, perspective, material, lighting} |
| No QA gate; renders ship whatever Gemini returns | `RenderQuality` validator: heuristic pre-check + AI visual QA review of (sketch ↔ render) pair |
| Only henna / abaya / thob / walls (4 categories) | + Ceramic category (mug / tile / plate) with full profiles |
| No color override system | `effectiveColor` propagates through render prompt for garments + ceramics |
| Suggestions are heuristic only | 4-8 region-anchored AI cards: category enum (line / symmetry / template / cultural / print / color), impact score 0-100, auto-fixable flag |
| Single fixed model `gemini-1.5-flash` (now 404) | Model registry — easily switch text + vision + image-edit models per provider |
| No product mockup catalog | 12 seeded product scenes: bridal palm, flat-lay abaya, majlis wall, stoneware mug, etc. |

---

## 🚀 Execution Order

**STAGE 0 — Done in current session.**
- ✅ MT-014: HTTP 404 fix (commit `90c8abed`) — Gemini 1.5 → 2.0-flash

**STAGE 1 — Provider gateway (DO FIRST, future-proofs everything).**
- E7.MT-036: VisionProvider sealed registry + fallback chain
- E7.MT-037: GroqClient (Llama 3.2 Vision)
- E7.MT-038: CloudflareWorkersAIClient vision support (LLaVA)
- E7.MT-039: Provider switcher UI in Settings

**STAGE 2 — Data model.**
- E8.MT-040: Room entities (Template, Project, Analysis, Suggestion, RenderQuality, ProductMockup)
- E8.MT-041: Migrations (non-destructive)
- E8.MT-042: Repositories + DAOs

**STAGE 3 — Surface intelligence.**
- E1.MT-015: SurfaceProfile sealed class hierarchy (12 profiles)
- E1.MT-016: SURFACE_DIRECTION render prompt strings
- E1.MT-017: TemplateAssetManager wiring

**STAGE 4 — Structured analysis pipeline (Phase 3).**
- E2.MT-018: SketchAnalysis data class hierarchy
- E2.MT-019: StructuredAnalysisClient (JSON mode + schema validation)
- E2.MT-020: Heuristic fallback analysis
- E2.MT-021: Persist analysis to Room

**STAGE 5 — Suggestions (Phase 4).**
- E3.MT-022: Suggestion data class + SuggestionsResponse
- E3.MT-023: SuggestionsClient
- E3.MT-024: Heuristic fallback
- E3.MT-025: Compose UI: SuggestionCardsScreen

**STAGE 6 — Render pipeline (Phase 5 + 7).**
- E4.MT-026: Structure-preservation prompt builder
- E4.MT-027: Image-edit render via Gemini 2.5 Flash Image (with provider fallback)
- E4.MT-028: Color override propagation
- E4.MT-029: Persist render output

**STAGE 7 — Render quality gate (Phase 6).**
- E5.MT-030: RenderQuality schema + heuristic pre-check
- E5.MT-031: AI visual QA review of (sketch ↔ render)
- E5.MT-032: Auto-block when score below threshold

**STAGE 8 — Ceramic category (new).**
- E9.MT-043: Add ceramic template assets (mug / tile / plate)
- E9.MT-044: Wire ceramic into TemplateAssetManager

**STAGE 9 — Product mockup catalog (Phase 8).**
- E6.MT-033: ProductMockup entity + 12 seeded mockups
- E6.MT-034: Mockup compositor (place render onto product scene)
- E6.MT-035: Export pipeline

**Total: 9 EPICs, 30 micro-tasks, ~2-3 weeks of focused work** if a downstream agent executes one MT at a time per the hard rules in `ai_handoff/MASTER_PLAN.md`.

---

## 🎮 How To Drive This

1. Open `ai_handoff/KICKOFF.md` and load it into your downstream agent (DeepSeek V3.x, Claude Sonnet, GPT-4o).
2. After the agent says "Ready", open `integration/PROMPTS.md`.
3. Copy the next MT block (start with E7.MT-036 — the multi-provider gateway).
4. Apply the diff. Run verification per `integration/VERIFICATION.md`.
5. Commit if all gates pass. Repeat.

The **eight hard rules** from `ai_handoff/MASTER_PLAN.md` apply to every MT in this folder too.

---

## ⚠️ Critical Constraints

1. **The user already has working canvas/drawing engine.** Do not port `DrawingCanvas.tsx` or `SketchCapture.tsx`. The existing `design/canvas/engine/CanvasEngine.kt` stays.
2. **Architecture transfer, not code transfer.** The source TypeScript uses React + Supabase + Vercel AI SDK. Kotlin needs Hilt + Room + OkHttp/Retrofit + manual JSON parsing. Do not copy code verbatim — re-architect using Kotlin idioms.
3. **No new dependencies without an MT authorizing it.** Each MT in `PROMPTS.md` explicitly lists any new dependency it requires.
4. **Strongly typed everything.** No `Map<String, Any>` — every schema becomes a sealed/data class.
5. **Sequential phases.** STAGE 1 (provider gateway) MUST land before STAGE 4 (analysis pipeline) because the analysis pipeline uses the gateway.

---

## 🛠 New Skill Added: `ai-provider-gateway`

A new specialist skill is added in this commit at `skills/ai-provider-gateway/SKILL.md`. It governs the discipline for designing multi-provider AI client abstractions on Android. The downstream agent should activate it whenever any STAGE 1 micro-task is running.

---

**Generated:** 2026-05-23
**Source project:** Lovable.dev Creative Studio (React + Supabase + Vercel AI SDK)
**Target project:** github.com/zahmedkia-crypto/Mawaai (Android Kotlin + Hilt + Compose + Room + OpenCV)
**Author:** mawaai-master-orchestrator skill + claude-code-task-director skill
