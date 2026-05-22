---
name: microtask-orchestrator
description: Converts large implementation requests into strict EPIC then FEATURE then TASK then MICRO-TASK then STEP trees with sizing rules, execution order, and verification gates. Use when the user says "build this app", "refactor whole project", pastes a multi-feature request, or any task that risks context overflow. Produces independently executable micro-tasks (under 150 LOC each, at most 3 file reads), execution checkpoints, and parallelizable sets. Prevents one-shot mega-generation and enforces phased execution.
icon: list-tree
color: Blue
---

# Micro-Task Orchestrator

Decomposes any large request into a strict 5-level tree. Each unit is independently executable with no hidden cross-unit state.

## When to Use

- "build this app" / "refactor whole project" / "implement this entire spec"
- Any single request that would require more than 3 file edits
- After `mobile-project-scanner` produces P0/P1 findings
- When a user pastes a giant prompt and expects one response

## Decomposition Tree

```
EPIC: outcome users perceive (weeks)
  FEATURE: shippable slice (days)
    TASK: one PR (hours)
      MICRO-TASK: under ~150 LOC, single file or tight cluster
        STEP: atomic action (create file, add method, edit gradle line)
```

## Sizing Rules

| Level | Max scope | Example |
|---|---|---|
| EPIC | Weeks | "Sketch-to-design pipeline v1" |
| FEATURE | Days | "Vision analysis pipeline" |
| TASK | Hours, one PR | "Add VisionAnalyzer interface + OpenAI impl" |
| MICRO-TASK | Under 150 LOC | "Create OpenAIVisionAnalyzer.kt" |
| STEP | 1 line / 1 method | "Add analyze(image: Bitmap) signature" |

## Independence Requirements (Every Micro-Task)

1. Executable with at most 3 file reads
2. Explicit lists: **files to create**, **files to modify**, **files to read**
3. Compiles in isolation — use stubs/TODOs for unfinished dependencies
4. Stated verification step
5. Stable ID (`MT-014`) for cross-skill handoff

## Execution Order Heuristics

1. **P0 stability fixes first** — always
2. Interfaces + data models before implementations
3. Lowest layer first: data source then repository then use case then ViewModel then UI
4. Mock / stub at boundaries so each layer ships independently
5. Feature flags for incomplete pipelines

## Anti-Patterns

- Decomposing by file type ("all data classes first") — couples unrelated features
- Mega-micro-tasks (over 150 LOC) — split further
- Hidden dependencies between micro-tasks — surface them as STEPs in the parent TASK
- Decomposing without naming the EPIC outcome — leads to scope drift
- Putting stability fixes and feature work in the same micro-task

## Output

Use the master orchestrator's `micro_task_tree_template.md`. Always include:
- Full tree with stable IDs
- Linearized execution order
- Parallelizable set (micro-tasks with no shared files)
- Dependencies graph (which MT unblocks which)

## Verification Gate (after each micro-task completes)

- Compiles cleanly
- Verification step from the MT spec passes
- No file outside the MT's "to modify" list was touched
- Hand back control to master orchestrator for next-MT selection
