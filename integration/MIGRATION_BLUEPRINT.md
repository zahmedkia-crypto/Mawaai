# MIGRATION BLUEPRINT — TypeScript → Kotlin Patterns

A pattern cookbook for porting Creative Studio TypeScript idioms to idiomatic Kotlin on Android. The downstream agent should reach for this file whenever it needs to translate a `*.ts` file from the source repo.

---

## 🎯 Core Translation Rules

| TypeScript pattern | Kotlin pattern |
|---|---|
| `interface Foo { a: string; b?: number }` | `data class Foo(val a: String, val b: Int? = null)` |
| `type Status = "a" \| "b" \| "c"` | `enum class Status { A, B, C }` |
| `z.object({ a: z.string() })` (Zod) | `data class A(val a: String)` |
| `z.enum(["a","b","c"])` | `enum class { A, B, C }` |
| `z.array(X).max(N)` | `val items: List<X>` + `require(items.size <= N)` in `init {}` |
| `Promise<T>` | `suspend fun ... : T` |
| `async/await` | `suspend` + `withContext(Dispatchers.IO) { ... }` |
| `fetch(...)` | Retrofit interface + `@Singleton @Inject class FooClient` |
| `process.env.X` | `BuildConfig.X` (set via `local.properties`) |
| `try { ... } catch (e: any) { if (...) ... }` | `runCatching { ... }.recoverCatching { e -> when {...} }` |
| `JSON.parse(...)` | `gson.fromJson(s, T::class.java)` |
| `useState<T>(...)` | `MutableStateFlow<T>(...)` exposed as `StateFlow` |
| `useEffect(() => {...}, [dep])` | `LaunchedEffect(dep) { ... }` |
| `createServerFn(...)` | `@HiltViewModel class ...ViewModel` + `suspend fun handler()` |
| `supabase.from('x').select(...)` | `xDao.observe...()` returning `Flow` |
| `supabase.storage.from(...).download(...)` | `ProjectFileStorage.read(path)` |

---

## 🧬 Schema Translation Example

### TypeScript (Zod):
```ts
const analysisSchema = z.object({
  art_style: z.string(),
  symmetry: z.object({
    type: z.string(),
    accuracy_pct: z.number().min(0).max(100),
    weaker_side: z.string(),
    notes: z.string(),
  }),
  findings: z.array(
    z.object({
      id: z.string(),
      severity: z.enum(["info", "warning", "critical"]),
      region_x_0_1: z.number().min(0).max(1),
      what: z.string(),
    }),
  ).max(12),
});

type SketchAnalysis = z.infer<typeof analysisSchema>;
```

### Kotlin equivalent:
```kotlin
data class SketchAnalysis(
    @SerializedName("art_style") val artStyle: String,
    val symmetry: Symmetry,
    val findings: List<Finding>
) {
    init {
        require(findings.size <= 12) { "max 12 findings" }
    }

    data class Symmetry(
        val type: String,
        @SerializedName("accuracy_pct") val accuracyPct: Int,
        @SerializedName("weaker_side") val weakerSide: String,
        val notes: String
    ) {
        init { require(accuracyPct in 0..100) }
    }

    data class Finding(
        val id: String,
        val severity: Severity,
        @SerializedName("region_x_0_1") val regionX: Float,
        val what: String
    ) {
        init { require(regionX in 0f..1f) }
    }

    enum class Severity {
        @SerializedName("info") INFO,
        @SerializedName("warning") WARNING,
        @SerializedName("critical") CRITICAL
    }
}
```

**Why `init { require(...) }`:** Kotlin doesn't have Zod's runtime parse + validate; we validate at construction. This catches malformed JSON from the AI early.

---

## 🧰 Schema-Validated AI Call Pattern

Lovable uses `generateText` with `experimental_output: Output.object({ schema })` — Vercel AI SDK strict mode. We need to replicate that with Gson + manual repair.

```kotlin
// design/ai/analysis/StructuredAnalysisClient.kt

class StructuredAnalysisClient @Inject constructor(
    private val gateway: ProviderRegistry,
    private val gson: Gson
) {
    /**
     * Send an image + structured-output prompt; validate JSON; retry once with
     * repair-prompt if validation fails; fall back to heuristic on second failure.
     */
    suspend fun analyze(sketch: Bitmap, template: Template): Result<SketchAnalysis> {
        val schemaSnippet = SCHEMA_DESCRIPTION  // human-readable JSON shape pasted into prompt
        val systemPrompt = SYSTEM_PROMPT
        val userPrompt = buildUserPrompt(template, schemaSnippet)

        val chain = gateway.activeVisionChain()
        val response = chain.visionAnalyze(userPrompt, sketch)
            .getOrElse { return Result.failure(it) }

        // Parse + validate
        return runCatching {
            val cleaned = response.trim().removeSurrounding("```json", "```").trim()
            gson.fromJson(cleaned, SketchAnalysis::class.java)
                ?: error("Gson returned null for: ${cleaned.take(200)}")
        }
    }

    private companion object {
        const val SYSTEM_PROMPT = "You are a master cultural designer..."
        // Embedded directly so the model knows the shape without an OpenAI-style "functions" call.
        const val SCHEMA_DESCRIPTION = """
            Return ONLY valid JSON matching this exact shape (no markdown):
            {
              "art_style": "string",
              "cultural_origin": "string",
              "symmetry": { "type": "string", "accuracy_pct": 0-100, ... },
              ...
            }
        """
    }
}
```

