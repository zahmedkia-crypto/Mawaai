# PROMPTS — Ready-To-Paste Per-MT Prompts

Each prompt is self-contained — paste verbatim into the downstream agent after `ai_handoff/KICKOFF.md` has been loaded.

**Execution order:** STAGE 1 → STAGE 9 (see `INTEGRATION_PLAN.md`).

---

## STAGE 1 — Multi-Provider AI Gateway

### E7.MT-036 — VisionProvider sealed registry + FallbackChain

```
E7.MT-036: Build the multi-provider AI gateway foundation.

CONTEXT
Today the app crashes when Gemini returns HTTP 404 (gemini-1.5 deprecation). MT-014
fixed the immediate symptom by pinning to gemini-2.0-flash, but the architectural
fix is a provider abstraction with automatic fallback. Read
integration/AI_PROVIDER_GATEWAY.md FULL TEXT before producing any diff.

YOUR TASK
Create three new files implementing the abstraction.

1. design/ai/gateway/AiProvider.kt
   - sealed interface VisionProvider { id: ProviderId; isConfigured: Boolean;
     suspend fun visionAnalyze(prompt: String, image: Bitmap): Result<String> }
   - sealed interface TextProvider { id: ProviderId; isConfigured: Boolean;
     suspend fun generateText(prompt: String, systemPrompt: String? = null): Result<String> }
   - enum class ProviderId { GEMINI, OPENROUTER, GROQ, CLOUDFLARE_WORKERS_AI, HUGGINGFACE }
     with displayName + freeTier properties.
   - sealed class ProviderRecoverableError: NotFound, RateLimited, ServiceUnavailable,
     Timeout, QuotaExhausted (each takes a message).
   - sealed class ProviderFatalError: InvalidKey, MalformedRequest, SafetyBlock.

2. design/ai/gateway/FallbackChain.kt
   - class FallbackChain(providers: List<VisionProvider>) with
     suspend fun visionAnalyze(prompt, image): Result<String>.
   - Iterate providers, skip unconfigured, try each, on ProviderRecoverableError
     continue, on ProviderFatalError stop and return the error, on success return.
   - Log each attempt with Android Log (tag "FallbackChain").
   - If all fail, return Result.failure with aggregated error summary.

3. design/ai/gateway/ProviderRegistry.kt
   - @Singleton with @Inject constructor taking all 5 providers + a settings DataStore.
   - suspend fun activeVisionChain(): FallbackChain that reads user preferences
     (mode: AUTO or pinned-to-provider; order: List<ProviderId>) and returns
     the assembled chain.
   - suspend fun setMode(mode: String) and suspend fun setOrder(order: List<ProviderId>).

4. Wire DI: add a @Module if needed to provide the existing GeminiClient / OpenRouterClient
   as VisionProvider implementations. Add stub classes for Groq / Cloudflare / HF
   that return ProviderFatalError.InvalidKey until their full clients ship in MT-037/038.

FILES TO READ
- integration/AI_PROVIDER_GATEWAY.md (full design)
- app/src/main/java/com/mawaai/love/app/design/ai/gemini/GeminiClient.kt
- app/src/main/java/com/mawaai/love/app/design/ai/openrouter/OpenRouterClient.kt
- app/src/main/java/com/mawaai/love/app/di/CoroutineScopesModule.kt
- app/src/main/java/com/mawaai/love/app/di/DataStoreModule.kt (find SettingsDataStore)

FILES YOU MAY MODIFY
- (create) design/ai/gateway/AiProvider.kt
- (create) design/ai/gateway/FallbackChain.kt
- (create) design/ai/gateway/ProviderRegistry.kt
- (create) di/GatewayModule.kt (if needed for DI)

ANTI-PATTERNS
- Do NOT modify GeminiClient or OpenRouterClient (they remain working as-is).
- Do NOT add a real Groq/CF/HF client here — those are MT-037, MT-038. Stub only.
- Do NOT introduce a new dependency.
- Do NOT use Map<String, Any> anywhere.

OUTPUT FORMAT
Follow Required Output Format from MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug    PASS
- ./gradlew test             PASS
- Unit tests for FallbackChain (5 cases listed in AI_PROVIDER_GATEWAY.md)

NEXT
E7.MT-037: GroqClient (Llama 3.2 90B Vision).
```

