---
name: stable-diffusion-pipeline-builder
description: Builds Stable Diffusion / SDXL + ControlNet inference pipelines for mobile design enhancement. Use for sketch beautification, image-to-image generation, design enhancement, or any stage that turns a structured vision analysis into a generated image. Produces dynamic positive/negative prompts, ControlNet type selection (canny/scribble/depth), inference parameters (CFG, steps, seed policy), endpoint contracts (Replicate, HuggingFace, fal.ai, self-hosted), and Kotlin client code with retry/timeout policies.
icon: image
color: Purple
---

# Stable Diffusion Pipeline Builder

Owns stages [3] and [4] of the AI pipeline: prompt synthesis + image generation. Consumes `VisionAnalysis` + `TemplateContext`, produces `GeneratedImage`.

## When to Use

- Designing or implementing the SD/SDXL + ControlNet stage
- Writing the prompt synthesizer that converts structured analysis into prompts
- Choosing inference parameters and endpoints
- Adding retries, timeouts, and fallbacks to generation

## Prompt Synthesis Contract

```kotlin
@Serializable
data class SdPromptBundle(
    val positive: String,             // <= 75 CLIP tokens
    val negative: String,
    val controlNetGuidance: ControlNetGuidance,
    val seed: Long?,                  // null = random; pinned for tests
    val steps: Int,                   // 28..35 default
    val cfg: Float,                   // 6.5..8.5 default
    val width: Int,
    val height: Int,
)

@Serializable
data class ControlNetGuidance(
    val type: ControlNetType,         // CANNY, SCRIBBLE, DEPTH, OPENPOSE
    val conditioningScale: Float,     // 0.6..1.0 typical
    val controlImage: ControlImageRef // URL or content hash
)

enum class ControlNetType { CANNY, SCRIBBLE, DEPTH, OPENPOSE, LINEART }
```

## ControlNet Selection (driven by VisionAnalysis)

| `SketchStructure.kind` | ControlNet | Conditioning |
|---|---|---|
| `OUTLINE` | CANNY | 0.8 |
| `ROUGH_SKETCH` | SCRIBBLE | 0.7 |
| `PHOTO_3D_HINT` | DEPTH | 0.65 |
| `FILLED` | LINEART | 0.75 |

Never let the LLM choose ControlNet type via free text — always enum-mapped.

## Prompt Synthesizer System Prompt

```
You convert structured VisionAnalysis + TemplateContext into a Stable Diffusion prompt bundle.

CONSTRAINTS:
- Positive prompt: <= 75 tokens, comma-separated keywords, no sentences.
- Always include a negative prompt covering: blurry, low quality, watermark, text, extra fingers, deformed hands, jpeg artifacts.
- Match TemplateContext.styleProfile.palette exactly — use the listed color names.
- Use TemplateContext.styleProfile.culturalStyle and motifs verbatim where applicable.
- Never invent brand names, real people, or copyrighted characters.
- Output JSON matching the SdPromptBundle schema. No prose.
```

## Inference Defaults

| Param | Default | Notes |
|---|---|---|
| steps | 30 | 28-35 range; higher = slower, marginal quality |
| cfg | 7.5 | 6.5-8.5; higher = stricter prompt adherence |
| sampler | DPM++ 2M Karras | best speed/quality tradeoff |
| seed | random for prod, pinned for tests |
| width/height | match template canvas | SDXL: ≥ 1024 short side |

## Endpoint Adapter

```kotlin
interface SdEndpoint {
    suspend fun generate(bundle: SdPromptBundle): Result<GeneratedImage>
}

class ReplicateSdEndpoint(...) : SdEndpoint { ... }
class FalAiSdEndpoint(...) : SdEndpoint { ... }
class HuggingFaceSdEndpoint(...) : SdEndpoint { ... }
```

All clients enforce:
- 60s timeout, 1 retry with backoff
- Authorization via `BuildConfig` (never source)
- Response validated against `GeneratedImage` schema
- Failed generations return `Result.failure`, never throw

## Output

Single micro-task block per request:
- `SdPromptBundle.kt` + `ControlNetGuidance.kt`
- `PromptSynthesizer.kt` interface + impl
- `SdEndpoint.kt` interface + chosen adapter
- System prompt as `const val`
- Unit test with a fixture vision analysis

## Anti-Patterns

- Synthesizing prompts inside the ViewModel
- Hardcoding endpoint URL — use config
- Skipping the negative prompt
- Letting the LLM produce free-text ControlNet config
- Storing API keys in source
- No retry on a 5xx
