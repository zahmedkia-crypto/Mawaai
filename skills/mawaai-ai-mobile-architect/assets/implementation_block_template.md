# Implementation Block — {{mt_id}} {{title}}

**Phase:** {{phase_number}} — {{phase_name}}
**Context budget:** read {{N}} files this turn → {{list}}

## Files to Create
- `{{path}}` — {{purpose}}

## Files to Modify
- `{{path}}` — {{exact change summary}}

## Files to Read (and ONLY these)
- `{{path}}`

## Code

### `{{path/to/NewFile.kt}}`
```kotlin
// full file contents, production-ready
```

### Patch to `{{path/to/ExistingFile.kt}}`
```kotlin
// show only the changed region with 3 lines of context above/below
```

## Execution Order
1. Create files
2. Apply patch(es)
3. Run validation step

## Risks
- {{risk}} → mitigation: {{action}}

## Dependencies
- Requires: {{prior MT-ids}}
- Unblocks: {{next MT-ids}}

## Validation
- [ ] Compiles (`./gradlew {{module}}:assembleDebug`)
- [ ] {{unit / instrumentation check}}
- [ ] No regression in {{related feature}}

## Next Micro-Task
**{{next_mt_id}}** — {{next_title}}