---

### E7.MT-037 — GroqClient (Llama 3.2 90B Vision)

```
E7.MT-037: Add Groq Cloud as the fastest free vision provider.

CONTEXT
Groq's llama-3.2-90b-vision-preview is currently the fastest free vision API
(~400ms average). Adding it to the FallbackChain (MT-036) gives the user a
high-quality fallback when Gemini quota is exhausted.

YOUR TASK
1. design/ai/groq/GroqApi.kt — Retrofit interface for /openai/v1/chat/completions
   with @Header Authorization + @Body GroqChatRequest, returning GroqChatResponse.

2. design/ai/groq/GroqDtos.kt — OpenAI-compatible chat completion DTOs.
   - GroqChatRequest with model, messages, max_tokens, temperature.
   - Message has role + content (List<Content>).
   - Content is sealed: Text(text) and ImageUrl(imageUrl) with @SerializedName("type").
   - GroqChatResponse with choices[] + optional error.

3. design/ai/groq/GroqVisionProvider.kt — @Singleton @Inject class implementing
   VisionProvider (from MT-036).
   - id = ProviderId.GROQ
   - isConfigured reads BuildConfig.GROQ_API_KEY
   - visionAnalyze: encode bitmap to JPEG base64 on Dispatchers.Default,
     call api on Dispatchers.IO, parse response, return Result.
   - Translate HTTP errors to typed gateway errors: 404 -> NotFound,
     429 -> RateLimited, 401 -> InvalidKey, 503 -> ServiceUnavailable, etc.
   - Model constant: "llama-3.2-90b-vision-preview". Comment with audit date.

4. design/ai/groq/GroqTextProvider.kt — same pattern but implementing TextProvider.
   - Model constant: "llama-3.1-70b-versatile" (Groq's flagship text model).

5. app/build.gradle.kts — add buildConfigField for GROQ_API_KEY (read from
   local.properties, default "").

6. di/GatewayModule.kt — update ProviderRegistry binding to inject these.

7. Add Groq instructions to integration/AI_PROVIDER_GATEWAY.md note section about
   getting a free key at https://console.groq.com/keys.

FILES TO READ
- design/ai/gateway/AiProvider.kt (just created in MT-036)
- design/ai/openrouter/OpenRouterClient.kt (reference pattern)
- app/build.gradle.kts (where to add buildConfigField)

FILES YOU MAY MODIFY
- (create) design/ai/groq/{GroqApi,GroqDtos,GroqVisionProvider,GroqTextProvider}.kt
- app/build.gradle.kts (1 line addition)
- di/GatewayModule.kt (update binding)

ANTI-PATTERNS
- Do NOT change GeminiClient or OpenRouterClient.
- Do NOT hard-code the API key. Only BuildConfig.GROQ_API_KEY.
- Do NOT call the API in a non-suspend context.

OUTPUT FORMAT
Required Output Format from MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug    PASS
- ./gradlew test             PASS
- BuildConfig.GROQ_API_KEY field exists in generated BuildConfig.java
- ProviderRegistry.activeVisionChain() includes Groq when GROQ_API_KEY is set

NEXT
E7.MT-038: Cloudflare Workers AI vision (LLaVA-1.5).
```

---

### E7.MT-038 — Cloudflare Workers AI vision (LLaVA-1.5)

