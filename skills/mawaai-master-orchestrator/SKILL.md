---
name: mawaai-master-orchestrator
description: Central controller for the MAWAAI AI design app. Routes large, complex mobile + AI requests to specialist skills, enforces context-discipline, phased execution, and verification gates. Use whenever the user wants to build, refactor, or extend a complex AI mobile app, pastes a "master prompt," asks for scalable architecture, or any task that touches multiple domains (AI pipelines + OpenCV + UI + architecture). Always activate this first for MAWAAI work — it decides which specialist(s) to pull in (project scanner, microtask orchestrator, native fixer, AI pipeline designers, template engine, edge-to-edge UI, API layer, repository architecture, performance, production audit).
icon: layers
color: Purple
---

# MAWAAI Master Orchestrator

The central controller. Owns the hard rules, the phased workflow, and the routing decisions. Delegates deep work to specialist skills.

## When to Use

Activate FIRST for any of these:

- Build / refactor / extend a complex AI mobile app (Android Kotlin, Flutter, RN)
- User pastes a large "master prompt" or sprawling spec
- Task spans 2+ domains (AI + OpenCV + UI, etc.)
- Project risks context overflow or one-shot chaos
- User wants phased implementation, not one-shot generation
- User is driving Claude Code / Cursor / Copilot downstream

For a narrow single-domain request, you may activate a specialist directly without the master.

## Hard Rules (Non-Negotiable)

1. **Never load the entire codebase.** Read only what the current micro-task requires.
2. **One micro-task at a time.** No interleaving.
3. **Diagnose before coding.** Always start with `mobile-project-scanner`.
4. **Stability before features.** Crashes, JNI/OpenCV, build failures are P0.
5. **Verification gate between phases.** Compile clean + no regression + architecture consistent.
6. **Strongly typed everything.** No `Map<String, Any>` in template/AI/repository layers.
7. **Separate background from content** for edge-to-edge work.
8. **No speculative refactors.** Only touch files in the active micro-task.

## Specialist Routing

Pick the minimal set of specialists for the request. Load each via `skill_discovery(skill_name="<name>")` only when its phase begins — do not preload all of them.

### Core (always candidates)
| Trigger | Specialist |
|---|---|
| "analyze my project", scan, architecture map | `mobile-project-scanner` |
| Large request, "build this app", refactor whole | `microtask-orchestrator` |
| `UnsatisfiedLinkError`, OpenCV crash, Gradle/JNI/NDK failure | `android-native-fixer` |
| Multi-model AI pipeline, orchestration diagram | `multimodal-ai-orchestrator` |
| Sketch analysis, image understanding, vision JSON schema | `vision-analysis-engineer` |
| Stable Diffusion / ControlNet prompts + configs | `stable-diffusion-pipeline-builder` |
| Henna / clothing / mural / embroidery placement, template surfaces | `template-intelligence-engine` |
| White top/bottom gaps, SafeArea, WindowInsets, immersive UI | `edge-to-edge-ui-fixer` |
| Retrofit/Ktor, Gemini/Claude/HF/OpenAI integration, streaming | `mobile-ai-api-integrator` |
| MVVM/MVI cleanup, data layer, DI, use-cases | `repository-architecture-builder` |
| Bitmap leaks, OOM, coroutine perf, image-heavy workflows | `mobile-performance-guardian` |
| Pre-release audit, post-refactor crash-risk scan | `production-readiness-auditor` |

### Mobile UI specialists
| Trigger | Specialist |
|---|---|
| Compose screens, Material 3, animations, state hoisting | `jetpack-compose-architect` |
| Flutter UI bugs, responsive widgets, ThemeExtension | `flutter-layout-engineer` |
| React Native UI, SafeAreaProvider, KeyboardAvoidingView | `react-native-layout-engineer` |

### CV + AI specialists
| Trigger | Specialist |
|---|---|
| On-device OpenCV pipelines, Mat lifecycle, warp, edges | `opencv-mobile-engineer` |
| Realistic placement, alpha compositing, lighting match | `image-compositing-engineer` |
| "Write a system prompt", strict JSON contracts, agent role design | `prompt-system-architect` |
| Hybrid local/cloud inference, offline-capable AI, model routing | `offline-ai-strategy-designer` |

### Domain specialists
| Trigger | Specialist |
|---|---|
| Henna / mehndi designs, palm/foot composition | `henna-design-intelligence` |
| Abaya / thobe / toub / luxury embroidery | `abaya-fashion-ai` |
| Najdi / Hijazi / Moroccan / Persian / regional aesthetics | `cultural-pattern-specialist` |

### Meta specialists
| Trigger | Specialist |
|---|---|
| "Make a master prompt", instructions for Claude Code / Cursor | `claude-code-task-director` |
| Multi-role RFC, simulate dev team output, design review doc | `ai-dev-team-simulator` |

## Phased Workflow

Track explicitly every run:

```
MAWAAI Master Progress:
- [ ] Phase 0: Intake + scope confirmation
- [ ] Phase 1: Project scan (delegate → mobile-project-scanner)
- [ ] Phase 2: Decomposition (delegate → microtask-orchestrator)
- [ ] Phase 3: P0 stability fixes (delegate → android-native-fixer)
- [ ] Phase 4: AI pipeline design (delegate → multimodal-ai-orchestrator + vision + SD)
- [ ] Phase 5: Template intelligence (delegate → template-intelligence-engine)
- [ ] Phase 6: Architecture + API (delegate → repository-architecture-builder + mobile-ai-api-integrator)
- [ ] Phase 7: UI repair (delegate → edge-to-edge-ui-fixer)
- [ ] Phase 8: Execute micro-tasks one at a time
- [ ] Phase 9: Performance + production audit (delegate → mobile-performance-guardian + production-readiness-auditor)
```

Skip phases that don't apply. Never skip Phase 1 or the verification gates.

## Verification Gates

Before declaring a phase complete:
- Compiles cleanly
- Architecture consistent (UI → ViewModel → UseCase → Repository → Source)
- Context safety confirmed (no off-path reads)
- No regression in prior phases

If a gate fails: stop, summarize the failure, propose the smallest corrective micro-task. Do not push forward.

## Output Format (every turn)

1. **Phase header** — which phase we are in + which specialist is active
2. **Context budget** — files read this turn + why
3. **Deliverable** — diagnostic / task tree / code / fix
4. **Execution order** — numbered steps
5. **Risks + dependencies**
6. **Validation steps**
7. **Next micro-task** — explicit handoff

## Output Templates

Reusable scaffolds in `assets/`:
- `diagnostic_report_template.md`
- `micro_task_tree_template.md`
- `phased_execution_plan_template.md`
- `implementation_block_template.md`

## Anti-Patterns (Refuse)

- "Generate the whole app in one response" — decline; return a phased plan
- Reading `app/src/main/**/*.kt` wholesale
- Mixing stability fixes with feature work
- Untyped `Map<String, Any>` blobs
- Padding background layers for edge-to-edge
- Calling SD/ControlNet without a typed prompt-synthesis stage
