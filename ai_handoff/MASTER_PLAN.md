# MAWAAI — Master Execution Plan for Downstream AI Agent

**Audience:** AI coding agent (DeepSeek V3.x / Claude Sonnet / GPT-4o / etc.)
**Purpose:** Finish the MAWAAI app's remaining backlog without context overflow, without speculative refactors, and without breaking the existing green build.
**Date generated:** 2026-05-22
**Author:** Orchestrator agent (mawaai-master-orchestrator skill) handing off to downstream.

You are the implementer. The orchestrator has already done the architecture diagnostic and decomposed the remaining work into atomic micro-tasks. Your job is to execute one micro-task at a time, exactly as specified.

---

## 1. Project Context (Read Once, Memorize)

### What MAWAAI Is
A Kuwaiti Arabic-first Android app that turns user sketches/uploads into professional designs for:
- **Henna** — palm, hand, foot placement with cultural patterns
- **Abaya / Thobe / Toub** — luxury embroidery overlay on garments
- **Walls** — mural mockups for room placement
- Other domain art

The pipeline is: **Vision Analysis → Prompt Synthesis → Stable Diffusion / ControlNet → Background Removal → Upscaling → OpenCV Compositing → Template Overlay**.

### Tech Stack
| Layer | Tech |
|---|---|
| Language | Kotlin 2.1.0 |
| Build | AGP 8.7.3, Gradle KTS |
| Compile/Target SDK | 34 |
| Min SDK | 26 |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt + KSP |
| DB | Room (schema export configured at `app/schemas/`) |
| Net | Retrofit + OkHttp + Gson |
| Image | Coil, Glide |
| AI | OpenCV 4.x, TensorFlow Lite, ML Kit Subject Segmentation |
| Remote AI | Gemini, HuggingFace (RMBG-1.4 / ControlNet-Canny / Real-ESRGAN), Cloudflare Workers AI (Llama 3.1 + SD 1.5 img2img), Remove.bg, OpenRouter (fallback) |
| Module | Single `:app` |

### Repository
- URL: `https://github.com/zahmedkia-crypto/Mawaai.git`
- Default branch: `master`
- Package: `com.mawaai.love.app`
- Current state: build PASSES (`./gradlew assembleDebug` + `./gradlew test`)

### Existing Architecture
```
app/src/main/java/com/mawaai/love/app/
├── core/                         # logging, common utilities
├── data/
│   ├── dao/                      # Room DAOs
│   ├── database/                 # MawaaiDatabase + Converters
│   ├── model/                    # Domain models
│   ├── remote/                   # Retrofit clients (NOT pexels — see backlog)
│   └── repository/               # Repository impls
├── design/
│   ├── ai/
│   │   ├── gemini/               # GeminiClient + GeminiVisionClient (Arabic prompts + vision)
│   │   ├── openrouter/           # ← NEW (added 2026-05-22) drop-in Gemini fallback
│   │   ├── pipelines/            # multi-stage AI pipelines
│   │   ├── AIEngineImpl.kt
│   │   ├── DrawingActionEngine.kt
│   │   └── LocalDrawingAnalyzer.kt
│   ├── canvas/engine/            # Canvas + Brush engines
│   └── ...
├── di/                           # Hilt modules (DatabaseModule, CoroutineScopesModule, DataStoreModule, ...)
└── ui/
    ├── navigation/NavGraph.kt    # splash, intro, onboarding, home, memories, letters, mood, settings, design
    └── ...
```

### Critical Files You'll Hear About (DO NOT preload these — read only when an MT requires)
- `MawaaiApp.kt` — Application class, eager OpenCV init
- `NavGraph.kt` — Route table
- `AIEngineImpl.kt` — Top-level AI orchestrator (large file)
- `pipelines/*.kt` — Stage implementations (large, sensitive)
- `canvas/engine/BrushEngine.kt`, `canvas/engine/CanvasEngine.kt` — Drawing engine (very large)

---

## 2. The 8 Hard Rules (Non-Negotiable)

These are inherited from the `mawaai-master-orchestrator` skill. **If you violate any of these, your output will be rejected and rolled back.**

1. **Never load the entire codebase.** Read only the files the active micro-task explicitly lists in its "Files to read" section. Do not browse, do not list directories speculatively.

2. **One micro-task at a time.** Do not work on two MTs in the same response. Do not anticipate the next MT.

