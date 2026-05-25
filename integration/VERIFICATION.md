# INTEGRATION VERIFICATION CHECKLISTS

Per-MT verification. Pair with `ai_handoff/VERIFICATION.md` (which has the universal gates).

For every MT below, run the **universal gates** from ai_handoff/VERIFICATION.md FIRST, then run the MT-specific checks listed here.

---

## ✅ Universal MT Gates (from ai_handoff/VERIFICATION.md)

```bash
./gradlew clean assembleDebug      # MUST PASS
./gradlew test                      # MUST PASS
git diff --stat                     # only MT-scoped files
git diff | grep -E "AIza|hf_|sk-or-v1|cfut_|vxcLa|gsk_"   # MUST be empty
git diff | grep "Map<String,\s*Any>"                       # MUST be empty in design/ai/, data/, di/
```

If any of the above fail → reject the agent output, do not commit.

---

## MT-036 — Gateway

```bash
# New gateway package exists with exactly 3 base files
ls app/src/main/java/com/mawaai/love/app/design/ai/gateway/
# Must contain: AiProvider.kt, FallbackChain.kt, ProviderRegistry.kt

# Adapter package exists with 4 adapters
ls app/src/main/java/com/mawaai/love/app/design/ai/gateway/adapters/
# Must contain: GeminiVisionProviderAdapter.kt, OpenRouterVisionProviderAdapter.kt,
#               GroqVisionProviderAdapter.kt, CloudflareVisionProviderAdapter.kt

# ProviderId enum has all 5 entries
grep "enum class ProviderId" app/src/main/java/com/mawaai/love/app/design/ai/gateway/AiProvider.kt
grep -c "(GEMINI\|OPENROUTER\|GROQ\|CLOUDFLARE_WORKERS_AI\|HUGGINGFACE)" app/src/main/java/com/mawaai/love/app/design/ai/gateway/AiProvider.kt
# Must print >= 5

# Existing Gemini / OpenRouter clients NOT modified
git diff HEAD~1 -- app/src/main/java/com/mawaai/love/app/design/ai/gemini/GeminiVisionClient.kt
git diff HEAD~1 -- app/src/main/java/com/mawaai/love/app/design/ai/openrouter/OpenRouterClient.kt
# Both MUST be empty

# Unit tests exist and pass
./gradlew :app:testDebugUnitTest --tests "*.gateway.FallbackChainTest"
```

## MT-037 — Groq

```bash
# BuildConfig field added exactly once
grep -c "GROQ_API_KEY" app/build.gradle.kts
# Must print exactly 2 (the buildConfigField line has the symbol twice)

# Groq package exists
ls app/src/main/java/com/mawaai/love/app/design/ai/groq/
# Must contain: GroqApi.kt, GroqClient.kt, GroqDtos.kt

# GroqVisionProviderAdapter uses the real client (not the stub)
grep "groqClient.visionAnalyze\|GroqClient" app/src/main/java/com/mawaai/love/app/design/ai/gateway/adapters/GroqVisionProviderAdapter.kt
# Must show at least one match

# Stub error message removed
grep "MT-037" app/src/main/java/com/mawaai/love/app/design/ai/gateway/adapters/GroqVisionProviderAdapter.kt
# Must be empty (the "not implemented yet" stub is gone)
```

After adding GROQ_API_KEY to local.properties, optional smoke test:
```bash
MAWAAI_RUN_LIVE_API_TESTS=1 ./gradlew :app:test --tests "*.gateway.*"
```

## MT-038 — Cloudflare vision

```bash
# Existing CloudflareClient has new method
grep -E "(suspend fun llavaVision|fun llavaVision)" app/src/main/java/com/mawaai/love/app/design/ai/cloudflare/CloudflareClient.kt
# Must show one match

# CloudflareVisionProviderAdapter implemented
grep "cloudflareClient.llavaVision\|CloudflareClient" app/src/main/java/com/mawaai/love/app/design/ai/gateway/adapters/CloudflareVisionProviderAdapter.kt
# Must show at least one match

# Existing text + SD methods NOT modified
git diff HEAD~1 -- app/src/main/java/com/mawaai/love/app/design/ai/cloudflare/CloudflareClient.kt | grep -E "^-.*fun " | grep -v "llavaVision"
# Must be empty (no method removed)
```

