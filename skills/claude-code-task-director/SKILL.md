---
name: claude-code-task-director
description: Generates optimal execution instructions for downstream coding agents (Claude Code, Cursor, Copilot Workspace, Cody). Use when the user says "make a master prompt", "guide the coding agent", "write instructions for Cursor", or any meta-task that produces a prompt to feed another agent. Produces phased prompts with context-control instructions, micro-task sequences with stop-points, explicit allow/deny file lists, verification commands, and rollback hints. Wraps the output of the master orchestrator into prompts the downstream agent will obey.
icon: bot
color: Purple
---

# Claude Code Task Director

Bridges the master orchestrator's plan into prompts that downstream coding agents will execute correctly.

## When to Use

- "make a master prompt" / "guide Claude Code" / "instructions for Cursor"
- Handing off a micro-task tree to an autonomous coding agent
- Wrapping an implementation block for an external agent

## Output Structure (canonical)

Every downstream-agent prompt has this shape:

```
[ROLE]
You are a senior mobile engineer executing one micro-task. You will not exceed scope.

[CONTEXT]
Project: MAWAAI (Android Kotlin + OpenCV + AI pipeline)
Phase: <phase number and name>
Active micro-task: <MT-id> — <title>

[ALLOWED READS]
Read ONLY these files this task:
  - <path>
  - <path>
You may not open any other file unless explicitly directed.

[FILES TO CREATE]
  - <path> — <purpose>

[FILES TO MODIFY]
  - <path> — <exact change summary>

[STEPS]
1. <step>
2. <step>

[VERIFICATION]
Run: <command>
Confirm: <expected result>

[STOP]
After verification passes, STOP and report:
  - Files changed
  - Verification output
  - Any unexpected findings
Do NOT continue to the next micro-task.

[GUARDRAILS]
- Do not refactor unrelated code.
- Do not modify file paths outside FILES TO CREATE / MODIFY.
- Do not install new dependencies without explicit instruction.
- Do not commit. Stage only.
```

## Multi-Micro-Task Sequencing

When wrapping a sequence (rare — prefer one-at-a-time), include explicit checkpoints:

```
[SEQUENCE]
MT-001 → MT-002 → MT-003

[CHECKPOINT after MT-001]
Verify: ./gradlew assembleDebug succeeds.
If fail: STOP, do not proceed to MT-002.

[CHECKPOINT after MT-002]
Verify: <command>
...
```

## Agent-Specific Tweaks

| Agent | Tweak |
|---|---|
| Claude Code | Use bracket sections, no markdown. Include `[STOP]` to prevent overreach. |
| Cursor | Place `ALLOWED READS` early; Cursor respects context hints. |
| Copilot Workspace | Add explicit `acceptance criteria:` block matching their convention. |
| Cody | Use file-path tags `@<path>` in steps for inline context. |

## Verification Commands (canonical for MAWAAI)

- Compile: `./gradlew :app:assembleDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`
- Lint: `./gradlew :app:lintDebug`
- Format: `./gradlew ktlintFormat` (if configured)
- APK ABI check: `unzip -l app-debug.apk | grep libopencv`

## Output Per Request

Single self-contained prompt ready to paste into the downstream agent, plus:
- Expected duration estimate
- Failure recovery hint
- Next micro-task to run after this one passes verification

## Anti-Patterns

- Wrapping multiple unrelated micro-tasks in one prompt
- Vague "implement the feature" instructions
- Missing `[STOP]` — agent runs ahead and breaks scope
- Allowing full repository reads
- No verification command — agent claims success without proof
- Including the agent's chain-of-thought instructions (let the agent reason)
