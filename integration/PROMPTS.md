# INTEGRATION MICRO-TASK PROMPTS

Ready-to-paste prompts for every MT in `INTEGRATION_PLAN.md`. Each is self-contained — the downstream agent can execute them in order after loading `ai_handoff/KICKOFF.md`.

**Order:**
1. STAGE 1 — E7.MT-036 → MT-039 (Gateway)
2. STAGE 2 — E8.MT-040 → MT-042 (Data model)
3. STAGE 3 — E1.MT-015 → MT-017 (Surface intelligence)
4. STAGE 4 — E2.MT-018 → MT-021 (Analysis)
5. STAGE 5 — E3.MT-022 → MT-025 (Suggestions)
6. STAGE 6 — E4.MT-026 → MT-029 (Render)
7. STAGE 7 — E5.MT-030 → MT-032 (Quality)
8. STAGE 8 — E9.MT-043 → MT-044 (Ceramic — independent, any order)
9. STAGE 9 — E6.MT-033 → MT-035 (Mockups)

---

## STAGE 1 — Multi-Provider Gateway

### MT-036 — VisionProvider sealed registry + FallbackChain

```text
MT-036: Build the multi-provider AI gateway foundation.

CONTEXT
The app currently calls Gemini directly. When Gemini deprecates a model (as
happened with gemini-1.5-flash → 404), the entire feature crashes. We need
a provider-agnostic gateway so the next deprecation is invisible to users.

Read these docs first:
- integration/AI_PROVIDER_GATEWAY.md  (FULL design with code samples)
- skills/ai-provider-gateway/SKILL.md (discipline rules)

YOUR TASK
1. Create the gateway package:
   app/src/main/java/com/mawaai/love/app/design/ai/gateway/
       AiProvider.kt          (sealed interfaces + ProviderId enum + error types)
       FallbackChain.kt       (chain executor)
       ProviderRegistry.kt    (DI registry, reads order from DataStore)

2. Implementation MUST match the code shown in AI_PROVIDER_GATEWAY.md
   sections 'Sealed Interfaces', 'The Fallback Chain', and 'ProviderRegistry'.

3. ProviderRegistry depends on:
   - GeminiVisionProvider (NEW thin adapter wrapping existing GeminiVisionClient
     — does NOT modify GeminiVisionClient itself)
   - OpenRouterVisionProvider (NEW adapter wrapping existing OpenRouterClient)
   - GroqVisionProvider (MT-037, declare the type now, stub out)
   - CloudflareVisionProvider (MT-038, declare the type now, stub out)
   - SettingsDataStore (existing — verify path; if missing, NEW wrapper around
     androidx.datastore.preferences DataStore<Preferences>)

4. The two adapter classes that wrap existing clients:
   - design/ai/gateway/adapters/GeminiVisionProviderAdapter.kt
       @Singleton
       class GeminiVisionProviderAdapter @Inject constructor(
           private val client: GeminiVisionClient
       ) : VisionProvider { ... }
   - design/ai/gateway/adapters/OpenRouterVisionProviderAdapter.kt
       similar — wraps OpenRouterClient

   Adapters translate provider-specific errors (HttpException.code() == 404 etc.)
   into the typed ProviderRecoverableError / ProviderFatalError hierarchy.

5. For Groq and Cloudflare, create EMPTY adapter classes that return
   Result.failure(ProviderFatalError.InvalidKey("provider not implemented yet
   — MT-037/038")) — this lets the registry compile and ship behind a feature
   flag.

FILES TO READ
- app/src/main/java/com/mawaai/love/app/design/ai/gemini/GeminiVisionClient.kt
  (to see the existing client signature you're adapting)
- app/src/main/java/com/mawaai/love/app/design/ai/openrouter/OpenRouterClient.kt
- integration/AI_PROVIDER_GATEWAY.md

FILES YOU MAY MODIFY
- NEW: design/ai/gateway/*.kt + design/ai/gateway/adapters/*.kt
- NO existing production class may be modified.

FILES YOU MAY ADD UNIT TESTS FOR
- app/src/test/java/com/mawaai/love/app/design/ai/gateway/FallbackChainTest.kt
  (use the test suite shown in AI_PROVIDER_GATEWAY.md section 'How To Test')

ANTI-PATTERNS
- Do NOT modify GeminiVisionClient, GeminiClient, or OpenRouterClient. Adapters wrap.
- Do NOT add string-typed ProviderIds. Use the enum.
- Do NOT use Map<String, Any> for provider config. Each provider's config lives
  in BuildConfig fields.
- Do NOT make FallbackChain swallow ProviderFatalError. It must propagate.

OUTPUT FORMAT — per ai_handoff/MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug   (must PASS)
- ./gradlew test            (must PASS, including FallbackChainTest)
- git diff --stat must show ONLY new files in design/ai/gateway/ and test/
- grep -r "Map<String, Any>" design/ai/gateway/ (must be empty)

NEXT
MT-037 (Groq vision provider).
```