**Critical idiom:** wrap the response parse in `runCatching` and let a higher layer decide whether to invoke the heuristic fallback. Don't catch-and-swallow inside the client.

---

## 🌐 Retrofit Client Pattern (per provider)

The source uses `fetch(...)` directly. On Android, every provider becomes a Retrofit interface + a `@Singleton @Inject class` wrapper.

```kotlin
// design/ai/groq/GroqApi.kt

interface GroqApi {
    @POST("openai/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") auth: String,
        @Body body: GroqChatRequest
    ): GroqChatResponse
}

// design/ai/groq/GroqDtos.kt — OpenAI-compatible chat schema (Groq follows OpenAI shape)

data class GroqChatRequest(
    val model: String,
    val messages: List<Message>,
    @SerializedName("max_tokens") val maxTokens: Int? = null,
    val temperature: Float? = null
) {
    data class Message(val role: String, val content: List<Content>) {
        // Vision: content is an array of {type, text} or {type, image_url}
        sealed interface Content {
            @SerializedName("type") val type: String
            data class Text(val text: String) : Content {
                @SerializedName("type") override val type = "text"
            }
            data class ImageUrl(@SerializedName("image_url") val imageUrl: ImageUrlValue) : Content {
                @SerializedName("type") override val type = "image_url"
            }
        }
        data class ImageUrlValue(val url: String)
    }
}

data class GroqChatResponse(
    val choices: List<Choice>,
    val error: GroqError? = null
) {
    data class Choice(val message: ResponseMessage)
    data class ResponseMessage(val role: String, val content: String) {
        val contentText: String get() = content
    }
    data class GroqError(val message: String, val code: String?)
}
```

---

## ⏳ Coroutines / Async Mapping

| TypeScript | Kotlin |
|---|---|
| `const x = await foo()` | `val x = foo()` (where `foo` is `suspend`) |
| `Promise.all([a(), b()])` | `coroutineScope { val a = async { a() }; val b = async { b() }; a.await() to b.await() }` |
| `try { await x } catch (e) { ... }` | `runCatching { x }.getOrElse { e -> ... }` |
| `setTimeout(fn, ms)` | `delay(ms); fn()` |
| `setInterval(fn, ms)` | `flow { while (true) { delay(ms); emit(Unit) } }` |

---

## 🪟 Compose UI Mapping (PipelineProgress example)

### TypeScript (React + Tailwind):
```tsx
function PipelineProgress({ stage }: { stage: string }) {
  const stages = ["sketch", "analyze", "render"];
  return (
    <div className="flex gap-2">
      {stages.map((s) => (
        <div key={s} className={cn("badge", s === stage && "active")}>
          {s}
        </div>
      ))}
    </div>
  );
}
```

### Compose:
```kotlin
@Composable
fun PipelineProgress(currentStage: PipelineStage, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PipelineStage.values().forEach { stage ->
            StageBadge(
                stage = stage,
                isActive = stage == currentStage,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

enum class PipelineStage(val label: String) {
    SKETCH("Sketch"),
    ANALYZE("Analyze"),
    SUGGEST("Suggest"),
    RENDER("Render"),
    QUALITY("Quality"),
    EXPORT("Export")
}

@Composable
private fun StageBadge(stage: PipelineStage, isActive: Boolean, modifier: Modifier = Modifier) {
    val container = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
    val content = if (isActive) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(stage.label, style = MaterialTheme.typography.labelMedium, color = content)
    }
}
```

---

## 🔁 Error Translation Pattern

### TypeScript:
```ts
try {
  await renderMasterpiece(projectId);
} catch (e: any) {
  if (e.message.includes("429")) { showToast("Rate limited"); }
  else if (e.message.includes("402")) { showToast("Credits exhausted"); }
  else { showToast("Render failed: " + e.message); }
}
```

### Kotlin:
```kotlin
viewModelScope.launch {
    renderer.render(projectId).fold(
        onSuccess = { /* navigate */ },
        onFailure = { e ->
            val userMessage = when (e) {
                is ProviderRecoverableError.RateLimited -> "AI is busy — try again shortly"
                is ProviderRecoverableError.QuotaExhausted -> "Free quota exhausted — switch provider in Settings"
                is ProviderFatalError.InvalidKey -> "API key invalid — check Settings"
                is ProviderFatalError.SafetyBlock -> "Content blocked by safety filter"
                else -> "Render failed: ${e.message}"
            }
            _events.emit(UiEvent.ShowSnackbar(userMessage))
        }
    )
}
```