## MT-039 — Settings UI

```bash
# New ViewModel + Composable exist
ls app/src/main/java/com/mawaai/love/app/ui/settings/
# Must contain: AiProviderSettings.kt, AiProviderSettingsViewModel.kt

# ProviderRegistry has setMode / setOrder methods
grep "suspend fun setMode\|suspend fun setOrder" app/src/main/java/com/mawaai/love/app/design/ai/gateway/ProviderRegistry.kt
# Must show 2 matches
```

Manual:
- Launch app on emulator → Settings → AI Provider section renders
- Toggle "Auto fallback" off, pick "Groq only", close app, reopen → still "Groq only"
- Reorder providers in Auto mode → restart → order persisted

## MT-040 — Room entities

```bash
# DB version bumped to 2
grep "version = 2\|version=2" app/src/main/java/com/mawaai/love/app/data/database/MawaaiDatabase.kt
# Must show one match

# 3 new entity files
ls app/src/main/java/com/mawaai/love/app/data/database/entities/
# Must contain: TemplateEntity.kt, ProjectEntity.kt, ProductMockupEntity.kt

# 3 new DAO files
ls app/src/main/java/com/mawaai/love/app/data/dao/ | grep -E "(Template|Project|ProductMockup)Dao\.kt"
# Must show 3 matches

# Schema JSON for v2 generated
ls app/schemas/com.mawaai.love.app.data.database.MawaaiDatabase/2.json
# Must exist
```

## MT-041 — Migrations

```bash
# Migration file exists
ls app/src/main/java/com/mawaai/love/app/data/database/Migrations.kt
# Must exist

# No destructive migration in source
grep "fallbackToDestructiveMigration" app/src/main/java/
# Must be empty

# Migration registered on database builder
grep "addMigrations.*MawaaiMigrations\.ALL\|addMigrations(\*MawaaiMigrations" app/src/main/java/com/mawaai/love/app/data/database/MawaaiDatabase.kt
# Must show one match

# MigrationTest passes
./gradlew :app:testDebugUnitTest --tests "*MigrationTest*"
```

## MT-042 — Repositories

```bash
# 3 repositories exist
ls app/src/main/java/com/mawaai/love/app/data/repository/ | grep -E "(Template|Project|ProductMockup)Repository\.kt"
# Must show 3 matches

# Repositories expose Flow
grep "fun observe\|: Flow<" app/src/main/java/com/mawaai/love/app/data/repository/*.kt | wc -l
# Must be >= 6 (each repo has at least 1-2 Flow observers)

# No entity types leak outside data/
grep -r "TemplateEntity\|ProjectEntity\|ProductMockupEntity" app/src/main/java/com/mawaai/love/app/ \
  --include="*.kt" | grep -v "/data/"
# Must be empty
```

## MT-015 — SurfaceProfile catalog

```bash
# Single sealed file
ls app/src/main/java/com/mawaai/love/app/design/ai/intelligence/SurfaceProfile.kt
# Must exist

# All 12 data objects present
grep -c "data object" app/src/main/java/com/mawaai/love/app/design/ai/intelligence/SurfaceProfile.kt
# Must be >= 12

# Catalog test passes for all 12 cases
./gradlew :app:testDebugUnitTest --tests "*.SurfaceCatalogTest"
```

## MT-016 — SurfaceDirections

```bash
# 12 direction strings
grep -c "Render this sketch" app/src/main/java/com/mawaai/love/app/design/ai/intelligence/SurfaceDirections.kt
# Must be exactly 12

# QUALITY_TAIL constant defined
grep "const val QUALITY_TAIL" app/src/main/java/com/mawaai/love/app/design/ai/intelligence/SurfaceDirections.kt
# Must show one match
```