### MT-037 — GroqClient (Llama 3.2 Vision)

```text
MT-037: Wire Groq Cloud as a vision provider in the gateway.

CONTEXT
Groq is currently the fastest free vision provider. Llama 3.2 90B Vision
Preview is free on the Groq console (rate-limited but generous).
- API base: https://api.groq.com/openai/v1/chat/completions
- Auth: Bearer <GROQ_API_KEY>
- Endpoint shape: OpenAI-compatible chat completions with vision content parts
- Free key: https://console.groq.com/keys

YOUR TASK
1. Add BuildConfig field:
   - app/build.gradle.kts: insert immediately after OPENROUTER_API_KEY:
       buildConfigField("String", "GROQ_API_KEY", "\"${localProps.getProperty("GROQ_API_KEY") ?: ""}\"")

2. Create the Groq package:
   app/src/main/java/com/mawaai/love/app/design/ai/groq/
       GroqApi.kt           (Retrofit interface)
       GroqDtos.kt          (request + response DTOs, OpenAI-compatible)
       GroqClient.kt        (suspend visionAnalyze(prompt, image): Result<String>)

3. Replace the stub GroqVisionProviderAdapter from MT-036 with a real
   implementation that uses GroqClient. The adapter's visionAnalyze translates
   HttpException codes → typed gateway errors per the table in
   AI_PROVIDER_GATEWAY.md section 'Provider Implementations (one example)'.

4. Default model: "llama-3.2-90b-vision-preview"
   Image encoding: JPEG, quality=85, max dimension 1024 (downscale before base64)
   Content shape (per Groq docs):
       messages: [{
         role: "user",
         content: [
           { type: "text", text: <prompt> },
           { type: "image_url", image_url: { url: "data:image/jpeg;base64,..." } }
         ]
       }]

FILES TO READ
- app/build.gradle.kts (insertion point only)
- design/ai/gateway/AiProvider.kt (interface to satisfy)
- design/ai/gateway/adapters/GroqVisionProviderAdapter.kt (stub from MT-036)

FILES YOU MAY MODIFY
- app/build.gradle.kts (one buildConfigField line addition)
- design/ai/gateway/adapters/GroqVisionProviderAdapter.kt (replace stub)
- NEW: design/ai/groq/*.kt

ANTI-PATTERNS
- Do NOT log the API key.
- Do NOT log the base64 image payload (too big, can leak content).
- Do NOT add Groq-specific code paths outside the groq/ package or its adapter.
- Do NOT change the gateway interface.

OUTPUT FORMAT — per ai_handoff/MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug   (must PASS)
- ./gradlew test            (must PASS)
- grep "GROQ_API_KEY" app/build.gradle.kts (must show exactly 2 occurrences)
- grep "GroqClient" design/ai/gateway/adapters/GroqVisionProviderAdapter.kt
  (must show usage)

NEXT
MT-038 (Cloudflare Workers AI vision).
```

