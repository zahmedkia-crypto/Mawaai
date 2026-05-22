# Phased Execution Plan — {{request_name}}

## Phase 0 — Intake
- Confirmed scope: {{bullets}}
- Assumptions: {{bullets}}
- Open questions: {{bullets}}

## Phase 1 — Project Scan (specialist: mobile-project-scanner)
- Deliverable: Diagnostic Report ({{P0_count}} P0, {{P1_count}} P1, {{P2_count}} P2)
- Gate: P0s have proposed micro-tasks

## Phase 2 — Decomposition (specialist: microtask-orchestrator)
- Deliverable: Micro-task tree ({{mt_count}} micro-tasks)
- Gate: every MT < 150 LOC and independently executable

## Phase 3 — Stability Fixes (specialist: android-native-fixer)
- Micro-tasks: {{ids}}
- Gate: clean build + OpenCV init confirmed + no JNI crashes

## Phase 4 — AI Pipeline Design (specialists: multimodal-ai-orchestrator + vision-analysis-engineer + stable-diffusion-pipeline-builder)
- Contracts: VisionAnalyzer, PromptSynthesizer, EnhancementModel, BackgroundRemoval, Upscaler, OpenCvProcessor
- System prompts: drafted per model
- Gate: each stage has typed input + typed output + Result<T> return

## Phase 5 — Template Intelligence (specialist: template-intelligence-engine)
- Types: Template sealed hierarchy, PlacementZone, StyleProfile, TemplateAnalyzer, TemplateContext
- Gate: zero `Map<String, Any>`; all `when` blocks exhaustive

## Phase 6 — Architecture + API (specialists: repository-architecture-builder + mobile-ai-api-integrator)
- Deliverable: clean MVVM/MVI layering, typed API clients, retry/backoff
- Gate: no leaked DTOs past repositories; no API keys in source

## Phase 7 — UI Edge-to-Edge Repair (specialist: edge-to-edge-ui-fixer)
- Pattern: BackgroundLayer + ContentLayer with `safeDrawing`
- Gate: verification checklist passed (gesture nav, IME, rotation)

## Phase 8 — Implementation
- Strategy: one micro-task at a time, lowest layer first
- Gate: each MT verified before next starts

## Phase 9 — Performance + Production Audit (specialists: mobile-performance-guardian + production-readiness-auditor)
- Deliverables: bitmap/Mat safety, cache strategy, audit report
- Gate: zero P0 findings before release