The `when` exhaustive over sealed errors > string-match. Type-safe, refactor-safe.

---

## 📦 Storage Pattern

### TypeScript (Supabase storage):
```ts
const { data } = await supabase.storage.from("sketches").download(path);
const buf = new Uint8Array(await data.arrayBuffer());
const b64 = btoa(String.fromCharCode(...buf));
```

### Kotlin (local file + Base64):
```kotlin
suspend fun loadSketchAsBase64(projectId: String): Pair<String, String> = withContext(Dispatchers.IO) {
    val file = projectFileStorage.sketchFile(projectId)
    require(file.exists()) { "Sketch not saved for project $projectId" }
    val bytes = file.readBytes()
    val mime = if (file.extension.equals("png", true)) "image/png" else "image/jpeg"
    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    mime to b64
}
```

---

## 🧪 Tests — Lovable's Zod vs Kotlin's `init {}`

### Lovable:
```ts
// schema validates at parse time
const result = analysisSchema.safeParse(json);
if (!result.success) { useFallback(); }
```

### Kotlin equivalent:
```kotlin
val analysis = runCatching { gson.fromJson(json, SketchAnalysis::class.java) }
    .getOrElse { return useFallback() }
// init { require(...) } already validated bounds; if it threw, runCatching captured it.
```

Add unit tests for boundary values:

```kotlin
class SketchAnalysisTest {
    @Test fun `Symmetry accepts boundary values`() {
        SketchAnalysis.Symmetry("bilateral", 0, "none", "")  // OK
        SketchAnalysis.Symmetry("bilateral", 100, "none", "") // OK
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Symmetry rejects accuracy_pct above 100`() {
        SketchAnalysis.Symmetry("bilateral", 101, "none", "")
    }

    @Test fun `findings max 12 enforced`() {
        SketchAnalysis(
            artStyle = "x",
            symmetry = SketchAnalysis.Symmetry("none", 0, "none", ""),
            findings = List(12) { dummyFinding(it.toString()) }
        )
        assertFailsWith<IllegalArgumentException> {
            SketchAnalysis(
                artStyle = "x",
                symmetry = SketchAnalysis.Symmetry("none", 0, "none", ""),
                findings = List(13) { dummyFinding(it.toString()) }
            )
        }
    }
}
```

---

## 🎨 Specific Idiomatic Differences To Watch

1. **Optional chaining** `a?.b?.c` ports cleanly: `a?.b?.c` — same in Kotlin.
2. **Nullish coalescing** `a ?? b` → `a ?: b`.
3. **String templates** `` `Hello ${name}` `` → `"Hello $name"`.
4. **Array spread** `[...arr]` → `arr.toMutableList()` or `arr + emptyList()`.
5. **Object spread** `{ ...obj, x: 1 }` → `obj.copy(x = 1)` (for data classes).
6. **Destructuring** `const { a, b } = obj` → `val (a, b) = obj` (data classes with `componentN`).
7. **Truthy/falsy** doesn't work in Kotlin: `if (str)` → `if (str.isNotEmpty())`.
8. **Number division** `5 / 2 == 2.5` in TS → `5 / 2 == 2` in Kotlin (use `5.0 / 2`).

---

## 🚫 What Not To Translate

- **React state hooks** (`useState`, `useReducer`, `useContext`) — use ViewModel + StateFlow.
- **React Query** (`useQuery`) — use a Flow that re-emits on database changes.
- **TanStack Router** (`createServerFn`) — replace with ViewModel methods.
- **Tailwind classes** — translate semantically to Compose `Modifier` calls, not 1:1.
- **shadcn/ui components** — Material 3 has equivalents (Button, Card, Switch, etc.).
- **Supabase auth** — single-user offline app; no auth layer.
- **Server functions** — Android calls APIs directly via Retrofit/the gateway.

---

## ✅ Pre-Commit Checklist For Any Port

For every `.ts` → `.kt` port commit, verify:

- [ ] No `Map<String, Any>` introduced (Hard Rule #6)
- [ ] No `as` casts on `Any?` outside of one well-commented site
- [ ] All Zod `.min()/.max()` translated to `init { require(...) }`
- [ ] All Zod enums translated to Kotlin `enum class` with `@SerializedName`
- [ ] All `Promise<T>` translated to `suspend fun ... : T`
- [ ] All `async/await` running inside an explicit Dispatcher
- [ ] No new dependencies (unless MT authorizes)
- [ ] Schema constraints unit-tested at boundary values
- [ ] Public API has KDoc explaining when to use it

Use this list when reviewing the downstream agent's diff.
