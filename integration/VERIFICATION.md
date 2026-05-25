# VERIFICATION — Per-MT Acceptance Checklists

After the downstream agent applies an MT diff, run the matching checklist here. **Do not commit if any step fails.**

Universal pre-flight (every MT):
```bash
cd ~/path/to/Mawaai
git status                                # must be clean before applying diff
git pull origin master                    # sync
./gradlew clean assembleDebug             # baseline must PASS
./gradlew test                            # baseline must PASS
git log --oneline -3                      # note current HEAD
```

After applying the diff (every MT):
```bash
./gradlew clean assembleDebug              # MUST PASS
./gradlew test                              # MUST PASS
git diff --stat                             # only files in MT scope
git diff | grep -E "AIza|hf_|sk-or-v1|cfut_|vxcLa|gsk_"   # MUST be empty
git diff | grep "Map<String,\s*Any>"        # MUST be empty in template/AI/repo/DI
```

---

## STAGE 1 — Provider Gateway

### E7.MT-036 — VisionProvider sealed registry + FallbackChain

```bash
# Files created
ls -la app/src/main/java/com/mawaai/love/app/design/ai/gateway/
# Expected: AiProvider.kt, FallbackChain.kt, ProviderRegistry.kt

# No production class touched
git diff HEAD~1 -- app/src/main/java/com/mawaai/love/app/design/ai/gemini/
# MUST be empty

# Sealed interface compiles
grep -n "sealed interface VisionProvider" app/src/main/java/com/mawaai/love/app/design/ai/gateway/AiProvider.kt
# MUST show match

# ProviderId enum has 5 entries
grep -c "GEMINI\|OPENROUTER\|GROQ\|CLOUDFLARE_WORKERS_AI\|HUGGINGFACE" \
    app/src/main/java/com/mawaai/love/app/design/ai/gateway/AiProvider.kt
# MUST >= 5

# Error hierarchy exists
grep "ProviderRecoverableError\|ProviderFatalError" \
    app/src/main/java/com/mawaai/love/app/design/ai/gateway/AiProvider.kt | wc -l
# MUST >= 8 (5 recoverable + 3 fatal)

# Unit tests exist and pass
./gradlew test --tests "*FallbackChainTest"
# MUST PASS
```

### E7.MT-037 — GroqClient

```bash
ls -la app/src/main/java/com/mawaai/love/app/design/ai/groq/
# Expected: GroqApi.kt, GroqDtos.kt, GroqVisionProvider.kt, GroqTextProvider.kt

# BuildConfig field added
grep "GROQ_API_KEY" app/build.gradle.kts
# MUST show 2 lines (declaration + property lookup)

# Model constant pinned with audit date
grep -A1 "llama-3.2-90b-vision-preview\|llama-3.1-70b-versatile" \
    app/src/main/java/com/mawaai/love/app/design/ai/groq/
# MUST show comment with date

# No real API key committed
grep -rE "gsk_[A-Za-z0-9]{30,}" app/src/main/java/ 2>/dev/null
# MUST be empty
```

### E7.MT-038 — Cloudflare vision

```bash
# Either a new CloudflareVisionProvider OR a method added to existing Cloudflare client
git diff HEAD~1 -- "*Cloudflare*" | grep -E "@cf/llava\|llava-1.5-7b"
# MUST show match

# Vision method takes Bitmap, not arbitrary type
git diff HEAD~1 | grep "fun visionAnalyze.*Bitmap"
# MUST show match
```

### E7.MT-039 — Provider switcher UI

```bash
ls -la app/src/main/java/com/mawaai/love/app/ui/settings/AiProviderSettings*.kt
# Expected: AiProviderSettings.kt, AiProviderSettingsViewModel.kt

# Radio group present
grep -c "RadioButton\|RadioRow" \
    app/src/main/java/com/mawaai/love/app/ui/settings/AiProviderSettings.kt
# MUST >= 5 (Auto + 5 providers)

# DataStore preference key declared
grep "ai_provider_mode\|ai_provider_order" \
    app/src/main/java/com/mawaai/love/app/core/preferences/SettingsDataStore.kt
# MUST show 2 matches

# No API key value displayed in UI
grep "BuildConfig\.\w\+_API_KEY\|BuildConfig\.GROQ_API_KEY" \
    app/src/main/java/com/mawaai/love/app/ui/settings/AiProviderSettings.kt
# MUST be empty (status only, never the value)

# Run app -> navigate to Settings -> AI Provider
# Manual: change selection, kill app, reopen, selection persists
```

