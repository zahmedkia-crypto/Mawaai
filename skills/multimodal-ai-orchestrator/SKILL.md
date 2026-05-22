---
name: multimodal-ai-orchestrator
description: Designs end-to-end multi-model AI pipelines for image generation and design enhancement apps. Use when the request involves combining Vision LLMs (GPT-4o/Gemini), Stable Diffusion + ControlNet, background removal, upscaling, and on-device OpenCV in a single workflow. Produces typed Kotlin contracts between stages, orchestration diagrams, model routing logic, prompt chain structure, fallback strategies, dispatcher/coroutine policies, caching rules, and telemetry events. Splits responsibilities so no single model does too much.
icon: workflow
color: Purple
---

# Multimodal AI Orchestrator

Designs the pipeline. Each stage has one responsibility, a typed contract, and an isolated system prompt. Delegates per-stage detail to specialists.

## When to Use

- Designing the AI pipeline for MAWAAI (sketch → design)
- Any multi-model workflow combining vision + generation + processing
- Selecting which stage owns which decision
- Defining fallbacks when a stage fails

## Standard MAWAAI Pipeline

```
User sketch + intent
   │
   ▼
[1] VisionAnalyzer        → VisionAnalysis
   │                       (subject, style hints, structure)
   ▼
[2] TemplateAnalyzer      → TemplateMatch
   │                       (best template + score)
   ▼
[3] PromptSynthesizer     → SdPromptBundle
   │                       (positive, negative, params, controlnet guidance)
   ▼
[4] EnhancementModel      → GeneratedImage
   │                       (SDXL + ControlNet[canny|scribble|depth])
   ▼
[5] BackgroundRemoval     → MaskedImage
   │                       (subject + alpha)
   ▼
[6] Upscaler              → HighResImage
   │                       (Real-ESRGAN)
   ▼
[7] OpenCvProcessor       → FinalComposite
                           (warp, blend, place onto template)
```

Specialists own each stage's detail:
- `vision-analysis-engineer` → stage [1]
- `template-intelligence-engine` → stage [2]
- `stable-diffusion-pipeline-builder` → stages [3]+[4]

## Kotlin Contracts (skeleton)

```kotlin
interface PipelineStage<In, Out> {
    suspend fun run(input: In): Result<Out>
}

interface VisionAnalyzer : PipelineStage<VisionInput, VisionAnalysis>
interface PromptSynthesizer : PipelineStage<PromptInput, SdPromptBundle>
interface EnhancementModel : PipelineStage<SdPromptBundle, GeneratedImage>
interface BackgroundRemoval : PipelineStage<GeneratedImage, MaskedImage>
interface Upscaler : PipelineStage<MaskedImage, HighResImage>
interface OpenCvProcessor : PipelineStage<ComposeInput, FinalComposite>
```

Every stage returns `Result<T>`. Never throw across stage boundaries.

## Orchestration Rules

1. Each stage runs on `Dispatchers.IO` (network) or `Dispatchers.Default` (CPU/OpenCV)
2. Cancellation propagates — wrap the pipeline in a single `CoroutineScope` tied to the screen
3. Release intermediate bitmaps immediately after the next stage consumes them
4. Cache by content hash, not by user session
5. Emit a `StageEvent` (start/end/error) per stage for telemetry

## Fallback Matrix

| Stage | Primary failure | Fallback |
|---|---|---|
| Vision | API timeout | Retry once → on-device MobileNet labels |
| Prompt | Schema invalid | Re-prompt with stricter system message |
| SD/ControlNet | 5xx | Switch endpoint or queue |
| Background removal | Mask < threshold | On-device U2Net |
| Upscale | OOM | Tile-based upscale at lower factor |
| OpenCV compose | Mat empty | Return unprocessed generated image with warning flag |

## Output

For any pipeline design request, produce:
1. **Diagram** (ASCII as above, customized to user's request)
2. **Stage table** — input type, output type, model, dispatcher, fallback
3. **Kotlin contract file** ready to drop into the project
4. **Cache + cancellation policy**
5. **Handoff list** — which specialist skill builds which stage next

## Anti-Patterns

- One mega-model doing vision + generation + compositing
- Untyped bitmap passing without metadata
- Synchronous stages on the main thread
- Sharing one system prompt across stages
- Skipping fallback design until production