```
E7.MT-038: Add Cloudflare Workers AI as the most resilient vision fallback.

CONTEXT
Cloudflare's @cf/llava-hf/llava-1.5-7b-hf runs on CF's edge network — when
Google/Groq are down, CF is usually still up. Lower model quality (7B params)
but battle-hardened uptime.

YOUR TASK
1. Find the existing CloudflareWorkersAiClient (it already exists for text).
   Search for it in design/ai/cloudflare/ or similar.

2. Add a vision method to it OR create a new CloudflareVisionProvider that
   reuses the same Retrofit interface.
   - Endpoint: /client/v4/accounts/{accountId}/ai/run/@cf/llava-hf/llava-1.5-7b-hf
   - Body: { prompt, image (base64) } — verify by reading current CF text body shape.
   - Headers: Bearer ${BuildConfig.CLOUDFLARE_API_TOKEN}.

3. Wrap it in the VisionProvider interface (id = ProviderId.CLOUDFLARE_WORKERS_AI).
   Implement isConfigured = BuildConfig.CLOUDFLARE_ACCOUNT_ID.isNotBlank() &&
   BuildConfig.CLOUDFLARE_API_TOKEN.isNotBlank().

4. Update di/GatewayModule.kt to inject the new provider into ProviderRegistry.

FILES TO READ
- design/ai/gateway/AiProvider.kt
- Whatever CloudflareWorkersAi*.kt file currently exists (find via grep)

FILES YOU MAY MODIFY
- The existing Cloudflare client file (add a vision method)
  OR
- (create) design/ai/cloudflare/CloudflareVisionProvider.kt
- di/GatewayModule.kt

ANTI-PATTERNS
- Do NOT change the existing text-generation behaviour.
- Do NOT modify the BuildConfig fields (CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN
  already exist from MT-011).

OUTPUT FORMAT
Required Output Format from MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug    PASS
- ./gradlew test             PASS
- ProviderRegistry includes Cloudflare in chain when keys are set

NEXT
E7.MT-039: Provider switcher UI in Settings.
```

---

### E7.MT-039 — Provider switcher UI

```
E7.MT-039: Add the user-facing AI provider switcher in Settings.

CONTEXT
The user wants to manually pick which provider runs (or keep Auto Fallback).
This is the visible payoff of E7: a Settings screen showing all 5 providers,
their key-configured status, the current order, and a "Run health check" button.

YOUR TASK
1. ui/settings/AiProviderSettings.kt — a @Composable screen.
   - Section 1: Radio buttons — "Auto fallback (recommended)" vs "Use X only"
     for each ProviderId.
   - Section 2 (only visible when Auto is selected): Reorderable list of providers
     showing each one's key-configured indicator (green dot / red dot) and the
     order can be dragged.
   - Section 3: Diagnostics — "Run live health check" button that runs each
     configured provider's visionAnalyze() with a tiny test image; show
     PASS/FAIL + latency.

2. ui/settings/AiProviderSettingsViewModel.kt — @HiltViewModel reading
   ProviderRegistry state, exposing a StateFlow<AiProviderUiState>.

3. Hook into the existing SettingsScreen — add a new section/row that navigates
   to AiProviderSettings.

4. DataStore: ensure preferences keys `ai_provider_mode` and `ai_provider_order`
   are wired through SettingsDataStore.

FILES TO READ
- ui/settings/SettingsScreen.kt (or whatever the current settings file is)
- Existing SettingsViewModel
- design/ai/gateway/ProviderRegistry.kt
- core/preferences/SettingsDataStore.kt

FILES YOU MAY MODIFY
- (create) ui/settings/AiProviderSettings.kt
- (create) ui/settings/AiProviderSettingsViewModel.kt
- Existing settings screen / nav graph (add the new entry)

ANTI-PATTERNS
- Do NOT add new dependencies (use Material 3 components already in the project).
- Do NOT show real API key values anywhere in the UI — only configured/not.
- Do NOT block on the health check — it runs in viewModelScope.

OUTPUT FORMAT
Required Output Format from MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug    PASS
- ./gradlew test             PASS
- Run app: navigate to Settings -> AI Provider; selection persists across launches

NEXT
STAGE 2 — E8.MT-040: Room entities for Project, Template, ProductMockup.
```

---

## STAGE 2 — Room Entities (Data Model)

### E8.MT-040 — Room entities + DAOs

