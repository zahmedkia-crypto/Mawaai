# ADR-0004 — `ExistingPeriodicWorkPolicy.KEEP` for scheduled work

- Status: Accepted
- Date: 2026-05-28
- Tags: background-work, reliability
- PR: #14

## Context

`MawaaiApp` schedules `DailyQuoteWorker` (periodic, 1 day) and
`SeedDatabaseWorker` (one-time) on every cold start. KEEP vs REPLACE
determines what happens when the same unique work is enqueued again.

REPLACE on the daily quote would reset the "next run" timer on every
cold start. A user who opens the app at 14:00 daily would push the 09:00
quote to 14:00, then later, drifting indefinitely. REPLACE on the seed
worker could re-run the seeder over user edits.

## Decision

Both calls use **KEEP**. Work names are hoisted to companion-object
constants (`DAILY_QUOTE_WORK_NAME`, `SEED_DATABASE_WORK_NAME`) so the
same name is used for any future cancel/replace. Inline comments in
`MawaaiApp.kt` explain why REPLACE would be wrong.

## Consequences

- ✅ 9 AM quote cadence stable across cold starts.
- ✅ Seed data not re-applied over user edits.
- ⚠️ A future Settings change to the quote time must explicitly cancel
  the existing work before enqueueing with the new schedule.
- ❌ User who never foregrounds the app never schedules the daily
  quote (MT-018 defers enqueue to the first foregrounding). Accepted.