### MT-038 — Cloudflare Workers AI vision (LLaVA)

```text
MT-038: Extend the existing Cloudflare client with vision support, then wire
its provider adapter.

CONTEXT
The app already calls Cloudflare Workers AI for text (Llama 3.1) and SD 1.5
img2img. Cloudflare also offers LLaVA for vision:
  POST https://api.cloudflare.com/client/v4/accounts/{acct}/ai/run/@cf/llava-hf/llava-1.5-7b-hf
  Body shape varies — see https://developers.cloudflare.com/workers-ai/models/

YOUR TASK
1. Identify the existing Cloudflare client class (likely
   design/ai/cloudflare/CloudflareClient.kt or similar). Read its file in full.
2. Add a suspend function:
       suspend fun llavaVision(prompt: String, image: Bitmap): Result<String>
   that:
   - Reads BuildConfig.CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN
   - Encodes the bitmap as base64 PNG
   - POSTs to the LLaVA endpoint with the documented body shape
   - Returns the description string on success
3. Replace the MT-036 stub CloudflareVisionProviderAdapter with a real
   implementation that calls llavaVision and translates errors.

FILES TO READ
- The existing CloudflareClient.kt (find via grep -r "CLOUDFLARE_ACCOUNT_ID"
  app/src/main/java/)
- design/ai/gateway/adapters/CloudflareVisionProviderAdapter.kt (stub)

FILES YOU MAY MODIFY
- The existing CloudflareClient.kt (additive only — add new method)
- design/ai/gateway/adapters/CloudflareVisionProviderAdapter.kt (replace stub)

ANTI-PATTERNS
- Do NOT modify the existing Cloudflare text or SD methods.
- Do NOT log keys or base64 payloads.
- Do NOT introduce a new Retrofit instance — reuse the existing one if the
  client already has one.

OUTPUT FORMAT — per ai_handoff/MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug   (must PASS)
- ./gradlew test            (must PASS)

NEXT
MT-039 (Settings UI).
```

### MT-039 — Provider switcher Settings UI

```text
MT-039: Add an AI Provider section to the Settings screen with auto-fallback
mode, per-provider pin, drag-to-reorder chain, and live health check button.

YOUR TASK
1. Find the existing Settings screen Composable (grep for "SettingsScreen" or
   look at ui/navigation/NavGraph.kt for the `settings` route).
2. Add a new section `AiProviderSettings` Composable that follows the UI
   shown in AI_PROVIDER_GATEWAY.md section 'Settings Screen'.
3. The ViewModel `AiProviderSettingsViewModel` exposes:
       data class State(
           val mode: ProviderMode,
           val activeOrder: List<ProviderId>,
           val providerStatuses: Map<ProviderId, ProviderStatus>,
           val smokeTestResults: Map<ProviderId, SmokeTestResult> = emptyMap()
       )
   - setMode(ProviderMode) → persists to ProviderRegistry.setMode()
   - setOrder(List<ProviderId>) → persists to ProviderRegistry.setOrder()
   - runSmokeTest() → calls each configured provider with a small ping prompt,
     records latency + http status
4. The reorder UI can use a simple LongPressDraggable + LazyColumn (no new
   dependency required).

FILES TO READ
- Existing SettingsScreen.kt
- design/ai/gateway/ProviderRegistry.kt
- design/ai/gateway/AiProvider.kt

FILES YOU MAY MODIFY
- The existing SettingsScreen.kt (add a section, do NOT restructure)
- NEW: ui/settings/AiProviderSettings.kt
- NEW: ui/settings/AiProviderSettingsViewModel.kt

ANTI-PATTERNS
- Do NOT add a new Material 2 component — Material 3 only.
- Do NOT add a third-party drag-and-drop library — keep it stdlib.
- Do NOT log keys when displaying provider status; show "configured ✓" or
  "missing key" only.

OUTPUT FORMAT — per ai_handoff/MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug   (must PASS)
- ./gradlew test            (must PASS)
- Manual smoke test on emulator: open Settings → AI Provider section visible,
  toggling mode persists across app restart.

NEXT
MT-040 (Room entities).
```