```
E8.MT-040: Create Room entities and DAOs for the Creative Studio data model.

CONTEXT
Read integration/DATA_MODEL.md FULL TEXT first — it has the complete entity
definitions, DAO contracts, and seed data.

YOUR TASK
Create these entities exactly as specified in DATA_MODEL.md:

1. data/database/entities/TemplateEntity.kt + Template (domain) + TemplateCategory enum
2. data/database/entities/ProjectEntity.kt + ProjectStatus enum
3. data/database/entities/ProductMockupEntity.kt
4. data/database/entities/TemplateZone, CulturalRules, LightingProfile (domain types)

Create these DAOs:
5. data/dao/TemplateDao.kt — observeAll, observeByCategory, byId, upsertAll, deleteAll
6. data/dao/ProjectDao.kt — observeAll, observe(id), byId, upsert, setStatus,
   setAnalysis, setSuggestions, setRender, setColorOverride, setAcceptedSuggestions, delete
7. data/dao/ProductMockupDao.kt — observeByCategory, observeForSurface, byId, seed

Update:
8. data/database/MawaaiDatabase.kt — bump database version, add @TypeConverters for
   Instant, register the new DAOs (templateDao, projectDao, productMockupDao).
9. di/DatabaseModule.kt — provide the new DAOs.

FILES TO READ
- integration/DATA_MODEL.md (FULL — has every entity definition)
- data/database/MawaaiDatabase.kt (current version, current converters)
- data/database/Converters.kt
- di/DatabaseModule.kt

FILES YOU MAY MODIFY
- (create) data/database/entities/{TemplateEntity, ProjectEntity, ProductMockupEntity}.kt
- (create) data/database/entities/Domain.kt (Template, TemplateZone, CulturalRules, LightingProfile types)
- (create) data/dao/{TemplateDao, ProjectDao, ProductMockupDao}.kt
- data/database/MawaaiDatabase.kt (bump version, register)
- di/DatabaseModule.kt (provide new DAOs)

ANTI-PATTERNS
- Do NOT use fallbackToDestructiveMigration() — would wipe existing user data.
- Do NOT migrate yet — MT-041 owns that.
- Do NOT use Map<String, Any> in any entity or domain type.
- Do NOT add new dependencies.

OUTPUT FORMAT
Required Output Format from MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug    EXPECTED TO FAIL because version was bumped
  without a migration. That's the bridge to MT-041.

NEXT
E8.MT-041: Add migrations.
```

---

### E8.MT-041 — Non-destructive migrations

```
E8.MT-041: Add Room migrations for the new entities introduced in MT-040.

YOUR TASK
1. data/database/Migrations.kt — define MIGRATION_<oldVersion>_TO_<newVersion>
   exactly as specified in integration/DATA_MODEL.md "Migration Strategy" section.

2. MawaaiDatabase.kt — register the migration in addMigrations() during the
   Room.databaseBuilder() in DatabaseModule.

3. app/schemas/ — Room will auto-generate the new schema JSON when the build runs.

4. Verify the existing user data tables (Memory, LoveLetter, Mood, etc.) are
   NOT touched by this migration. Add a comment in Migrations.kt confirming this.

FILES TO READ
- integration/DATA_MODEL.md (Migration Strategy section)
- data/database/MawaaiDatabase.kt (current)
- di/DatabaseModule.kt
- app/schemas/com.mawaai.love.app.data.database.MawaaiDatabase/ (existing schemas)

FILES YOU MAY MODIFY
- (create) data/database/Migrations.kt
- di/DatabaseModule.kt (add migration to builder chain)

ANTI-PATTERNS
- Do NOT add ALTER TABLE to existing tables (Memory, LoveLetter, etc.).
- Do NOT drop or rename anything.
- Do NOT use fallbackToDestructiveMigration anywhere.

OUTPUT FORMAT
Required Output Format from MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug    PASS
- ./gradlew test             PASS
- app/schemas/.../V<new>.json exists and contains the new tables

NEXT
E8.MT-042: Repositories.
```

---

### E8.MT-042 — Repositories

