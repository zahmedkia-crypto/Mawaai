# Contributing to Mawaai

Thanks for thinking about contributing. This document covers the **minimum
you need to know** to ship a working PR. Everything else lives in
`docs/` — see `docs/README.md` for the index.

## 1. Local setup

Read `docs/dev-setup.md`. Short version:

```bash
git clone <repo>
cd Mawaai
# Create local.properties with API keys (see docs/security-runbook.md § 1
# for which keys exist and how to obtain each).
./gradlew :app:assembleDebug
```

You need JDK 21. Gradle's toolchain auto-download (configured in
`gradle.properties` after MT-021) fetches it for you on a fresh machine.

## 2. Workflow

1. **One MT-### per PR.** If you accidentally bundle two unrelated changes,
   split them. Reviewers will ask.
2. **Branch naming**: `fix/MT-###-short-slug`, `feat/MT-###-short-slug`,
   `chore/short-slug`. Avoid `dev/` or `feature/` — they collide with
   conventional CI patterns.
3. **Commit messages** follow the
   [Conventional Commits](https://www.conventionalcommits.org/) spec. The
   prefix matters (`feat:`, `fix:`, `build:`, `refactor:`, `chore:`,
   `docs:`, `test:`, `ci:`) — Dependabot uses the prefix to set the right
   label on its auto-PRs.
4. **PR descriptions** follow the template in `.github/pull_request_template.md`.
   The reviewer checklist is the bare minimum; embellish as needed.
5. **Tests**: add a JVM unit test in `app/src/test/` when possible.
   Instrumented tests (`app/src/androidTest/`) cost emulator time on CI;
   only use them when an Android dependency forces it (Keystore, ML Kit,
   Compose UI).

## 3. Before you push

```bash
./gradlew :app:assembleDebug
./gradlew test
./gradlew :app:lintDebug
```

CI will rerun these (see `docs/ci-workflow.md`), but local feedback is
faster. If lint is noisy on existing code, that's tracked in MT-008 — feel
free to clean up adjacent warnings in your PR.

## 4. Reviewing someone else's PR

- Read the PR description first. If it doesn't follow the template,
  comment "please fill in the template" and stop.
- Look at the diff with a focus on **the failure modes the author didn't
  mention**. The author already thought about the success path.
- For DB / build / manifest changes, sanity-check against:
  - `docs/database-migrations.md`
  - `docs/android-15-checklist.md`
  - `docs/security-runbook.md`
- Approve when the PR is correct, useful, and reviewable. Don't gate on
  bikeshedding.

## 5. Security

**Do not** open public issues for security-sensitive defects. Read
`SECURITY.md` for the disclosure procedure.

## 6. Filing issues

- **Bug?** Use `.github/ISSUE_TEMPLATE/bug_report.md`. Include device + Android
  version, logcat (redacted), and reproduction steps.
- **Feature?** Use `.github/ISSUE_TEMPLATE/feature_request.md`. Frame the
  problem in user-facing terms; list acceptance criteria.

## 7. The MT-### registry

Every actionable issue is tagged `MT-###` (Micro-Task). Numbers are
monotonic across the whole project. When opening a new issue, pick the
next free number — `git grep "MT-" | sort -u` gives you the current
ceiling.

The original sequence started in `PROJECT_SCAN_2026-05-22.md`. P0-A / P0-B
are reserved for issues that block a release (used once, for the v5→v6
migration bug).

## 8. Operational doc index

When in doubt:

- **Onboarding** → `docs/dev-setup.md`
- **Build, CI** → `docs/ci-workflow.md`, `docs/follow-ups.md`
- **Security** → `docs/security-runbook.md`, `SECURITY.md`
- **Database** → `docs/database-migrations.md`
- **Platform compliance** → `docs/android-15-checklist.md`
- **Release** → `docs/release-checklist.md`
- **Architecture decisions** → `docs/adr/`

## 9. Anything else

Open a `[discussion]`-tagged issue or DM the maintainer.

Thanks for reading.
