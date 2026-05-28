# `docs/` — operational guides

Single entry point for every doc the team has shipped. Each guide is small enough to read end-to-end in a single sitting.

## Onboarding

- **[`dev-setup.md`](dev-setup.md)** — prerequisites, `local.properties` keys, how to pin a JDK without breaking other developers, common Gradle commands, troubleshooting. **Start here on a fresh machine.**
- **[`ci-workflow.md`](ci-workflow.md)** + **[`ci-workflow.yml.template`](ci-workflow.yml.template)** — install the GitHub Actions workflow that runs `assembleDebug`, JVM tests, lint, Room schema-export guard, and optional emulator tests.

## Operating

- **[`security-runbook.md`](security-runbook.md)** — what is and is not a secret; per-provider console restriction recipes (Gemini, HuggingFace, Cloudflare, Remove.bg, OpenRouter, Groq, Firebase); `KeyVault` migration template; rotation cadences; incident-response playbook.
- **[`database-migrations.md`](database-migrations.md)** — workflow for adding a new `@Database(version = N)` bump; the five gotchas that bit us on the v5→v6 migration; the "never use `fallbackToDestructiveMigration()`" policy.
- **[`android-15-checklist.md`](android-15-checklist.md)** — what's already compliant after `targetSdk = 35` (PR #10); what still needs on-device QA (partial photo access, 16 KB page sizes, splash theme).

## Project state

- **[`follow-ups.md`](follow-ups.md)** — ordered checklist of every action that must happen after the 12 PRs in the current stack start merging. Update as the queue evolves; do not bury follow-ups in PR descriptions.
- **[`archive/`](archive/)** — historical project log + any other deprecated documentation. Read-only.

## Conventions

- Each new operational doc lives in `docs/` and gets a link here in the same PR.
- Skill markdown (under `skills/`) is for AI-orchestration prompts, not human onboarding. If you find yourself documenting something for a human that is also useful for an AI sub-agent, write the human version in `docs/` first and reference it from the skill.
- Reference issues by their `MT-###` id (and link to the closing PR once merged). The id naming convention started in `PROJECT_SCAN_2026-05-22.md`.