```
E8.MT-042: Create the repository layer for the new entities.

YOUR TASK
1. data/repository/TemplateRepository.kt — interface + impl. observeAll(),
   observeByCategory(), byId(). Reads from TemplateDao + AssetManager bundled
   templates if needed.

2. data/repository/ProjectRepository.kt — interface + impl. Methods as listed in
   integration/DATA_MODEL.md "Repository Contract" section (observe, observeAll,
   create, saveSketch, saveAnalysis, saveSuggestions, acceptSuggestions,
   setColorOverride, saveRender, saveExport, delete).

3. data/repository/ProductMockupRepository.kt — interface + impl. observeByCategory,
   observeForSurface, byId, seed.

4. data/storage/ProjectFileStorage.kt — helper as shown in DATA_MODEL.md.

5. Hilt: provide repositories in DatabaseModule (or a new RepositoryModule).

FILES TO READ
- integration/DATA_MODEL.md (Repository Contract + ProjectFileStorage)
- The DAOs from MT-040

FILES YOU MAY MODIFY
- (create) data/repository/{Template,Project,ProductMockup}Repository.kt + impls
- (create) data/storage/ProjectFileStorage.kt
- di/DatabaseModule.kt (or new RepositoryModule.kt) — provide repositories

VERIFICATION
- ./gradlew assembleDebug    PASS
- ./gradlew test             PASS
- Unit test: TemplateRepository.observeAll() emits the seeded templates

NEXT
STAGE 3 — E1.MT-015: SurfaceProfile sealed class hierarchy.
```

---

## STAGE 3 — Surface Intelligence Catalog

### E1.MT-015 — SurfaceProfile sealed class hierarchy

```
E1.MT-015: Port the 12-surface catalog to Kotlin.

CONTEXT
Read integration/SURFACE_PROFILES.md FULL TEXT — it has the complete sealed
interface, all 12 data objects, and the SurfaceCatalog resolver.

YOUR TASK
1. design/ai/intelligence/SurfaceProfile.kt — sealed interface + 12 data objects
   (SkinPalm, SkinHandFull, SkinFoot, FabricAbaya, FabricThobe, FabricToub,
   WallStone, WallPlaster, WallArch, CeramicPlate, CeramicTile, CeramicMug).
   Each with id, label, targetSurface, constraints[], maskingRules[],
   perspectiveRules[], materialResponse.

2. design/ai/intelligence/SurfaceCatalog.kt — object with byId(id), forTemplate(entity)
   matching the TS resolveTemplateSurface() heuristics exactly.

3. design/ai/intelligence/SurfaceDirections.kt — object with forProfile(profile)
   returning the verbatim render-prompt string for each of the 12 surfaces.
   Plus QUALITY_TAIL constant.

4. design/ai/intelligence/TemplateIntelligencePrompt.kt — top-level fun
   templateIntelligencePrompt(template: TemplateEntity): String — the Phase 2
   prompt block as shown in SURFACE_PROFILES.md.

FILES TO READ
- integration/SURFACE_PROFILES.md (FULL — Kotlin code already written, paste verbatim)
- TemplateEntity from MT-040

FILES YOU MAY MODIFY
- (create) design/ai/intelligence/SurfaceProfile.kt
- (create) design/ai/intelligence/SurfaceCatalog.kt
- (create) design/ai/intelligence/SurfaceDirections.kt
- (create) design/ai/intelligence/TemplateIntelligencePrompt.kt

VERIFICATION
- ./gradlew assembleDebug    PASS
- ./gradlew test             PASS
- Unit tests: SurfaceCatalog.byId("skin_palm") == SurfaceProfile.SkinPalm
- Unit tests: SurfaceCatalog.forTemplate(<henna+palm name>) == SkinPalm

NEXT
E1.MT-016: (already covered above)
E1.MT-017: TemplateAssetManager wiring.
```

---

### E1.MT-017 — TemplateAssetManager wiring

```
E1.MT-017: Wire TemplateAssetManager to use SurfaceCatalog when loading templates.

YOUR TASK
Modify the existing TemplateAssetManager so that when it builds a Template
domain object from disk (assets/templates/*.json + image), it sets
surfaceType = SurfaceCatalog.forTemplate(entity).id.

That's it — pure wiring, no new behaviour.

FILES TO READ
- Find TemplateAssetManager.kt in the codebase (search for the class)
- design/ai/intelligence/SurfaceCatalog.kt (from MT-015)

FILES YOU MAY MODIFY
- ONLY TemplateAssetManager.kt

VERIFICATION
- Existing template loading still works (no template invisibly disappears)
- Loaded templates now have a non-empty surfaceType matching a SurfaceProfile.id

NEXT
STAGE 4 — E2.MT-018: SketchAnalysis data class hierarchy.
```

