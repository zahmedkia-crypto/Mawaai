# MIGRATION BLUEPRINT — TypeScript → Kotlin Patterns

How to translate Creative Studio idioms into Kotlin idioms that fit your existing Android architecture. Use as a reference while implementing any STAGE 4+ MT.

---

## 🎯 The Core Principle

**Do not translate code line-by-line.** The TS app uses React + Vercel AI SDK + Zod + Supabase + tRPC. The Android app uses Hilt + Compose + Retrofit + Room + Gson. Translate the **shape and contract** of each module, not its syntax.

---

## 📐 Pattern 1: Zod Schemas → Kotlin Data Classes

### TypeScript (Lovable analysis.functions.ts)
```typescript
const analysisSchema = z.object({
  art_style: z.string().describe("e.g. islamic_geometric, arabesque, calligraphy"),
  symmetry: z.object({
    type: z.string().describe("e.g. bilateral, radial, none"),
    accuracy_pct: z.number().min(0).max(100),
  }),
  findings: z.array(z.object({ ... })).max(12)
});
type SketchAnalysis = z.infer<typeof analysisSchema>;
```

### Kotlin equivalent
```kotlin
data class SketchAnalysis(
    @SerializedName("art_style") val artStyle: String,
    val symmetry: Symmetry,
    val findings: List<Finding>             // max 12 enforced in builder
) {
    init { require(findings.size <= 12) { "findings must be ≤ 12" } }

    data class Symmetry(
        val type: String,
        @SerializedName("accuracy_pct") val accuracyPct: Int
    ) {
        init {
            require(accuracyPct in 0..100) { "accuracyPct must be 0..100" }
        }
    }

    data class Finding(/* ... */) { /* ... */ }
}
```

**Rules:**
- Use `@SerializedName` for snake_case ↔ camelCase
- Enforce min/max via `init { require() }` (becomes IllegalArgumentException on deserialize)
- Use `Int` not `Number` for integer fields (Kotlin distinguishes)
- Nested types as nested data classes inside the outer class — no namespace pollution

---

## 📐 Pattern 2: Vercel AI SDK `generateText` → Custom Retrofit Call

### TypeScript
```typescript
const { experimental_output } = await generateText({
  model: getModel(),
  system: systemPrompt,
  experimental_output: Output.object({ schema: analysisSchema }),
  temperature: 0.1,
  maxOutputTokens: 4096,
  messages: [{
    role: "user",
    content: [
      { type: "text", text: userText },
      { type: "image", image: `data:${mediaType};base64,${b64}` }
    ]
  }]
});
```

### Kotlin equivalent (Gemini specifically)
```kotlin
suspend fun analyze(prompt: String, image: Bitmap): Result<SketchAnalysis> {
    val analysisSchemaJson = """
        {"type":"object","properties":{
            "art_style":{"type":"string"},
            "symmetry":{"type":"object","properties":{...}},
            ...
        },"required":["art_style","symmetry","findings"]}
    """.trimIndent()

    val systemPrompt = "You are a master designer..."
    val userText = "..."

    val b64 = bitmapToBase64(image)

    val request = GeminiRequest(
        contents = listOf(
            GeminiRequest.Content(parts = listOf(
                GeminiRequest.Part(text = "$systemPrompt\n\n$userText"),
                GeminiRequest.Part(inlineData = GeminiRequest.InlineData(
                    mimeType = "image/jpeg",
                    data = b64
                ))
            ))
        ),
        generationConfig = GeminiRequest.GenerationConfig(
            temperature = 0.1f,
            maxOutputTokens = 4096,
            responseMimeType = "application/json",
            responseSchema = analysisSchemaJson    // Gemini supports JSON schema constrained output
        )
    )

    return runCatching {
        val response = api.generateContent(MODEL, key, request)
        val jsonText = response.candidates?.firstOrNull()
            ?.content?.parts?.firstOrNull()
            ?.text ?: error("empty candidate")
        Gson().fromJson(jsonText, SketchAnalysis::class.java)
    }
}
```

