# ADR-0003 — Explicit Room migrations, no destructive fallback

- Status: Accepted
- Date: 2026-05-28
- Tags: database, reliability
- PR: #7

## Context

The original v5→v6 migration shipped with two bugs:

1. **`6.json` schema export was not committed.** No test exercised the
   migration; the bug below shipped undetected.
2. **Index names used `idx_*` prefix.** Room generates `index_<table>_<columns>`
   from un-named `@Index(...)`. `RoomOpenHelper.validateMigration` throws
   `IllegalStateException` on first launch with a v5 database. Every
   existing user crashes on update.

`fallbackToDestructiveMigration` was in use at one point. That wipes user
data on schema mismatch — catastrophic in production.

## Decision

Three policies, codified in PR #7 and `docs/database-migrations.md`:

1. **No `fallbackToDestructiveMigration` in production.** Missing
   migration crashes loudly in `MigrationTest`, not silently in front of
   the user.
2. **Schema files are checked in.** Every `@Database(version = N)` ships
   with `app/schemas/<...>/<N>.json`. CI `schema-guard` (PR #13)
   enforces this.
3. **Migrations match Room's emitted DDL byte-for-byte.** Backtick-
   quoted identifiers, table-level PK, explicit FK actions,
   `index_<table>_<col>` naming. Five gotchas documented.

Regression test `MigrationTest` in `androidTest/` exercises every
(n → n+1) path via `MigrationTestHelper.runMigrationsAndValidate`.

## Consequences

- ✅ Existing users keep their data across updates.
- ✅ Schema drift cannot ship.
- ✅ The five-gotcha list informs the next migration author.
- ⚠️ Adding a migration is a four-step ritual (bump, write, build,
  commit). Price of correctness.
- ⚠️ Full migration validation requires the opt-in `instrumented-tests`
  CI job (~20 min). `schema-guard` catches the most common failure mode
  in the cheap path.
- ❌ Does not address Room's lack of zero-downtime online migrations.
  For Mawaai's data sizes, this is acceptable.