---

## STAGE 2 — Data Model

### E8.MT-040 — Room entities + DAOs

```bash
ls -la app/src/main/java/com/mawaai/love/app/data/database/entities/
# Expected: TemplateEntity.kt, ProjectEntity.kt, ProductMockupEntity.kt, Domain.kt

ls -la app/src/main/java/com/mawaai/love/app/data/dao/
# Expected: TemplateDao.kt, ProjectDao.kt, ProductMockupDao.kt (existing + new)

# Entities have @Entity annotation
grep -l "@Entity" app/src/main/java/com/mawaai/love/app/data/database/entities/*.kt | wc -l
# MUST >= 3

# Database version bumped
git diff HEAD~1 app/src/main/java/com/mawaai/love/app/data/database/MawaaiDatabase.kt | grep -E "^\+.*version\s*="
# MUST show new version

# DAOs registered in database
grep "abstract fun.*Dao" app/src/main/java/com/mawaai/love/app/data/database/MawaaiDatabase.kt | wc -l
# MUST increase by 3 (templateDao, projectDao, productMockupDao)

# Hilt provides DAOs
grep "TemplateDao\|ProjectDao\|ProductMockupDao" \
    app/src/main/java/com/mawaai/love/app/di/DatabaseModule.kt | wc -l
# MUST >= 3

# No fallbackToDestructiveMigration
git diff HEAD~1 | grep "fallbackToDestructiveMigration"
# MUST be empty
```

### E8.MT-041 — Migrations

```bash
ls -la app/src/main/java/com/mawaai/love/app/data/database/Migrations.kt
# MUST exist

# Migration registered
grep "addMigrations(" app/src/main/java/com/mawaai/love/app/di/DatabaseModule.kt
# MUST show match

# Schema files exported
ls -la app/schemas/com.mawaai.love.app.data.database.MawaaiDatabase/
# New <version>.json file MUST appear after build

# No DROP TABLE
git diff HEAD~1 | grep "DROP TABLE"
# MUST be empty

# Build PASS (schema export reflects new tables)
./gradlew assembleDebug
```

### E8.MT-042 — Repositories

```bash
ls -la app/src/main/java/com/mawaai/love/app/data/repository/
# Expected: TemplateRepository.kt + impl, ProjectRepository.kt + impl, ProductMockupRepository.kt + impl

ls -la app/src/main/java/com/mawaai/love/app/data/storage/ProjectFileStorage.kt
# MUST exist

# Repositories return Flow
grep "fun observe.*Flow" app/src/main/java/com/mawaai/love/app/data/repository/*.kt | wc -l
# MUST >= 3

# Hilt binds the interfaces
grep "@Binds\|@Provides" app/src/main/java/com/mawaai/love/app/di/RepositoryModule.kt 2>/dev/null \
    || grep "Repository" app/src/main/java/com/mawaai/love/app/di/DatabaseModule.kt
# MUST show match
```

---

## STAGE 3 — Surface Intelligence

### E1.MT-015 — SurfaceProfile sealed hierarchy

```bash
ls -la app/src/main/java/com/mawaai/love/app/design/ai/intelligence/
# Expected: SurfaceProfile.kt, SurfaceCatalog.kt, SurfaceDirections.kt, TemplateIntelligencePrompt.kt

# 12 data objects exist
grep -c "data object" \
    app/src/main/java/com/mawaai/love/app/design/ai/intelligence/SurfaceProfile.kt
# MUST == 12

# SurfaceCatalog.byId for each id returns non-null
./gradlew test --tests "*SurfaceCatalogTest"
# MUST PASS
```

### E1.MT-017 — TemplateAssetManager wiring

```bash
# Find TemplateAssetManager
TAM=$(find app/src/main/java -name TemplateAssetManager.kt)
echo "$TAM"

# Confirm it calls SurfaceCatalog
grep "SurfaceCatalog\.forTemplate" "$TAM"
# MUST show match
```

---

## STAGE 4 — Structured Analysis

### E2.MT-018 — SketchAnalysis data classes