### Through the gateway (preferred — post-MT-036)
```kotlin
suspend fun analyze(prompt: String, image: Bitmap): Result<SketchAnalysis> {
    val chain = providerRegistry.activeVisionChain()
    val structuredPrompt = """
        $systemPrompt

        Return ONLY a JSON object matching this schema:
        ${analysisSchemaText}

        $userText
    """.trimIndent()

    return chain.visionAnalyze(structuredPrompt, image)
        .mapCatching { jsonText -> Gson().fromJson(jsonText, SketchAnalysis::class.java) }
        .recover { e ->
            Log.w(TAG, "Structured analysis failed: ${e.message} — using heuristic fallback")
            FallbackAnalysis.build(template)
        }
}
```

**Why:** The gateway abstracts whether the call goes to Gemini's `responseSchema` constrained mode, Groq's OpenAI-compatible API, or OpenRouter's auto-router. The caller only deals with `String → SketchAnalysis`.

---

## 📐 Pattern 3: Server Function Mutation → Repository Mutation

### TypeScript (Supabase)
```typescript
await supabaseAdmin
  .from("projects")
  .update({ analysis: experimental_output as any, status: "analyzed" })
  .eq("id", data.projectId);
```

### Kotlin (Room)
```kotlin
// In ProjectRepository:
suspend fun saveAnalysis(projectId: String, analysis: SketchAnalysis) {
    val json = gson.toJson(analysis)
    withContext(Dispatchers.IO) {
        projectDao.updateAnalysis(projectId, json, status = ProjectStatus.ANALYZED.name)
    }
}

// In ProjectDao:
@Query("UPDATE projects SET analysisJson = :json, status = :status, updatedAt = :now WHERE id = :id")
suspend fun updateAnalysis(id: String, json: String, status: String, now: Long = System.currentTimeMillis())
```