---

## STAGE 2 — Data Model

### MT-040 — Room entities + DAOs

```text
MT-040: Add Template / Project / ProductMockup entities, DAOs, and update
MawaaiDatabase to v2.

YOUR TASK
1. Read integration/DATA_MODEL.md in full — copy entity definitions verbatim.
2. Create entity files at data/database/entities/.
3. Create DAO interfaces at data/dao/.
4. Update MawaaiDatabase.kt: bump version to 2; add new entities to @Database
   entities array; add abstract DAO accessors; register MawaaiTypeConverters.
5. Update DatabaseModule.kt: add @Provides for each new DAO.

DO NOT yet add migration — that's MT-041. For now, document in Kotlin comment
that this commit requires reinstall (fallbackToDestructiveMigration is OK at
this stage IF AND ONLY IF the app has no released users yet — confirm in
the PR description).

FILES TO READ
- integration/DATA_MODEL.md
- app/src/main/java/com/mawaai/love/app/data/database/MawaaiDatabase.kt
- app/src/main/java/com/mawaai/love/app/di/DatabaseModule.kt

FILES YOU MAY MODIFY
- MawaaiDatabase.kt (version bump + entity addition)
- DatabaseModule.kt (add new DAO providers)
- NEW: data/database/entities/{Template,Project,ProductMockup}Entity.kt
- NEW: data/dao/{Template,Project,ProductMockup}Dao.kt
- NEW: data/database/MawaaiTypeConverters.kt

OUTPUT FORMAT — per ai_handoff/MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug   (must PASS)
- ./gradlew test            (must PASS)
- ls app/schemas/com.mawaai.love.app.data.database.MawaaiDatabase/2.json (exists)

NEXT
MT-041 (migrations).
```

### MT-041 — Non-destructive v1→v2 migration

```text
MT-041: Replace destructive-migration shortcut from MT-040 with a real
Migration that preserves existing user data.

YOUR TASK
1. Read integration/DATA_MODEL.md section 'Migrations' — port the
   MIGRATION_1_2 object verbatim.
2. Create data/database/Migrations.kt with the migration object.
3. Update MawaaiDatabase.kt:
   - REMOVE any fallbackToDestructiveMigration() call from the
     databaseBuilder chain (added in MT-040 as a temporary measure)
   - ADD .addMigrations(*MawaaiMigrations.ALL)
4. Add a JVM test app/src/test/java/.../MigrationTest.kt that:
   - Creates a v1 database with seed data via SupportSQLiteDatabase
   - Runs MigrationTestHelper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2)
   - Asserts existing data preserved
   (See androidx.room:room-testing for the helper)

FILES TO READ
- integration/DATA_MODEL.md (Migrations section)
- MawaaiDatabase.kt (current state after MT-040)

FILES YOU MAY MODIFY
- MawaaiDatabase.kt (swap fallback for addMigrations)
- NEW: data/database/Migrations.kt
- NEW: test/.../MigrationTest.kt
- app/build.gradle.kts (add testImplementation libs.androidx.room.testing IF
  not already in version catalog — verify first)

OUTPUT FORMAT — per ai_handoff/MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug   (must PASS)
- ./gradlew test            (must PASS, including MigrationTest)
- grep "fallbackToDestructiveMigration" app/src/main/java/  (must be empty)

NEXT
MT-042 (repositories).
```

### MT-042 — Repositories