---

## STAGE 4 — Structured Vision Analysis (Phase 3)

### E2.MT-018 — SketchAnalysis data class hierarchy

```
E2.MT-018: Port the Zod analysisSchema to Kotlin data classes.

YOUR TASK
1. design/ai/analysis/SketchAnalysis.kt — top-level data class with nested types
   exactly matching the schema in integration/PIPELINE_ARCHITECTURE.md "Phase 3"
   section.
   - Outer fields: artStyle, culturalOrigin, symmetry, lineQuality, composition,
     sketchStructure, templateMapping, templateFit, findings.
   - Nested: Symmetry, LineQuality, Composition, SketchStructure, TemplateMapping,
     TemplateFit, Finding.
   - Finding.Severity enum: INFO, WARNING, CRITICAL.
   - NormalizedRect value class with init { require(...) } bounds.
   - findings list capped via init { require(findings.size <= 12) }.
   - All @SerializedName mappings for snake_case JSON keys.

2. Unit tests in app/src/test/.../analysis/SketchAnalysisTest.kt covering
   boundary values and the max-12-findings constraint.

FILES TO READ
- integration/PIPELINE_ARCHITECTURE.md (Phase 3 section)
- integration/MIGRATION_BLUEPRINT.md (Zod -> Kotlin pattern)

FILES YOU MAY MODIFY
- (create) design/ai/analysis/SketchAnalysis.kt
- (create) app/src/test/java/com/mawaai/love/app/design/ai/analysis/SketchAnalysisTest.kt

VERIFICATION
- ./gradlew assembleDebug    PASS
- ./gradlew test             PASS (boundary tests pass)
- gson.fromJson(<sample json>, SketchAnalysis::class.java) round-trips

NEXT
E2.MT-019: StructuredAnalysisClient.
```

---

### E2.MT-019 — StructuredAnalysisClient

```
E2.MT-019: Build the AI client that produces a SketchAnalysis from a sketch image.

YOUR TASK
1. design/ai/analysis/StructuredAnalysisClient.kt — @Singleton @Inject class.
   - Constructor: ProviderRegistry, Gson.
   - suspend fun analyze(sketchBitmap, template): Result<SketchAnalysis>.
   - Build the user prompt: TEMPLATE PROFILE + templateIntelligencePrompt(template)
     + PHASE 3 SKETCH ANALYSIS instructions + the schema description in plain text.
     Use the system prompt from analysis.functions.ts line 250 verbatim (port to
     Kotlin string).
   - Call gateway.activeVisionChain().visionAnalyze(prompt, sketchBitmap).
   - Strip markdown code fences from response.
   - Parse via gson.fromJson(); validate with the data class init blocks.
   - On parse failure or empty response, return Result.failure of a typed
     SchemaValidationException so the caller can decide to use the fallback.

FILES TO READ
- integration/MIGRATION_BLUEPRINT.md (StructuredAnalysisClient pattern)
- design/ai/gateway/ProviderRegistry.kt (from MT-036)
- design/ai/intelligence/TemplateIntelligencePrompt.kt (from MT-015)
- design/ai/analysis/SketchAnalysis.kt (from MT-018)
- Source reference (for prompt copy): analysis.functions.ts in source repo

FILES YOU MAY MODIFY
- (create) design/ai/analysis/StructuredAnalysisClient.kt

VERIFICATION
- ./gradlew assembleDebug    PASS
- ./gradlew test             PASS
- Manual: run a smoke test with a real sketch image; analysis JSON parses

NEXT
E2.MT-020: Heuristic fallback.
```

---

### E2.MT-020 — Heuristic fallback analysis

