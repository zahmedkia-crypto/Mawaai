# ADR-0005 — Conservative version pins behind a single catalog

- Status: Accepted
- Date: 2026-05-28
- Tags: build, dependencies
- PRs: #6, #10, #12

## Context

Two questions arose during MT-015 (modernise versions) and MT-016
(`targetSdk = 35`):

1. **How aggressive should bumps be?** Latest Compose BOM `2025.04.01`
   ships Material 3 1.4 with stricter deprecations. A jump surfaces a
   wave of lint errors that obscure the bump itself.
2. **What about coupling?** `core-ktx 1.15.0` requires `compileSdk = 35`.
   Bumping it without first merging the SDK bump breaks the build.

## Decision

Three rules:

1. **Conservative bumps.** Most recent stable that does NOT require
   source-code migration. MT-015 picked Compose BOM `2024.10.00`
   (Material 3 1.3), not `2025.04.01`. The 1.4 bump is a separate
   follow-up after deprecation cleanup.
2. **`compileSdk`-coupled bumps follow the SDK PR.** `coreKtx 1.15.0`
   is deferred until both #10 and #12 are on master.
3. **Group related libs in Dependabot** (`.github/dependabot.yml`).
   Compose, Hilt, Room, lifecycle+nav, and networking each get a group
   so a minor bump produces ONE PR instead of fifteen.

The catalog at `gradle/libs.versions.toml` is the single source of
truth; no version string lives in `app/build.gradle.kts` directly.

## Consequences

- ✅ PRs reviewable on their own (version bump ≠ code migration).
- ✅ Dependabot does not flood the queue.
- ✅ `lint { disable += "StateFlowValueCalledInComposition" }` has a
  clear off-ramp.
- ⚠️ Catalog lags upstream by 3–6 months for some modules.
- ⚠️ Some libraries cannot be Dependabot-tracked cleanly (ML Kit beta,
  OpenCV fork). Dependabot config ignores these; manual review tracks.
- ❌ Holding back means we sometimes miss bug fixes in minor versions.
  Accepted; alternative is paying for every catalog-wide chase.