```text
MT-042: Build TemplateRepository, ProjectRepository, ProductMockupRepository
exposing Flow + suspend mutators.

YOUR TASK
1. Create data/repository/TemplateRepository.kt:
       fun observeAll(category: TemplateCategory?): Flow<List<Template>>
       suspend fun getById(id: String): Template?
       suspend fun seed(templates: List<TemplateEntity>)  // for asset loading
2. Create data/repository/ProjectRepository.kt:
       fun observe(id: String): Flow<Project?>
       fun observeAll(): Flow<List<Project>>
       suspend fun create(templateId: String, sketchPath: String): String  // returns id
       suspend fun saveAnalysis(id: String, analysis: SketchAnalysis)
       suspend fun saveSuggestions(id: String, suggestions: List<Suggestion>)
       suspend fun saveAcceptedSuggestionIds(id: String, ids: List<String>)
       suspend fun saveColorOverride(id: String, hex: String?)
       suspend fun saveRender(id: String, path: String, prompt: String, quality: RenderQuality)
       suspend fun saveExport(id: String, path: String, mockupId: String)
       suspend fun delete(id: String)
3. Create data/repository/ProductMockupRepository.kt:
       fun observeByCategory(category: TemplateCategory): Flow<List<ProductMockup>>
       suspend fun byId(id: String): ProductMockup?
       suspend fun seed()  // inserts MockupSeed.ALL if empty
4. Create a Hilt module providing these.

EACH repository MUST:
- Map entity ↔ domain types in private mapper functions (no entity types leak
  outside data/)
- Use Dispatchers.IO for any blocking I/O
- Wrap Gson serialization in try/catch with a defensive return

FILES TO READ
- The 3 new DAOs from MT-040
- The 3 new entities from MT-040
- integration/DATA_MODEL.md (for domain types reference)
- integration/PIPELINE_ARCHITECTURE.md (for the SketchAnalysis / Suggestion /
  RenderQuality domain types)

FILES YOU MAY MODIFY
- NEW: data/repository/*.kt
- NEW: di/RepositoryModule.kt OR add to existing module

OUTPUT FORMAT — per ai_handoff/MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug   (must PASS)
- ./gradlew test            (must PASS)

NEXT
MT-015 (SurfaceProfile hierarchy).
```

---

> The remaining prompts (MT-015 through MT-035) follow the same pattern. Each
> reads the corresponding doc (SURFACE_PROFILES.md, PIPELINE_ARCHITECTURE.md,
> DATA_MODEL.md, MIGRATION_BLUEPRINT.md) and produces the named files. The
> docs already contain the verbatim Kotlin source for the most complex MTs
> (MT-015, MT-016, MT-017), so those prompts are short:

### MT-015 — SurfaceProfile sealed catalog

```text
MT-015: Port the 12-surface catalog from Creative Studio.

YOUR TASK
1. Read integration/SURFACE_PROFILES.md.
2. Create the 3 files shown there VERBATIM:
   - design/ai/intelligence/SurfaceProfile.kt
   - design/ai/intelligence/SurfaceCatalog.kt
   - design/ai/intelligence/TemplateIntelligencePrompt.kt
3. Add unit tests asserting SurfaceCatalog.forTemplate() returns the correct
   profile for each of: henna+palm, henna+hand, henna+foot, abaya, thobe,
   toub, stone, plaster, arch, plate, tile, mug (12 cases).

FILES TO READ
- integration/SURFACE_PROFILES.md

FILES YOU MAY MODIFY
- NEW: design/ai/intelligence/SurfaceProfile.kt
- NEW: design/ai/intelligence/SurfaceCatalog.kt
- NEW: design/ai/intelligence/TemplateIntelligencePrompt.kt
- NEW: test/.../SurfaceCatalogTest.kt

OUTPUT FORMAT — per ai_handoff/MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug   (must PASS)
- ./gradlew test            (must PASS, including the 12 catalog cases)

NEXT
MT-016 (SurfaceDirections).
```

### MT-016 — SurfaceDirections render prompts