**Rules:**
- DAO methods always `suspend` (not Flow when it's a mutation)
- `updatedAt` parameter has default `System.currentTimeMillis()` — never use `now()` inside the SQL (SQLite has limited time fns)
- Repository decides the JSON serialization; DAO is dumb storage
- Status enums round-trip as their `.name` string

---

## 📐 Pattern 4: Multi-Modal Image Content

### TypeScript
```typescript
messages: [{
  role: "user",
  content: [
    { type: "text", text: userText },
    { type: "image", image: `data:${mediaType};base64,${b64}` }
  ]
}]
```

### Kotlin — Gemini
```kotlin
GeminiRequest.Content(parts = listOf(
    GeminiRequest.Part(text = userText),
    GeminiRequest.Part(inlineData = GeminiRequest.InlineData(
        mimeType = "image/jpeg",
        data = base64
    ))
))
```

### Kotlin — OpenAI-compatible (Groq, OpenRouter, Cloudflare)
```kotlin
ChatRequest.Message(
    role = "user",
    content = listOf(
        ChatRequest.Content.Text(text = userText),
        ChatRequest.Content.ImageUrl(imageUrl = "data:image/jpeg;base64,$base64")
    )
)
```

**Bitmap → base64 helper:**
```kotlin
suspend fun Bitmap.toJpegBase64(quality: Int = 85, maxDimension: Int = 1024): String =
    withContext(Dispatchers.Default) {
        val resized = if (width > maxDimension || height > maxDimension) {
            val scale = maxDimension.toFloat() / maxOf(width, height)
            Bitmap.createScaledBitmap(this@toJpegBase64, (width * scale).toInt(), (height * scale).toInt(), true)
        } else this@toJpegBase64
        ByteArrayOutputStream().use { out ->
            resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }
    }
```

---

## 📐 Pattern 5: React `useQuery` → Compose `collectAsStateWithLifecycle`

### TypeScript (TanStack Query)
```tsx
const { data: project } = useQuery({
  queryKey: ['project', projectId],
  queryFn: () => fetchProject(projectId),
});
return <div>{project?.title}</div>;
```

### Kotlin (Compose + Flow)
```kotlin
@HiltViewModel
class ProjectViewModel @Inject constructor(
    repo: ProjectRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val projectId: String = savedStateHandle["projectId"]!!
    val project: StateFlow<Project?> = repo.observe(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ProjectScreen(viewModel: ProjectViewModel = hiltViewModel()) {
    val project by viewModel.project.collectAsStateWithLifecycle()
    Text(project?.title ?: "Loading…")
}
```

---

## 📐 Pattern 6: Error Translation (HTTP → Domain Errors)

### TypeScript (loose `any`)
```typescript
catch (err: any) {
  const msg = err?.message ?? String(err);
  if (msg.includes("429")) throw new Error("AI service is rate limited");
  if (msg.includes("402")) throw new Error("AI credits exhausted");
  throw new Error(`Analysis failed: ${msg}`);
}
```

### Kotlin (typed)
```kotlin
.recoverCatching { e ->
    val httpCode = (e as? HttpException)?.code()
    when (httpCode) {
        404 -> throw ProviderRecoverableError.NotFound("Model deprecated: ${e.message()}")
        429 -> throw ProviderRecoverableError.RateLimited("Rate limited")
        402 -> throw ProviderRecoverableError.QuotaExhausted("Credits exhausted")
        503 -> throw ProviderRecoverableError.ServiceUnavailable("Provider unavailable")
        401, 403 -> throw ProviderFatalError.InvalidKey("Auth failed")
        in 400..499 -> throw ProviderFatalError.MalformedRequest("HTTP ${httpCode}: ${e.message()}")
        else -> throw e
    }
}
```

Every HTTP-talking call site MUST translate errors into the typed gateway hierarchy. Untyped error strings are forbidden in design/ai/ (Hard Rule #6).

---

## 📐 Pattern 7: Supabase Storage → Local Files

### TypeScript
```typescript
const { data, error } = await supabaseAdmin.storage
  .from("renders")
  .upload(renderPath, bytes, { contentType, upsert: true });

const { data: signed } = await supabaseAdmin.storage
  .from("renders")
  .createSignedUrl(renderPath, 3600);
```

### Kotlin (local file system)
```kotlin
@Singleton
class MawaaiStorage @Inject constructor(@ApplicationContext ctx: Context) {
    private val renders: File = File(ctx.filesDir, "renders").apply { mkdirs() }

    suspend fun writeRender(projectId: String, bytes: ByteArray, ext: String = "png"): File =
        withContext(Dispatchers.IO) {
            val f = File(renders, "$projectId.$ext")
            f.writeBytes(bytes)
            f
        }

    fun renderUri(projectId: String, ext: String = "png"): Uri? {
        val f = File(renders, "$projectId.$ext")
        return if (f.exists()) Uri.fromFile(f) else null
    }
}
```

For external-share (the "signed URL" equivalent), copy the file to `MediaStore.Downloads` and use a `FileProvider` URI.

---

## 📐 Pattern 8: Optimistic UI Updates

### TypeScript (TanStack)
```tsx
const mutation = useMutation({
  mutationFn: acceptSuggestion,
  onMutate: async (id) => {
    await queryClient.cancelQueries(['project', projectId]);
    queryClient.setQueryData(['project', projectId], (old) => ({
      ...old, accepted_suggestion_ids: [...old.accepted_suggestion_ids, id]
    }));
  }
});
```

### Kotlin (StateFlow-based)
```kotlin
class SuggestionCardsViewModel @Inject constructor(...) : ViewModel() {
    private val _optimisticAcceptedIds = MutableStateFlow<Set<String>>(emptySet())
    val acceptedIds: StateFlow<Set<String>> =
        combine(repo.observe(projectId).map { it?.acceptedSuggestionIds.orEmpty().toSet() },
                _optimisticAcceptedIds) { server, local -> server + local }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun accept(id: String) {
        _optimisticAcceptedIds.update { it + id }
        viewModelScope.launch {
            repo.saveAcceptedSuggestionIds(
                projectId,
                (acceptedIds.value + id).toList()
            )
            // On success, _optimisticAcceptedIds can be cleared — server is now source of truth
            _optimisticAcceptedIds.update { it - id }
        }
    }
}
```

---

## 📐 Pattern 9: Surface-Conditional Logic

### TypeScript
```typescript
const colorInstr = effectiveColor
  ? GARMENT_SURFACES.has(surfaceKey)
    ? `The garment's base fabric color must be ${effectiveColor}. The embroidery/pattern color should harmonize.`
    : `Use ${effectiveColor} as the dominant color in the design.`
  : "";
```

### Kotlin (exhaustive `when` on sealed)
```kotlin
fun colorInstruction(profile: SurfaceProfile, color: String?): String? {
    if (color.isNullOrBlank()) return null
    return when (profile) {
        is SurfaceProfile.FabricAbaya, is SurfaceProfile.FabricThobe, is SurfaceProfile.FabricToub ->
            "The garment's base fabric color must be $color. The embroidery/pattern color should harmonize."
        is SurfaceProfile.CeramicPlate, is SurfaceProfile.CeramicTile, is SurfaceProfile.CeramicMug,
        is SurfaceProfile.SkinPalm, is SurfaceProfile.SkinHandFull, is SurfaceProfile.SkinFoot,
        is SurfaceProfile.WallStone, is SurfaceProfile.WallPlaster, is SurfaceProfile.WallArch ->
            "Use $color as the dominant color in the design."
    }
}
```

**Why exhaustive when:** Adding a new SurfaceProfile variant (e.g. `FabricKurta`) becomes a compile error here, forcing you to decide which group it belongs to. No "default" branch silently misclassifying.

---

## 📐 Pattern 10: Quality Validation Two-Tier

### TypeScript (lines 78-145 of render.functions.ts)
```typescript
// Tier 1: heuristic from analysis.template_fit
const heuristic = { passed: blockers.length === 0, ... };
if (!heuristic.passed) return heuristic;
// Tier 2: AI visual QA on (sketch, render)
try {
  const result = await generateText({ ... });
  return { passed: qa.passed && qa.composition_preservation_0_100 >= 70 ... };
} catch (err) {
  return { passed: heuristic.score >= 70, ... };
}
```

### Kotlin (clean separation of two reviewers + composer)
```kotlin
class QualityGate @Inject constructor(
    private val heuristic: HeuristicQualityCheck,
    private val ai: AiQualityReviewer,
) {
    suspend fun evaluate(
        sketch: Bitmap, render: Bitmap, prompt: String,
        analysis: SketchAnalysis, template: Template
    ): RenderQuality {
        // Tier 1
        val hScore = heuristic.score(analysis, template)
        if (hScore.blockers.isNotEmpty()) {
            return RenderQuality(
                compositionPreservation = hScore.compositionScore,
                surfaceFit = hScore.surfaceScore,
                lightingRealism = 60,           // unknown without AI tier
                passed = false,
                issues = hScore.blockers,
                notes = "Tier-1 heuristic blocked: ${hScore.blockers.joinToString(" / ")}"
            )
        }
        // Tier 2
        return ai.review(sketch, render, prompt, template).getOrElse {
            // Fallback to tier-1 score if AI itself failed
            RenderQuality(
                compositionPreservation = hScore.compositionScore,
                surfaceFit = hScore.surfaceScore,
                lightingRealism = 70,
                passed = hScore.compositionScore >= 70 && hScore.surfaceScore >= 70,
                issues = if (hScore.compositionScore >= 70) emptyList() else listOf("AI review unavailable; heuristic-only pass"),
                notes = "Tier-2 AI review failed: ${it.message?.take(120)}"
            )
        }
    }
}
```

**Why the split:** Each reviewer is independently testable. The composer is a tiny coordinator with one job.

---

## 📐 Pattern 11: Render Prompt Assembly

### TypeScript
```typescript
const prompt = [
  structure,
  templateIntelligencePrompt(template),
  baseDirection,
  palette,
  colorInstr,
  refinements,
  "Final image only ..."
].filter(Boolean).join(" ");
```

### Kotlin (RenderPromptBuilder with explicit segments)
```kotlin
class RenderPromptBuilder @Inject constructor() {
    fun build(
        template: Template,
        profile: SurfaceProfile,
        acceptedSuggestions: List<Suggestion>,
        colorOverride: String?,
    ): RenderPrompt = RenderPrompt(
        structurePreservation = STRUCTURE_RULE,
        templateIntelligence = templateIntelligencePrompt(template),
        baseDirection = SurfaceDirections.forProfile(profile),
        palette = template.traditionalPalette
            .takeIf { it.isNotEmpty() }
            ?.let { "Honor the traditional palette where natural: ${it.joinToString(", ")}." },
        colorOverride = colorInstruction(profile, colorOverride),
        refinements = acceptedSuggestions
            .takeIf { it.isNotEmpty() }
            ?.let { suggs ->
                "Apply these refinements while rendering: " +
                    suggs.joinToString(" | ") { "${it.title} — ${it.previewHint}" }
            },
    )

    private companion object {
        const val STRUCTURE_RULE = "CRITICAL: Preserve the exact composition, motif " +
            "placement, line layout, and proportions of the input sketch — do not " +
            "reinterpret, restyle, or rearrange. Treat the sketch as the definitive " +
            "design and only translate its strokes into the target medium with " +
            "realistic materials, lighting, and texture."
    }
}
```

Then `RenderPrompt.toPromptString()` (defined in PIPELINE_ARCHITECTURE.md) does the final concatenation.

---

## 🚫 Anti-Patterns To Avoid

### ❌ Don't port React state hooks as `mutableStateOf` everywhere
React stores everything in component state. Compose state is for UI-only ephemera. **Domain state lives in StateFlow inside a ViewModel.**

### ❌ Don't translate `as any` to `Any`
TS's `as any` is a confession of "I don't know the type". Kotlin's `Any` is a deliberate choice. If you see `as any` in TS, your job is to **figure out the actual type** and model it properly.

### ❌ Don't replicate Supabase RLS in repository code
Supabase Row-Level Security policies live in the database. On Android, there's no row-level concept — the device IS the user. **Skip RLS translation entirely.**

### ❌ Don't translate `createServerFn` middleware as Kotlin annotations
TS server functions wrap a handler with middleware (auth, validation). In Android, auth is enforced by the OS (the app is signed for the user) and validation is the input layer's job (ViewModel). **Just call the repository directly.**

### ❌ Don't keep TypeScript's `null` vs `undefined` distinction
Kotlin has only `null`. Treat `undefined` as `null`.

### ❌ Don't put Gson serialization in entities
Entities are pure data. Serialization belongs in the Repository's mapper functions.

### ❌ Don't import `kotlinx.serialization` if you're using Gson elsewhere
The existing app uses Gson. **Stay with Gson.** Mixing serializers doubles your dependency footprint.

---

## ✅ Pattern Cheat Sheet

| TypeScript idiom | Kotlin idiom |
|---|---|
| `z.object({ ... })` | `data class X(...)` with `init { require(...) }` |
| `z.infer<typeof X>` | The data class itself |
| `await fn()` | `suspend fun` + `withContext(Dispatchers.IO) { fn() }` |
| `useQuery` | `repo.observe(id).stateIn(viewModelScope, ...)` |
| `useMutation` | `viewModelScope.launch { repo.mutate(...) }` |
| `process.env.X` | `BuildConfig.X` (from local.properties via build.gradle.kts) |
| `as any` | Figure out the type. Always. |
| `try { ... } catch (e: any)` | `runCatching { ... }.recoverCatching { e -> ... }` |
| `console.log` | `Log.d(TAG, ...)` or `AppLogger.d(...)` if present |
| `JSON.stringify(x)` | `gson.toJson(x)` |
| `JSON.parse(s)` | `gson.fromJson(s, T::class.java)` |
| `Date.now()` | `System.currentTimeMillis()` |
| `setTimeout(fn, ms)` | `viewModelScope.launch { delay(ms); fn() }` |
| React `props` | Compose function parameters |
| Tailwind classes | Compose `Modifier` chain + Material theme |
| Supabase RPC | Direct repository call |
| Supabase storage URL | `FileProvider` content:// URI |
| TanStack mutation | StateFlow + viewModelScope.launch |
| React Router route | Navigation Compose `composable("route/{id}") { ... }` |

---

**End of Migration Blueprint.** Cross-reference PIPELINE_ARCHITECTURE.md and SURFACE_PROFILES.md when implementing the higher-stage MTs.