```bash
ls app/src/main/java/com/mawaai/love/app/design/ai/analysis/SketchAnalysis.kt
ls app/src/test/java/com/mawaai/love/app/design/ai/analysis/SketchAnalysisTest.kt

# All nested types present
grep -c "data class\|enum class" \
    app/src/main/java/com/mawaai/love/app/design/ai/analysis/SketchAnalysis.kt
# MUST >= 8 (Symmetry, LineQuality, Composition, SketchStructure, TemplateMapping,
#            TemplateFit, Finding, NormalizedRect, Severity)

# Init blocks enforce schema bounds
grep -c "init {" \
    app/src/main/java/com/mawaai/love/app/design/ai/analysis/SketchAnalysis.kt
# MUST >= 4

# Tests cover boundary + max-12-findings
./gradlew test --tests "*SketchAnalysisTest"
# MUST PASS
```

### E2.MT-019 — StructuredAnalysisClient

```bash
ls app/src/main/java/com/mawaai/love/app/design/ai/analysis/StructuredAnalysisClient.kt

# Uses gateway
grep "ProviderRegistry\|activeVisionChain" \
    app/src/main/java/com/mawaai/love/app/design/ai/analysis/StructuredAnalysisClient.kt
# MUST show match

# Strips markdown
grep "removeSurrounding\|trim" \
    app/src/main/java/com/mawaai/love/app/design/ai/analysis/StructuredAnalysisClient.kt
# MUST show match
```

### E2.MT-020 — Heuristic fallback

```bash
ls app/src/main/java/com/mawaai/love/app/design/ai/analysis/FallbackAnalysis.kt

# Has the 2 default findings
grep -c "fallback-preserve-composition\|fallback-surface-fit" \
    app/src/main/java/com/mawaai/love/app/design/ai/analysis/FallbackAnalysis.kt
# MUST == 2

# StructuredAnalysisClient calls FallbackAnalysis on schema-validation failure
grep "FallbackAnalysis\.build" \
    app/src/main/java/com/mawaai/love/app/design/ai/analysis/StructuredAnalysisClient.kt
# MUST show match
```

### E2.MT-021 — Persist to Room

```bash
ls app/src/main/java/com/mawaai/love/app/design/ai/analysis/AnalysisOrchestrator.kt
ls app/src/main/java/com/mawaai/love/app/ui/design/analysis/AnalysisViewModel.kt

# Orchestrator calls repository
grep "projectRepository\.saveAnalysis\|projectRepository\.setStatus" \
    app/src/main/java/com/mawaai/love/app/design/ai/analysis/AnalysisOrchestrator.kt
# MUST show >= 3 (analyzing, analyzed, saveAnalysis)
```

---

## 🛡 Universal Red-Flag Reject Rules

Apply to every MT. Reject the AI's output if ANY of these fire:

1. New dependency added without explicit MT authorization → `git diff HEAD~1 app/build.gradle.kts | grep "implementation\|api\(.*libs\."`
2. `Map<String, Any>` introduced → `git diff HEAD~1 | grep "Map<String,\s*Any>"`
3. API key leaked → `git diff HEAD~1 | grep -E "AIza[0-9A-Za-z_-]{20,}|hf_[A-Za-z0-9]{30,}|sk-or-v1-[a-f0-9]{40,}|gsk_[A-Za-z0-9]{30,}"`
4. Production class touched outside MT scope → cross-reference with `git diff --stat`
5. `fallbackToDestructiveMigration` → `git diff HEAD~1 | grep fallbackToDestructive`
6. Output missing required sections (Phase Header, Context Budget, Diagnostic Summary, etc.)
7. AI says "I'll also fix..." or "While I'm here..." → out-of-scope refactor
8. New `@OptIn(...)` added to silence a warning → silencing instead of fixing
9. `lateinit var` introduced outside DI/lifecycle
10. `runBlocking` in non-test code

---

## 📝 Tracking Log Template

Copy this into a notebook or sticky file:

```
2026-05-XX  E7.MT-036  sha=<…>  build✅ test✅ secrets✅ scope✅
2026-05-XX  E7.MT-037  sha=<…>  build✅ test✅ secrets✅ scope✅
2026-05-XX  E7.MT-038  sha=<…>  ...
2026-05-XX  E7.MT-039  sha=<…>  ...
2026-05-XX  E8.MT-040  sha=<…>  ...
...
```

When STAGE 9 (E6.MT-035) is committed and verified, tag:
```bash
git tag -a v2.0.0-rc1 -m "Creative Studio integration complete"
git push origin v2.0.0-rc1
```
