# MAWAAI — AI Agent Handoff Package

This folder is a **complete, self-contained execution plan** for a downstream AI coding agent (DeepSeek V3.x / Claude Code / Cursor / Continue.dev) to finish the MAWAAI Android app's remaining backlog with **production-grade quality** and **without context overflow**.

It is the operational output of the `mawaai-master-orchestrator` + `claude-code-task-director` skills applied to today's project state (2026-05-22).

---

## 📖 Read In This Order

| Order | File | Purpose | Read Time |
|---|---|---|---|
| 1 | [`MASTER_PLAN.md`](./MASTER_PLAN.md) | Project context + complete map + rules + quality gates | 10 min |
| 2 | [`PROMPTS.md`](./PROMPTS.md) | Ready-to-paste prompts for every remaining micro-task | scan as needed |
| 3 | [`VERIFICATION.md`](./VERIFICATION.md) | How **you** (the human) verify each task was done correctly | 5 min |

That's it. Three files, total ~15 min reading. Everything the AI needs is in `MASTER_PLAN.md` + the specific prompt in `PROMPTS.md`.

---

## 🚀 How To Use This Package

### Step 1 — Set up the AI agent

You can use any of these (DeepSeek V3.x recommended, free via OpenRouter):

**Option A — Cursor / Continue.dev / Claude Code**
1. Open the Mawaai repo in your IDE
2. Configure the agent to use DeepSeek V3.x (or Claude Sonnet 4 / GPT-4o)
3. Paste `MASTER_PLAN.md` content into the agent's "system prompt" or "rules" field
   (in Cursor: `.cursorrules`; in Claude Code: append to existing project rules)

**Option B — Chat-only (ChatGPT / Claude.ai web / DeepSeek chat)**
1. Open a fresh conversation
2. Paste `MASTER_PLAN.md` as the first message
3. The agent will respond "ready — which micro-task should I start?"

### Step 2 — Run one micro-task at a time

For each micro-task in the backlog (MT-006, MT-007, MT-008, MT-013, MT-014, MT-010):

1. Open `PROMPTS.md`
2. Copy the entire prompt block for that MT
3. Paste it into the agent
4. The agent will produce the diff/files and explain what changed
5. **Open `VERIFICATION.md`** and run the verification checklist for that MT
6. **Only commit if ALL verification steps pass**
7. Move to the next MT

### Step 3 — Never skip the verification gate

If any verification step fails, the agent broke the rules. Either:
- Tell it specifically which check failed and ask it to fix
- Roll back the change (`git restore .`) and start fresh with the same prompt

### Step 4 — Commit message protocol

Each commit message must follow this format (the prompts enforce this):

```
MT-XXX: <one-line summary>

Phase: <phase number + name>
Specialist: <which skill governs this>
Files added: N
Files modified: M
Files deleted: K

Verification passed:
- [x] gradlew assembleDebug clean
- [x] gradlew test clean
- [x] no API keys in diff
- [x] no files read outside micro-task scope
- [x] strongly typed (no Map<String, Any>)
- [x] zero regression in earlier MTs

Next: MT-YYY
```

---

## 🎯 What "100% Accuracy" Means Here

It is **mathematically impossible** to guarantee any AI produces 100% bug-free code on first attempt. What this package guarantees is:

1. **100% scope discipline** — the agent will not touch files outside the explicit "files to modify" list of each MT, because the prompt lists them.
2. **100% regression safety** — the verification gate is a hard `./gradlew assembleDebug && ./gradlew test` PASS requirement. If that fails, you do not commit.
3. **100% architectural consistency** — strongly-typed-everything is a hard rule; no `Map<String, Any>` allowed in any reviewed diff.
4. **100% reversibility** — every MT is designed as an atomic commit. `git revert <sha>` undoes any single MT cleanly.
5. **100% auditability** — every commit message documents what was read, what changed, and what was verified, so you can review the full chain.

If a bug slips past the verification gate, it is a verification-checklist defect, not an agent defect. Update `VERIFICATION.md` and re-run that MT's tests.

---

## 🛑 The 8 Hard Rules (Excerpt — Full Versions in MASTER_PLAN.md)

The downstream agent **must not violate** any of these:

1. **Never load the entire codebase.** Read only the files the active MT explicitly lists.
2. **One micro-task at a time.** No interleaving features.
3. **Diagnose before coding.** Read the file before modifying it.
4. **Stability before features.** Crashes, build failures, JNI/OpenCV issues are P0.
5. **Verification gate between phases.** Build + test must pass.
6. **Strongly typed everything.** No `Map<String, Any>` in template/AI/repository layers.
7. **Separate background from content.** Edge-to-edge work always isolates rendering layers.
8. **No speculative refactors.** Touch only files in the active MT's list.

---

## 📊 Remaining Backlog At A Glance

| ID | P | Title | Owner skill | LOC est. | Time est. |
|---|---|---|---|---|---|
| MT-013 | P1 | Wire OpenRouter fallback at GeminiClient call sites | `mobile-ai-api-integrator` | <50 | 15 min |
| MT-014 | P1 | Audit GeminiVisionClient.kt for deprecated model names | `production-readiness-auditor` | <20 | 10 min |
| MT-006 | P2 | thob_sudani template metadata scaffold (5 entries) | `template-intelligence-engine` | data only | 10 min |
| MT-007 | P2 | API key hygiene audit + `.gitignore` review | `production-readiness-auditor` | <30 | 20 min |
| MT-008 | P2 | Compose deprecation pass | `jetpack-compose-architect` | <100 | 30 min |
| MT-010 | P2 | On-device template QA for 28 default_estimate entries | `template-intelligence-engine` | data only | manual |

**Total estimated agent time: ~90 min of focused work** spread across 5 atomic commits.

The full task tree, scope, files, verification commands, and ready-to-paste prompts are in the next two documents.

---

## 🔗 Cross-References

- Master orchestrator skill: `skills/mawaai-master-orchestrator/SKILL.md`
- All 26 specialist skills: `skills/*/SKILL.md`
- Previous diagnostic: `PROJECT_SCAN_2026-05-22.md`
- Continuation diagnostic: `PROJECT_SCAN_CONTINUATION_2026-05-22.md`
- API health report: `API_HEALTH_2026-05-22.md`

---

**Generated:** 2026-05-22
**Owner skill:** `claude-code-task-director` + `mawaai-master-orchestrator`
**Target downstream agent:** DeepSeek V3.x (free via OpenRouter), Claude Sonnet, GPT-4o, or any Claude Code-compatible coding agent
