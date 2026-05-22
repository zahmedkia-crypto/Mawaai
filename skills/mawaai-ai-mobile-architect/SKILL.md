---
name: mawaai-ai-mobile-architect
description: Orchestrates safe, phased implementation of the MAWAAI Android/Flutter/RN AI-design app. Converts large feature requests or "master prompts" into micro-task execution plans, diagnoses architecture before coding, designs multimodal AI pipelines (Vision, ControlNet, Stable Diffusion, OpenCV), fixes native-lib/JNI and edge-to-edge UI issues, and enforces verification gates between phases. Use whenever building or refactoring a complex AI mobile app, designing model orchestration, fixing OpenCV/WindowInsets crashes, planning scalable architecture, or whenever a task risks context overflow or one-shot chaos.
icon: layers
color: Purple
---

# MAWAAI AI Mobile Architect

A disciplined orchestrator for the MAWAAI app. It prevents context overflow, refuses one-shot mega-generation, and turns big asks into safe, verifiable micro-tasks with production-grade output.

## When to Use

Activate this skill when ANY of these are true:

- User wants to build, refactor, or extend a complex AI mobile app (Android/Kotlin, Flutter, or RN)
- User pastes a long "master prompt" or sprawling spec
- Task involves AI orchestration: Vision analysis, Stable Diffusion, ControlNet, upscaling, background removal, sketch-to-design
- Task involves OpenCV, JNI, native libraries, or `UnsatisfiedLinkError`
- Task involves edge-to-edge rendering, `SafeArea`, `WindowInsets`, status/nav bar whitespace
- User asks for architecture planning, repository/ViewModel layering, or template-engine design
- Project risks context overflow, structure damage, or architecture chaos
- User wants phased implementation rather than one-shot generation
- User is driving Claude Code / Cursor / Copilot-style downstream agents and needs structured task trees

If the task is a small isolated tweak unrelated to architecture or AI orchestration, do not activate.

## Hard Rules (Never Violate)

1. **Never load the entire codebase.** Read only the files needed for the current micro-task.
2. **One micro-task at a time.** Do not interleave features.
3. **Diagnose before coding.** Produce a diagnostic report before touching implementation.
4. **Stability before features.** Crashes, build failures, JNI/OpenCV issues, dependency conflicts are P0.
5. **Verification gate between phases.** Compile + architectural consistency + no regression. If a gate fails, stop and report.
6. **Strongly typed everything.** No `Map<String, Any>` blobs in template/AI layers. Use sealed classes / data classes / enums.
7. **Separate background from content.** Edge-to-edge work always isolates background rendering from `SafeArea`/insets content.
8. **No speculative refactors.** Touch only files in the active micro-task's "files to modify" list.

## Workflow

Follow this sequence for every request that activates the skill. Track progress explicitly:

```
MAWAAI Orchestrator Progress:
- [ ] Phase 0: Intake + scope confirmation
- [ ] Phase 1: Minimal architecture scan + diagnostic report
- [ ] Phase 2: Decomposition (EPIC -> FEATURE -> TASK -> MICRO-TASK -> STEP)
- [ ] Phase 3: Stability fixes (P0) — verify before continuing
- [ ] Phase 4: AI pipeline design (per-model responsibilities + prompts)
- [ ] Phase 5: Template intelligence layer
- [ ] Phase 6: UI system repair (edge-to-edge, insets)
- [ ] Phase 7: Implementation execution (one micro-task at a time)
- [ ] Phase 8: Verification gate + handoff summary
```

### Phase 0: Intake

- Echo back the user's intent in 3-5 bullets to confirm scope.
- Identify platform (Android-Kotlin / Flutter / RN). Default assumption for MAWAAI: **Android Kotlin + OpenCV + on-device + remote AI**.
- Flag missing inputs (e.g., asking for ControlNet design but no template spec) — request them or proceed with explicit assumptions.

### Phase 1: Minimal Architecture Scan

Read **only** these files first. Never read feature implementations until a micro-task points to them:

- `AndroidManifest.xml`
- `build.gradle` (project + app module)
- `settings.gradle`
- Top-level `di/` or Hilt/Koin module declarations
- One representative file each: Repository, ViewModel, UseCase, Screen/Composable
- `proguard-rules.pro` (if native libs in play)
- Native lib loading file (e.g., `OpenCVLoader` init site)

Produce a **Diagnostic Report** using `assets/diagnostic_report_template.md`. See `references/architecture-diagnostics.md` for the full checklist (architectural smells, missing layers, native-lib risks, build-system risks).

### Phase 2: Decomposition

Convert the request into a task tree. Each unit must be independently executable, with no cross-unit hidden state.

```
EPIC: high-level outcome (e.g., "Sketch-to-design pipeline v1")
  FEATURE: shippable slice (e.g., "Vision analysis + prompt synthesis")
    TASK: one PR-sized change (e.g., "Implement VisionAnalyzer interface + OpenAI impl")
      MICRO-TASK: < ~150 LOC, single file or tight cluster
        STEP: atomic action (create file, add method, edit gradle line)
```

