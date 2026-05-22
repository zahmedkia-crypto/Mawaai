# Micro-Task Decomposition

Convert any request into a strict EPIC → FEATURE → TASK → MICRO-TASK → STEP tree.

## Sizing Rules

| Level | Scope | Size | Example |
|---|---|---|---|
| EPIC | Outcome users perceive | weeks | "Sketch-to-design v1" |
| FEATURE | Shippable slice | days | "Vision analysis pipeline" |
| TASK | One PR | hours | "Add `VisionAnalyzer` interface + OpenAI impl" |
| MICRO-TASK | One file or tight cluster | < ~150 LOC | "Create `OpenAIVisionAnalyzer.kt`" |
| STEP | Atomic action | 1 line / 1 method | "Add `analyze(image: Bitmap)` method signature" |

## Independence Requirements

Each micro-task MUST:

1. Be executable without reading more than 3 files
2. Have a clearly stated "files to create" + "files to modify" + "files to read" list
3. Compile in isolation (use stubs/TODOs for not-yet-implemented dependencies)
4. State its verification step

## Decomposition Anti-Patterns

- Decomposing by file type ("all data classes first") — couples unrelated features
- Mega-micro-tasks (>150 LOC) — split further
- Hidden dependencies between micro-tasks — surface them as STEPs in the parent TASK
- Decomposing without naming the EPIC outcome — leads to scope drift

## Execution Order Heuristics

1. P0 stability fixes first (always)
2. Interfaces + data models before implementations
3. Lowest layer first: data source → repository → use case → ViewModel → UI
4. Mock / stub at boundaries so each layer ships independently
5. Feature flags for incomplete pipelines

## Output

Use `assets/micro_task_tree_template.md`. Each MICRO-TASK gets a stable ID (e.g., `MT-014`) used in handoff summaries.
