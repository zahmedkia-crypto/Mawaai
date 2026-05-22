# AI Pipeline Design

Multimodal orchestration for MAWAAI. One model per responsibility. Strong types between stages.

## Standard Pipeline

```
User sketch + intent
   │
   ▼
[1] VisionAnalyzer        → VisionAnalysis (subject, style hints, structure)
   │
   ▼
[2] TemplateAnalyzer      → TemplateMatch (best template + score)
   │
   ▼
[3] PromptSynthesizer     → SdPromptBundle (prompt, negative, params)
   │
   ▼
[4] EnhancementModel      → GeneratedImage (raw)
   │                       (SDXL + ControlNet[canny|scribble|depth])
   ▼
[5] BackgroundRemoval     → MaskedImage (subject + alpha)
   │
   ▼
[6] Upscaler              → HighResImage (Real-ESRGAN)
   │
   ▼
[7] OpenCvProcessor       → FinalComposite (warp, blend, place)
```

## Kotlin Contracts (skeleton)

```kotlin
interface VisionAnalyzer {
    suspend fun analyze(image: Bitmap, hint: UserHint): Result<VisionAnalysis>
}

data class VisionAnalysis(
    val subject: Subject,
    val styleSignals: List<StyleSignal>,
    val structure: SketchStructure,
    val confidence: Float,
)

interface PromptSynthesizer {
    fun synthesize(
        analysis: VisionAnalysis,
        template: TemplateContext,
    ): SdPromptBundle
}

data class SdPromptBundle(
    val positive: String,
    val negative: String,
    val controlNetGuidance: ControlNetGuidance,
    val seed: Long?,
    val steps: Int,
    val cfg: Float,
)
```

Each stage returns `Result<T>` — never throw across stage boundaries.

## Per-Model System Prompts

Each model gets its own isolated system prompt. Never share prompts across stages.

### Vision Analyzer (GPT-4o / Gemini)
```
You analyze a user-provided sketch for a design app.
Output STRICT JSON with keys: subject, styleSignals[], structure, confidence.
Do not invent details beyond what is visible.
If confidence < 0.5, set confidence and stop.
Never produce prose. JSON only.
```

### Prompt Synthesizer (LLM, not a vision model)
```
You convert structured VisionAnalysis + TemplateContext into a Stable Diffusion prompt bundle.
Constraints:
- Positive prompt: <= 75 tokens, comma-separated keywords
- Always include negative prompt for hands, text artifacts, watermark
- Match TemplateContext.styleProfile.palette exactly
- Never hallucinate brand names
Output JSON matching SdPromptBundle schema.
```

### Enhancement Model (SDXL + ControlNet config)
- ControlNet type chosen by `SketchStructure.kind`:
  - `Outline` → canny
  - `RoughSketch` → scribble
  - `Photo3DHint` → depth
- CFG: 6.5–8.5 default
- Steps: 28–35 default
- Seed pinned for reproducibility in test fixtures

## Orchestration Rules

1. Each stage runs on `Dispatchers.IO` (network) or `Dispatchers.Default` (CPU/OpenCV)
2. Cancellation propagates — wrap the pipeline in a single `CoroutineScope` tied to the screen
3. Memory: release intermediate bitmaps immediately after the next stage consumes them
4. Cache: cache by content hash, not by user session
5. Telemetry: emit a `StageEvent` per stage (start/end/error) for debugging

## Failure Modes & Fallbacks

| Stage | Failure | Fallback |
|---|---|---|
| Vision | API timeout | Retry once, then on-device MobileNet labels |
| Prompt | Schema invalid | Re-prompt with stricter system message |
| SD/ControlNet | 5xx | Switch endpoint or queue |
| Background removal | Mask < threshold | On-device U2Net |
| Upscale | OOM | Tile-based upscale at lower factor |
| OpenCV compose | Mat empty | Return unprocessed generated image with warning flag |