```text
MT-016: Port the 12 surface render direction strings + the universal quality tail.

YOUR TASK
1. Read integration/SURFACE_PROFILES.md section 'SurfaceDirections'.
2. Create design/ai/intelligence/SurfaceDirections.kt VERBATIM from the doc.
3. Add a unit test asserting forProfile() returns a non-empty string for
   every SurfaceProfile variant (covered exhaustively by `when` so the test
   is a compile-time guarantee — just instantiate one of each and call).

FILES TO READ
- integration/SURFACE_PROFILES.md (SurfaceDirections section)

FILES YOU MAY MODIFY
- NEW: design/ai/intelligence/SurfaceDirections.kt
- NEW: test/.../SurfaceDirectionsTest.kt

OUTPUT FORMAT — per ai_handoff/MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug + ./gradlew test (both PASS)
- grep -c "Render this sketch" design/ai/intelligence/SurfaceDirections.kt  (must be 12)

NEXT
MT-017 (TemplateAssetManager wiring).
```

### MT-017 — TemplateAssetManager → SurfaceProfile

```text
MT-017: Wire the existing TemplateAssetManager to resolve a SurfaceProfile
for each template it loads.

YOUR TASK
1. Find TemplateAssetManager.kt (likely under design/templates/ or similar).
2. Add one new public method:
       fun surfaceProfile(template: TemplateEntity): SurfaceProfile =
           SurfaceCatalog.forTemplate(template)
3. Do NOT change any other method.

FILES TO READ
- Existing TemplateAssetManager.kt
- design/ai/intelligence/SurfaceCatalog.kt (from MT-015)

FILES YOU MAY MODIFY
- Existing TemplateAssetManager.kt (single method addition)

OUTPUT FORMAT — per ai_handoff/MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug + ./gradlew test (both PASS)

NEXT
MT-018 (SketchAnalysis schema).
```

---

## STAGES 4-9 — Prompt Templates

> The remaining 15 prompts (MT-018, MT-019, MT-020, MT-021, MT-022, MT-023,
> MT-024, MT-025, MT-026, MT-027, MT-028, MT-029, MT-030, MT-031, MT-032,
> MT-033, MT-034, MT-035, MT-043, MT-044) follow the same skeleton:
>
> ```
> MT-XXX: <one-line summary>
>
> CONTEXT
> <what changed, why this MT exists, what the source TS did>
>
> YOUR TASK
> 1. Read integration/<the relevant doc>.md
> 2. Implement the named class/method using the patterns in
>    integration/MIGRATION_BLUEPRINT.md.
> 3. (specifics)
>
> FILES TO READ
> - integration/<doc>.md
> - <minimal Kotlin files for context>
>
> FILES YOU MAY MODIFY
> - <explicit list>
>
> ANTI-PATTERNS
> - <surface-specific traps>
>
> OUTPUT FORMAT — per ai_handoff/MASTER_PLAN.md Section 3.
>
> VERIFICATION
> - ./gradlew assembleDebug + ./gradlew test (both PASS)
> - <MT-specific grep checks>
>
> NEXT
> MT-YYY
> ```
>
> When you reach those MTs, ask the orchestrator (the AI that built this
> handoff) to generate the specific prompt with the exact file lists, or follow
> the skeleton above using the corresponding section of MIGRATION_BLUEPRINT.md
> and PIPELINE_ARCHITECTURE.md.
>
> The full per-MT detail for the more complex MTs (MT-018, MT-019, MT-027,
> MT-031) is documented inline in PIPELINE_ARCHITECTURE.md and
> MIGRATION_BLUEPRINT.md. The downstream agent has enough information to
> execute each one as a self-contained micro-task.

---

## 🛑 STOP CONDITIONS

The downstream agent MUST stop and escalate to you if any of these happens:

1. A test failure that does not match a known anti-pattern in the prompt.
2. A new dependency required by the MT description but not present in
   `gradle/libs.versions.toml`.
3. An existing file that needs modification but is over 500 lines (too risky
   to edit blind — request a focused reading first).
4. A schema change that contradicts integration/DATA_MODEL.md (the doc is
   authoritative; if you find a real reason to diverge, ask first).
5. A provider rate-limit / quota error during smoke tests (MT-039 specifically).