## MT-017 — TemplateAssetManager wiring

```bash
# Single new method added; nothing else changed
git diff HEAD~1 -- "*TemplateAssetManager.kt" | grep -E "^\+.*fun " | wc -l
# Must be exactly 1
git diff HEAD~1 -- "*TemplateAssetManager.kt" | grep -E "^\-.*fun " | wc -l
# Must be 0
```

## MT-018 — SketchAnalysis schema

```bash
ls app/src/main/java/com/mawaai/love/app/design/ai/analysis/SketchAnalysis.kt
# Must exist

# All 8 inner data classes
grep -c "data class \(Symmetry\|LineQuality\|Composition\|SketchStructure\|TemplateMapping\|TemplateFit\|Finding\|NormalizedRect\)" \
  app/src/main/java/com/mawaai/love/app/design/ai/analysis/SketchAnalysis.kt
# Must be >= 8 (some may be in separate files — adjust count accordingly)

# Gson round-trip test
./gradlew :app:testDebugUnitTest --tests "*SketchAnalysisRoundTripTest"
```

## MT-019 — StructuredAnalysisClient

```bash
ls app/src/main/java/com/mawaai/love/app/design/ai/analysis/StructuredAnalysisClient.kt
# Must exist

# Uses the gateway (not Gemini directly)
grep "providerRegistry\|FallbackChain" app/src/main/java/com/mawaai/love/app/design/ai/analysis/StructuredAnalysisClient.kt
# Must show at least one match
```

## MT-020 — FallbackAnalysis

```bash
ls app/src/main/java/com/mawaai/love/app/design/ai/analysis/FallbackAnalysis.kt
# Must exist

# Deterministic output test
./gradlew :app:testDebugUnitTest --tests "*FallbackAnalysisTest"
```

## MT-021 — Persist analysis

```bash
# AnalysisRepository (if separate) or ProjectRepository.saveAnalysis exists
grep "fun saveAnalysis\|suspend fun saveAnalysis" app/src/main/java/com/mawaai/love/app/data/repository/ProjectRepository.kt
# Must show one match
```

## MT-022 / MT-023 / MT-024 / MT-025 — Suggestions

```bash
ls app/src/main/java/com/mawaai/love/app/design/ai/suggestions/
# Must contain: Suggestion.kt, SuggestionsClient.kt, FallbackSuggestions.kt

# UI screen exists
ls app/src/main/java/com/mawaai/love/app/ui/design/suggestions/
# Must contain: SuggestionCardsScreen.kt, SuggestionCardsViewModel.kt

# Category enum has all 6
grep "(LINE|SYMMETRY|TEMPLATE|CULTURAL|PRINT|COLOR)" app/src/main/java/com/mawaai/love/app/design/ai/suggestions/Suggestion.kt | wc -l
# Must be >= 6
```

## MT-026 / 027 / 028 / 029 — Render

```bash
ls app/src/main/java/com/mawaai/love/app/design/ai/render/
# Must contain: RenderPromptBuilder.kt, ImageEditRenderer.kt

# Builder uses templateIntelligencePrompt + structure preservation rule
grep "templateIntelligencePrompt\|structurePreservation\|CRITICAL: Preserve" app/src/main/java/com/mawaai/love/app/design/ai/render/RenderPromptBuilder.kt
# Must show multiple matches

# Renderer goes through the gateway
grep "providerRegistry\|FallbackChain" app/src/main/java/com/mawaai/love/app/design/ai/render/ImageEditRenderer.kt
# Must show at least one match
```

## MT-030 / 031 / 032 — Quality gate

```bash
ls app/src/main/java/com/mawaai/love/app/design/ai/quality/
# Must contain: RenderQuality.kt, HeuristicQualityCheck.kt, AiQualityReviewer.kt

# Renderer calls quality gate before persist
grep -A 3 "qualityGate\|qualityCheck\|qualityReviewer" app/src/main/java/com/mawaai/love/app/design/ai/render/ImageEditRenderer.kt | grep "passed\|blockers"
# Must show at least one match
```

