# CI Workflow

This directory contains the **template** for the project's CI workflow. The
agent that authored MT-009 (PR #13) could not commit directly into
`.github/workflows/` because the integration token lacks GitHub's `workflow`
scope. One manual step is required by the project owner:

```bash
mkdir -p .github/workflows
cp docs/ci-workflow.yml.template .github/workflows/ci.yml
git add .github/workflows/ci.yml
git commit -m "ci: install workflow from docs template (MT-009 part 2)"
git push
```

After that the workflow runs on every push / PR — see the contents of the
template for the job matrix.

## Why a separate file at all?

So the YAML is reviewable as part of this PR and is committed to the repo
in a form that does not need elevated GitHub permissions. The maintainer's
one-time copy step requires a token with the `workflow` scope — the
default `Personal access token (classic)` with `repo` scope already has
it; the integration token used for the other PRs does not.

## Validation

Once installed, the workflow runs three jobs:

- `build-and-test`: assembleDebug + JVM tests + lint, on every push/PR (~5 min).
- `schema-guard`: regenerates Room schemas and fails on uncommitted diff. Catches the P0-A class of bug.
- `instrumented-tests`: API 35 emulator + connectedDebugAndroidTest. Runs on master pushes and on PRs labelled `ci:emulator` (~20 min, opt-in).

See `ci-workflow.yml.template` itself for the full annotated YAML.
