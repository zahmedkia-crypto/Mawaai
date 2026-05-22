# Micro-Task Tree — {{epic_name}}

## EPIC: {{epic_id}} — {{outcome}}

### FEATURE: {{feature_id}} — {{slice}}

#### TASK: {{task_id}} — {{pr_description}}

- **MICRO-TASK {{mt_id}}**: {{single_change}}
  - Files to create: {{list or none}}
  - Files to modify: {{list or none}}
  - Files to read: {{strictly limited list}}
  - Dependencies: {{prior MT-ids}}
  - Risk: {{low | med | high — reason}}
  - Verification: {{compile / unit test / manual check}}
  - STEPs:
    1. {{atomic action}}
    2. {{atomic action}}

  ---

- **MICRO-TASK {{mt_id+1}}**: ...

## Execution Order (linearized)
1. MT-001
2. MT-002
3. ...

## Parallelizable Set (optional)
- MT-007 + MT-008 (no shared files)
