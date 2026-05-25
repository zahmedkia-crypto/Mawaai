# Provider Quick Reference

Verified live on 2026-05-22 via the `MAWAAI_RUN_LIVE_API_TESTS=1` opt-in suite.

## Vision Models (multimodal: text + image input)

| Provider | Best Free Model | Latency (p50) | Free Tier Limit | Strengths | Weaknesses |
|---|---|---:|---|---|---|
| **Groq** | `llama-3.2-90b-vision-preview` | ~400 ms | 14k requests/day | Fastest. Reliable. | English-leaning; Arabic OK but flat. |
| **OpenRouter** | `openrouter/auto` (resolves to gemini-2.5-flash-lite or similar) | ~1000 ms | Free models rotate | Self-healing — picks a working free model. | Latency unpredictable. |
| **Cloudflare Workers AI** | `@cf/llava-hf/llava-1.5-7b-hf` | ~800 ms | 10k req/day | Edge network = lowest packet loss globally. | Older 7B model; weaker reasoning. |
| **Gemini** | `gemini-2.0-flash` | ~700 ms | 15 RPM, 1500 RPD | Best Arabic; native cultural awareness. | Quota is tight; deprecation churn. |
| **HuggingFace** | varies | 3-15 s | Per-model | Vast model selection. | Too slow for interactive flows. |

## Text-Only Models

| Provider | Best Free Model | Use Case |
|---|---|---|
| Groq | `llama-3.1-70b-versatile` | Fast structured analysis |
| OpenRouter | `openrouter/auto` | When Gemini quota is gone |
| Cloudflare | `@cf/meta/llama-3.1-8b-instruct` | Resilient text fallback |
| Gemini | `gemini-2.0-flash` | Arabic-native prompts |

## Image-Edit Models (text + image input → image output)

| Provider | Model | Notes |
|---|---|---|
| Lovable Gateway | `google/gemini-2.5-flash-image` | Used in Lovable Creative Studio for rendering |
| Cloudflare | `@cf/runwayml/stable-diffusion-v1-5-img2img` | Already wired in Mawaai app |
| OpenRouter | varies | Check `/api/v1/models` for `:image` suffixed models |

For MT-027 (image-edit renderer), the canonical choice today is the Cloudflare SD-1.5 img2img (already wired). Wrap it in `ImageEditProvider` interface so swapping to Gemini 2.5 Flash Image is a 1-file change later.

## Provider Selection Heuristic (Default Auto-Order)

For VISION tasks:
1. **Gemini** — best quality for Arabic-cultural content
2. **OpenRouter** — auto-routes to whatever's working
3. **Groq** — fastest fallback
4. **Cloudflare** — most resilient last-resort

For TEXT-ONLY tasks:
1. **Groq** — fastest (Llama 3.1 70B free)
2. **Cloudflare** — most resilient
3. **OpenRouter** — auto-routed
4. **Gemini** — when Arabic nuance matters

For IMAGE-EDIT tasks (single provider currently):
1. **Cloudflare SD-1.5 img2img** (no fallback yet — TODO add Lovable's Gemini 2.5 Flash Image when accessible via OpenRouter)