3. **Diagnose before coding.** Read the existing file completely before producing a diff. If the file is large, summarize its current structure in 5–10 bullets first, then propose the diff.

4. **Stability before features.** If the current build is failing, fix that first. Crashes, build failures, JNI/OpenCV issues, dependency conflicts are P0 and pre-empt feature work.

5. **Verification gate between phases.** After your change: `./gradlew assembleDebug` must PASS and `./gradlew test` must PASS. If you cannot run these, state this explicitly in your response so the human runs them.

6. **Strongly typed everything.** No `Map<String, Any>` in template, AI, repository, or DI layers. Use sealed classes, data classes, enums, value classes. If you need a heterogeneous payload, design a sealed hierarchy.

7. **Separate background from content** for edge-to-edge UI work. Background renders behind insets; content consumes `WindowInsets.safeDrawing`. Never apply padding to background layers.

8. **No speculative refactors.** Touch only files in the active MT's "Files to modify" list. Do not "while I'm here" anything. Do not rename, do not reorganize, do not migrate. If you see something else broken, write it down for a future MT — do not fix it now.

---

## 3. Required Output Format (Every Response)

When you respond to an MT prompt, your message MUST include these sections **in this order**:

```
## Phase Header
Active phase: <e.g., Phase 6 — Architecture + API>
Active specialist: <e.g., mobile-ai-api-integrator>

## Context Budget
Files I read this turn (and only these):
- <path>: <why>
- ...

Files I deliberately did NOT read:
- <path>: <reason>

## Diagnostic Summary
<3-10 bullets summarizing the current state of the files you read>

## Diff / Files

### File: <path>
```kotlin (or json/xml/gradle.kts)
<full new file content OR a precise diff block>
```
### File: <path>
```...
<...>
```

## Verification Plan
After applying these changes, the human must run:
1. <command>
2. <command>
3. <manual check, if any>

Expected outcome: <one sentence>

## Risks + Rollback
- Risk 1: <...>
- Rollback: `git restore <files>` or `git revert <sha>`

## Commit Message
```
MT-XXX: <one-line summary>

Phase: <phase>
Specialist: <skill>
Files added: N | modified: M | deleted: K

Verification passed:
- [ ] gradlew assembleDebug (run by human)
- [ ] gradlew test (run by human)
- [ ] no API keys in diff
- [ ] only files in MT scope touched
- [ ] strongly typed (no Map<String, Any>)

Next: MT-YYY
```

## Next Micro-Task
The next MT in the backlog (do not start it — just name it).
```

If you skip any section, the human is instructed to reject your output and re-prompt.

---

## 4. Quality Gates — How To Self-Check Before Responding

Before you send your response, run through this checklist privately:

### Code quality gates
- [ ] All new types are data classes / sealed classes / enums (no anonymous Maps)
- [ ] All public APIs have KDoc explaining when to use them
- [ ] All `runCatching` / `try` blocks have a meaningful failure log (Tag, message, exception)
- [ ] No `println` (use Android `Log` or the project's `AppLogger` if it exists in the file you read)
- [ ] No `!!` unless the contract guarantees non-null and you commented why
- [ ] No `lateinit var` outside DI/lifecycle (use `by lazy {}` instead)
- [ ] Coroutine work happens inside `withContext(Dispatchers.IO)` for I/O
- [ ] No `runBlocking` in production code
- [ ] No hard-coded API keys (always `BuildConfig.X_KEY` reading from `local.properties`)
- [ ] Imports are sorted (Kotlin first, then Android, then third-party, then java)

### Scope discipline gates
- [ ] Every file you touched is in the MT's "Files to modify" list
- [ ] You did not read any file outside the "Files to read" list
- [ ] You did not rename, move, or reorganize any file
- [ ] You did not add a new dependency unless the MT explicitly authorizes it
- [ ] You did not modify `settings.gradle.kts`, `build.gradle.kts` (root), `gradle.properties`, or version catalogs unless the MT authorizes

### Security gates
- [ ] No API key values, secrets, or tokens appear anywhere in the diff
- [ ] No `Log.d(...)` of request bodies, headers, or response payloads that may contain keys
- [ ] No file written that could be committed accidentally (e.g., `local.properties`, `.env`)
- [ ] No new `INTERNET`-elevated permission added without justification

### Build safety gates
- [ ] If you modified a Retrofit interface, the Retrofit base URL still matches
- [ ] If you added a `@Singleton @Inject constructor` class, the constructor has no parameters Hilt can't resolve
- [ ] If you added a Room entity/dao, the migration story is documented (do not add destructive migrations)
- [ ] If you modified Compose, you did not introduce a state read inside a `remember{}` block

If any gate fails, fix it before sending. Do not send a half-finished output.

---

## 5. Anti-Patterns — Refuse These

If the human asks for any of these, **refuse explicitly** and propose an alternative:

| Anti-pattern | Refusal phrase |
|---|---|
| "Generate the whole app in one response" | "I'll produce a phased plan instead — see backlog MT-XXX." |
| "Just read every file under app/src/main/" | "That violates Hard Rule #1. I'll read only the files the active MT needs." |
| "Fix this bug AND add this feature in one commit" | "Those are separate MTs. I'll do the bug fix first, then propose the feature MT separately." |
| "Add a `Map<String, Any>` payload" | "I'll model this as a sealed class hierarchy to preserve type safety." |
| "Apply padding to the background layer for edge-to-edge" | "Background must render full-screen behind insets; only content consumes `WindowInsets.safeDrawing`." |
| "Call SD/ControlNet directly without a prompt-synthesizer stage" | "I'll insert a typed prompt-synthesizer in front to keep the pipeline contract clean." |
| "Add a `fallbackToDestructiveMigration()` to fix the schema error" | "That destroys user data. I'll write a proper Migration class instead." |

---

## 6. Backlog — Map Of All Remaining Work

Each MT has a one-line summary here; full executable prompts are in `PROMPTS.md`.

| ID | P | Title | Phase | Specialist | Files touched | Time |
|---|---|---|---|---|---|---|
| **MT-013** | P1 | Wire OpenRouter fallback at GeminiClient call sites | 6 | mobile-ai-api-integrator | 1–3 | 15 min |
| **MT-014** | P1 | Audit GeminiVisionClient.kt for deprecated `-latest` model names | 9 | production-readiness-auditor | 1 | 10 min |
| **MT-006** | P2 | thob_sudani template metadata scaffold | 5 | template-intelligence-engine | 1 | 10 min |
| **MT-007** | P2 | API key hygiene audit + `.gitignore` review | 9 | production-readiness-auditor | 0–2 | 20 min |
| **MT-008** | P2 | Compose deprecation cleanup | 7 | jetpack-compose-architect | <10 | 30 min |
| **MT-010** | P2 | On-device template QA for 28 default_estimate entries | 5 | template-intelligence-engine + image-compositing-engineer | 2 (JSONs) | manual on-device, 1–2 hr |

Execution order (top to bottom):
1. **MT-013** first — small, additive, unlocks Gemini quota safety
2. **MT-014** — quick audit, may produce 0–1 line fix
3. **MT-006** — independent data-only change
4. **MT-007** — independent audit
5. **MT-008** — independent UI cleanup
6. **MT-010** — final on-device pass before release; cannot be done by AI alone

---

## 7. Backlog — Items Formally Deferred (Do Not Touch)

These were considered and explicitly removed from this handoff. Do not regress them:

| ID | Title | Reason deferred |
|---|---|---|
| F-001 | Restore Pexels integration | Not in current source; was in old PROJECT_LOG. Formally backlogged. |
| F-002 | Supabase / Ktor cloud sync | Not in current source. Treat as EPIC requiring its own RFC. |
| F-003 | Cards / photo-card / music / wishes / countdown / quiz / story screens | Not in current source. Backlog. |
| F-004 | PEXELS_API_KEY BuildConfig field cleanup | Cosmetic; do not touch unless asked. |

If the human prompts you to restore any of these, respond: "This is F-XXX in the deferred backlog. Confirm you want a full EPIC plan first, or remove this from deferred status."

---

## 8. Verification Gates — End-of-MT Acceptance Criteria

After applying any MT's diff, all of these must hold. The human runs them.

### Build gates
```bash
./gradlew clean
./gradlew assembleDebug    # MUST PASS
./gradlew test             # MUST PASS
./gradlew lintDebug        # WARNINGS OK, no new errors
```

### Diff gates (the human runs `git diff` and checks)
- [ ] No file outside the MT's "Files to modify" list was changed
- [ ] No API key value, no secret, no token appears in the diff
- [ ] Every new type is data class / sealed class / enum / value class
- [ ] Every new public function has a KDoc
- [ ] No `Map<String, Any>` in template, AI, repository layers
- [ ] No new `runBlocking`, no new `lateinit var` in non-DI code
- [ ] Imports sorted (or at least: no glob imports of `*` for kotlinx.*, android.*, javax.*)

### Behavioural gates
- [ ] If a public API contract was changed, all call-sites compile (Kotlin compiler enforces this)
- [ ] If a DI binding was added, the dependency graph compiles (KSP enforces this)
- [ ] If a Room schema changed, a migration class is present

### Commit gates
- [ ] Commit message follows the template in Section 3
- [ ] Commit message lists files added/modified/deleted with exact counts
- [ ] Commit message names the next MT

---

## 9. Tooling — What The Agent Can Use

| Tool | When to use |
|---|---|
| `cat <file>` / IDE file viewer | To read a specific file in the MT's "Files to read" list |
| `grep -rn "<pattern>" app/src/main/` | To find a specific symbol (use sparingly; prefer the MT-supplied location) |
| `./gradlew tasks` | To list available Gradle tasks |
| `./gradlew :app:dependencies` | To inspect the dependency graph (read-only) |

Do **not** use these:
- `find` / `ls` recursive across `app/src/main/` (violates Hard Rule #1)
- Auto-fix / auto-import IDE features that modify files outside MT scope
- Any "refactor → rename" or "refactor → move" feature

---

## 10. Specialist Skill Routing — When To Apply Which Discipline

Each MT names its primary specialist. The skills live in `skills/`. If you're unsure how to handle a specific concern, consult the matching SKILL.md:

| Concern | Skill |
|---|---|
| AI pipeline orchestration (multi-stage) | `multimodal-ai-orchestrator` |
| Stable Diffusion / ControlNet prompts | `stable-diffusion-pipeline-builder` |
| Vision JSON schemas, image understanding | `vision-analysis-engineer` |
| Strict JSON contracts, agent role design | `prompt-system-architect` |
| Hybrid local/cloud inference, model routing | `offline-ai-strategy-designer` |
| Retrofit, Gemini/Claude/HF/OpenAI integration | `mobile-ai-api-integrator` |
| MVVM/MVI, repository, use-cases, DI | `repository-architecture-builder` |
| OpenCV, Mat lifecycle, warp, edges | `opencv-mobile-engineer` |
| Realistic alpha compositing, lighting match | `image-compositing-engineer` |
| Henna / mehndi composition | `henna-design-intelligence` |
| Abaya / thobe / luxury embroidery | `abaya-fashion-ai` |
| Najdi/Hijazi/Moroccan/Persian patterns | `cultural-pattern-specialist` |
| Template surface design (placement zones) | `template-intelligence-engine` |
| Compose UI, Material 3, animations | `jetpack-compose-architect` |
| White top/bottom gaps, SafeArea, immersive | `edge-to-edge-ui-fixer` |
| Bitmap leaks, OOM, image-heavy workflows | `mobile-performance-guardian` |
| Pre-release audit, post-refactor scan | `production-readiness-auditor` |
| `UnsatisfiedLinkError`, OpenCV/JNI crashes | `android-native-fixer` |
| Master prompts, downstream agent instructions | `claude-code-task-director` |

---

## 11. How To Handle Ambiguity

If an MT's prompt is unclear or you need a value the prompt does not provide:

1. **State the ambiguity explicitly** in your response (do not guess).
2. **Propose the smallest reasonable default** and explain why.
3. **Mark the choice as a TODO comment** in the code: `// TODO(MT-XXX): confirm <choice> with human`.
4. **Do not block** on it — proceed with the default so the build stays green.

You should never silently invent: model names, API endpoints, package paths, package versions, file locations, env var names, secrets, or domain logic.

---

## 12. End State — When This Plan Is Done

When all 6 MTs in Section 6 are committed and verified, the project is at the **"production-ready for v1.0.0"** waypoint. The next phase (out of this handoff scope) is:

- v1.0.0 release: signed build, Play Store assets, version bump, tag
- Post-launch: telemetry, crash reporting, user feedback intake
- v1.1.0: deferred backlog (F-001 through F-004) by EPIC

---

**End of Master Plan. Proceed to `PROMPTS.md` and run one MT at a time.**
