# Phased Execution Plan — {{request_name}}

## Phase 0 — Intake
- Confirmed scope: {{bullets}}
- Assumptions: {{bullets}}
- Open questions: {{bullets}}

## Phase 1 — Diagnostics
- Deliverable: Diagnostic Report ({{P0_count}} P0, {{P1_count}} P1, {{P2_count}} P2)
- Gate: pass when P0s have proposed micro-tasks

## Phase 2 — Decomposition
- Deliverable: Micro-task tree ({{mt_count}} micro-tasks)
- Gate: every MT < 150 LOC and independently executable

## Phase 3 — Stability Fixes (P0)
- Micro-tasks: {{ids}}
- Gate: clean build + OpenCV init confirmed + no JNI crashes

## Phase 4 — AI Pipeline Design
- Contracts: VisionAnalyzer, PromptSynthesizer, EnhancementModel, BackgroundRemoval, Upscaler, OpenCvProcessor
- System prompts: drafted per model
- Gate: each stage has typed input + typed output + Result<T> return

## Phase 5 — Template Intelligence
- Types: Template sealed hierarchy, PlacementZone, StyleProfile, TemplateAnalyzer, TemplateContext
- Gate: zero `Map<String, Any>`; all `when` blocks exhaustive

## Phase 6 — UI Edge-to-Edge Repair
- Pattern: BackgroundLayer + ContentLayer with `safeDrawing`
- Gate: verification checklist passed (gesture nav, IME, rotation)

## Phase 7 — Implementation
- Strategy: one micro-task at a time, lowest layer first
- Gate: each MT verified before next starts

## Phase 8 — Handoff
- Deliverables: code blocks, validation steps, next-up MT
- Gate: regression check on prior phases