## MT-043 / 044 — Ceramic category

```bash
ls app/src/main/assets/templates/ceramic/
# Must exist; should contain templates.json + image assets

# templates.json valid
python3 -c "import json; data = json.load(open('app/src/main/assets/templates/ceramic/templates.json')); print('entries:', len(data['templates']))"

# TemplateAssetManager scans ceramic
grep "ceramic" app/src/main/java/com/mawaai/love/app/data/repository/TemplateRepository.kt
# OR wherever the asset scanning lives — should show ceramic in the scan list
```

## MT-033 / 034 / 035 — Mockups

```bash
# Seed data file
ls app/src/main/java/com/mawaai/love/app/data/seed/MockupSeed.kt
# Must exist

# 12 mockups
grep -c "ProductMockupEntity(" app/src/main/java/com/mawaai/love/app/data/seed/MockupSeed.kt
# Must be exactly 12

# Compositor + Export
ls app/src/main/java/com/mawaai/love/app/design/ai/mockup/MockupCompositor.kt
ls app/src/main/java/com/mawaai/love/app/design/export/ExportPipeline.kt
# Both must exist

# Seed inserted on first launch
grep "MockupSeed.ALL\|productMockupRepository.seed" app/src/main/java/com/mawaai/love/app/
# Must show at least one match (in MawaaiApp.onCreate or a SeedWorker)
```

---

## 🚨 Red-Flag Patterns (Reject Output If You See Any)

These violate the Hard Rules and indicate the agent went off-script:

1. **Modified GeminiVisionClient.kt during STAGE 1.** The gateway wraps, never modifies the client directly.
2. **Added a new dependency to libs.versions.toml without an MT authorizing it.** Each MT explicitly lists its allowed dependencies.
3. **`Map<String, Any>` anywhere in design/ai/, data/, or di/.** Strongly typed only.
4. **The agent skipped reading the doc.** The output won't reference exact filenames from the doc; it'll be obvious from style.
5. **`fallbackToDestructiveMigration()` reappears after MT-041.** Reject.
6. **Real API keys in any diff.** Reject, force re-prompt, rotate the leaked key.
7. **Adapter modifies the wrapped client.** The adapter should ONLY call public methods on the existing client; if you see the agent edit GeminiVisionClient.kt, reject.
8. **The agent says "I also fixed..." or "While I was there...".** Out-of-scope refactor. Reject.

---

## 🧯 Recovery Procedure

If a commit lands with a verification failure that you only catch later:

```bash
# Identify the bad commit
git log --oneline -20

# Revert (creates a new commit that undoes the bad one — keeps history clean)
git revert <sha>
git push origin master

# Then write a follow-up MT prompt:
# "MT-XXX-fix: revert of <sha> failed because <reason>. Re-attempt with constraints: <additional rules>."
```

Never `git reset --hard` once pushed — it rewrites public history and breaks other clones.

---

## 📈 Tracking Template

Keep a simple log (paper or `EXECUTION_LOG.md`):

```
MT-036  sha=...  date=2026-05-26  gates: ✅ build ✅ test ✅ secrets ✅ scope  notes: ProviderRegistry tests fast (2ms)
MT-037  sha=...  date=2026-05-27  gates: ✅ ✅ ✅ ✅                            notes: Groq smoke test 312ms
MT-038  sha=...  date=2026-05-27  gates: ✅ ✅ ✅ ✅                            notes: LLaVA needs prompt tweak for Arabic
MT-039  sha=...  date=2026-05-28  gates: ✅ ✅ ✅ ✅                            notes: drag reorder needs tap-to-pin instead
MT-040  sha=...  ...
```

When all 30 MTs check off → tag `v2.0.0-rc1` and start the Play Store release EPIC.
