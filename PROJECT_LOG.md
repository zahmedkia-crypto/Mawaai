# PROJECT_LOG.md — ARCHIVED

The original 290 KB `PROJECT_LOG.md` has been moved to
[`docs/archive/PROJECT_LOG-2026-05.md`](docs/archive/PROJECT_LOG-2026-05.md).

## Why

The log accumulated state from earlier project shapes:

- **Pexels integration** — described as wired, but no `data/remote/pexels` package exists in source. Tracked as `PEXELS-001`.
- **Supabase cloud sync** — described as in-progress, but no Supabase / Ktor dependencies in `gradle/libs.versions.toml` and no Supabase package in source.
- **Romantic-side screens** — cards, photo-cards, music, wishes, countdown, quiz, story — described as implemented, but `NavGraph.kt` only routes splash, intro, onboarding, home, memories, add/detail memory, letters, compose/detail letter, mood, settings, design.

Per `PROJECT_SCAN_CONTINUATION_2026-05-22.md` § "Reconciliation Note (MT-003)",
these are **formally backlogged, not regressed**. Future agents should not
"restore" them speculatively — treat each as its own EPIC if reintroduced.

## Where to look instead

- **Current open work** → the open pull requests on `master`.
- **Current findings + fix plan** → `PROJECT_SCAN_2026-05-22.md`,
  `PROJECT_SCAN_CONTINUATION_2026-05-22.md`, `API_HEALTH_2026-05-22.md`.
- **Operational guides** → `docs/security-runbook.md`,
  `docs/database-migrations.md`, `docs/dev-setup.md`,
  `docs/android-15-checklist.md`.
- **Original 290 KB history** → `docs/archive/PROJECT_LOG-2026-05.md`.
  Treat as historical reference only.

Closes MT-003.
