# Architecture Decision Records

This directory captures the *why* of every architectural choice the project
has committed to. Each ADR is a 50-line markdown file with three sections:
**Context**, **Decision**, **Consequences**. We use the
[Michael Nygard format](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions).

ADRs are **immutable** once accepted. To overturn an earlier decision, write
a new ADR that supersedes it; do not edit the old file. The historical
record matters.

## Index

| # | Title | Status | Date |
|---|---|---|---|
| [0001](0001-key-vault-storage.md) | KeyVault storage via EncryptedSharedPreferences | Accepted | 2026-05-28 |
| [0002](0002-openrouter-fallback.md) | OpenRouter as transparent Gemini fallback | Accepted | 2026-05-28 |
| [0003](0003-room-migrations-policy.md) | Explicit Room migrations, no destructive fallback | Accepted | 2026-05-28 |
| [0004](0004-workmanager-keep-policy.md) | `ExistingPeriodicWorkPolicy.KEEP` for scheduled work | Accepted | 2026-05-28 |
| [0005](0005-version-pinning-strategy.md) | Conservative version pins behind a single catalog | Accepted | 2026-05-28 |

## Template

When writing a new ADR, copy this skeleton:

```markdown
# ADR-NNNN — <short title>

- Status: Proposed | Accepted | Superseded by [ADR-XXXX](XXXX-link.md)
- Date: YYYY-MM-DD
- Tags: <area>, <area>

## Context

<2-4 paragraphs. What problem are we solving? What forces are at play?
What did the project look like before this decision?>

## Decision

<1-2 paragraphs. What did we decide? Be specific — name the libraries,
versions, file paths, classes. A future reader should not have to dig
into git blame to understand the choice.>

## Consequences

<Bullet list. Both upsides and downsides. What does this decision make
easy, and what does it make hard? What follow-up work does it imply?>

- ✅ <good consequence>
- ⚠️ <trade-off>
- ❌ <bad consequence we accepted anyway>
```

Number the file using the next free integer in this index (zero-padded to
four digits). Add a row to the table above in the same PR.