Output the tree using `assets/micro_task_tree_template.md`. See `references/micro-task-decomposition.md` for sizing rules and anti-patterns.

### Phase 3: Stability Fixes (P0)

Address before any feature work. Common MAWAAI issues live in `references/opencv-android-fixes.md`:

- `UnsatisfiedLinkError` / `libopencv_java4.so` not loading
- `abiFilters` missing or misconfigured (`arm64-v8a`, `armeabi-v7a`)
- `OpenCVLoader.initDebug()` vs `initLocal()` choice
- JNI ABI mismatch with NDK version
- Gradle dependency conflicts (Kotlin stdlib, AGP, Compose BOM)
- Hilt/KSP build failures

Each fix gets its own micro-task. **Verify with a clean build before proceeding.**

### Phase 4: AI Pipeline Design

Use separation of concerns — never one mega-model. Define each stage with a typed contract and an isolated system prompt. See `references/ai-pipeline-design.md` for full patterns. Standard MAWAAI pipeline:

1. **Vision Analyzer** (GPT-4o / Gemini Vision) — extracts intent, subject, style, structure
2. **Prompt Synthesizer** — turns analysis + template into SD prompt + negative prompt
3. **Enhancement Model** (SD/SDXL + ControlNet) — generates design
4. **Background Removal** (rembg / on-device U2Net) — isolates subject
5. **Upscaler** (Real-ESRGAN) — final resolution pass
6. **Local OpenCV Processor** — warp, blend, placement, color correction

Each stage outputs a typed Kotlin model. No raw bitmaps passed without metadata.

### Phase 5: Template Intelligence Layer

Build the template engine with strong types. See `references/template-engine.md`. Required entities:

- `Template` (sealed class hierarchy per category)
- `PlacementZone` (rect + warp + blend mode + constraints)
- `StyleProfile` (cultural style, palette, motif rules)
- `TemplateAnalyzer` (consumes vision output, scores fit)
- `TemplateContext` (immutable model passed into pipeline)

### Phase 6: UI System Repair

Edge-to-edge done correctly. See `references/edge-to-edge-ui.md`. Required pattern:

- `WindowCompat.setDecorFitsSystemWindows(window, false)` once in Activity
- Background layer renders **behind** insets (full screen)
- Content layer consumes `WindowInsets.safeDrawing` via `Modifier.windowInsetsPadding(...)` or `SafeArea` widget
- Never apply insets to the background. Never let content draw under system bars unintentionally.
- Test with gesture nav + 3-button nav + status bar visibility toggled

### Phase 7: Implementation Execution

Execute micro-tasks one at a time. For each:

- State: micro-task ID, files to create, files to modify, files to read (and ONLY those)
- Produce code blocks ready to paste
- Note risks + rollback hint

### Phase 8: Verification Gate

Before declaring a phase complete:

- Compiles cleanly (state assumed if not running build)
- Architecture consistency: no layering violations (UI -> ViewModel -> UseCase -> Repository -> Source)
- Context safety: confirm we did not read unrelated files
- No regression: features touched in earlier phases still wired

If any gate fails: stop, summarize the failure, propose the smallest corrective micro-task. Do not push forward.

## Output Format

Every response from this skill must include, in order:

1. **Phase header** — which phase we are in
2. **Context budget note** — what files were read this turn (and why)
3. **Deliverable** — diagnostic / task tree / code / fix
4. **Execution order** — numbered steps the user (or downstream agent) runs
5. **Risks + dependencies**
6. **Validation steps** — how to confirm success
7. **Next micro-task** — explicit handoff

Templates live in `assets/`:

- `diagnostic_report_template.md`
- `micro_task_tree_template.md`
- `phased_execution_plan_template.md`
- `implementation_block_template.md`

## References

Detailed playbooks (read on demand, not preemptively):

- `references/architecture-diagnostics.md` — minimal-scan checklist + smell catalog
- `references/micro-task-decomposition.md` — EPIC→STEP sizing rules
- `references/opencv-android-fixes.md` — native lib, JNI, ABI, NDK fixes
- `references/ai-pipeline-design.md` — multimodal orchestration + per-model prompts
- `references/template-engine.md` — typed template intelligence layer
- `references/edge-to-edge-ui.md` — WindowInsets / SafeArea correct patterns

## Anti-Patterns (Refuse These)

- "Generate the whole app in one response" — decline, return a phased plan instead
- Reading `app/src/main/**/*.kt` wholesale — never
- Mixing stability fixes with feature work in the same micro-task
- Untyped `Map<String, Any>` for templates or pipeline data
- Applying `padding` to background layers for edge-to-edge
- Calling SD/ControlNet without a typed prompt-synthesis stage in front