```
E2.MT-020: Build the deterministic fallback for when AI returns invalid JSON.

YOUR TASK
1. design/ai/analysis/FallbackAnalysis.kt — object with
   fun build(template: TemplateEntity, reason: String): SketchAnalysis.
   - Mirror the buildFallbackAnalysis() function from analysis.functions.ts line 95.
   - Use SurfaceCatalog.forTemplate to get profile.
   - Return a SketchAnalysis with neutral mid-range numeric values (accuracy_pct=72,
     line_quality=7s, balance=7, negative_space=34, scale_match=7, etc.)
   - Two default findings: "fallback-preserve-composition" and "fallback-surface-fit".

2. Update StructuredAnalysisClient (MT-019): if analyze returns Result.failure
   with a SchemaValidationException, return Result.success(FallbackAnalysis.build(...)).
   Log a warning with the reason.

FILES TO READ
- analysis.functions.ts in source repo (lines 95-155)
- design/ai/intelligence/SurfaceCatalog.kt
- design/ai/analysis/SketchAnalysis.kt

FILES YOU MAY MODIFY
- (create) design/ai/analysis/FallbackAnalysis.kt
- design/ai/analysis/StructuredAnalysisClient.kt (add fallback branch)

VERIFICATION
- ./gradlew test             PASS (new unit test: invalid AI response → fallback)
- Manual: corrupt the AI response (e.g., disable network); analysis returns
  fallback instead of crashing

NEXT
E2.MT-021: Persist analysis to Room.
```

---

### E2.MT-021 — Persist analysis to Room

```
E2.MT-021: Wire the analysis pipeline to persist results via ProjectRepository.

YOUR TASK
1. Find or create the AnalysisOrchestrator (a use case that ties together
   StructuredAnalysisClient + ProjectRepository).

2. In the orchestrator:
   - projectRepository.setStatus(projectId, "analyzing")
   - val sketch = projectFileStorage.sketchFile(projectId).readBitmap()
   - val template = templateRepository.byId(project.templateId)
   - val analysis = structuredAnalysisClient.analyze(sketch, template)
   - On success: projectRepository.saveAnalysis(projectId, analysis); status=analyzed
   - On fallback success: same, but log a warning so we can audit fallback rates
   - On hard failure: projectRepository.setStatus(projectId, "failed")

3. Add @HiltViewModel for the analysis screen that observes
   projectRepository.observe(projectId) and triggers the orchestrator.

FILES TO READ
- data/repository/ProjectRepository.kt (from MT-042)
- design/ai/analysis/StructuredAnalysisClient.kt
- Existing design studio orchestrator if any

FILES YOU MAY MODIFY
- (create) design/ai/analysis/AnalysisOrchestrator.kt
- (create) ui/design/analysis/AnalysisViewModel.kt

VERIFICATION
- ./gradlew test             PASS
- Run app: sketch -> analyze -> analysis_json column in projects table is populated

NEXT
STAGE 5 — E3.MT-022/023/024/025: Suggestions system.
```

---

> **Note:** Prompts for STAGE 5 (E3), STAGE 6 (E4), STAGE 7 (E5), STAGE 8 (E9), STAGE 9 (E6) follow the exact same structure. The downstream agent should ask "next prompt please" after completing one, and the human pastes the next block.
>
> For STAGE 5+, build prompts by referring to:
> - `integration/INTEGRATION_PLAN.md` for scope per MT
> - `integration/PIPELINE_ARCHITECTURE.md` for type contracts
> - `integration/MIGRATION_BLUEPRINT.md` for TS→Kotlin patterns
> - Source TypeScript files in the Creative Studio archive
>
> Each prompt MUST follow the template seen above:
> 1. CONTEXT
> 2. YOUR TASK (numbered steps)
> 3. FILES TO READ
> 4. FILES YOU MAY MODIFY
> 5. ANTI-PATTERNS
> 6. OUTPUT FORMAT
> 7. VERIFICATION
> 8. NEXT

---

## 🔑 Universal Prompt Header (Add To Every MT Prompt)

If the downstream agent ever loses context (chat history truncated, etc.), the human can re-prime it by pasting `ai_handoff/KICKOFF.md` again. After that, paste any MT prompt; the agent will resume cleanly.
