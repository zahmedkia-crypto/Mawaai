## What does this PR do?

<!-- 1-2 sentences. Link the MT-### or PEXELS-### / P0-### issue this
     closes. If this is a refactor with no associated ticket, say "no
     issue — refactor only" and justify the scope. -->

Closes: MT-XXX

## Behavioural changes for end users

<!-- Anything visible in the app? UX, performance, crashes? If purely
     internal (refactor, docs, build), say "none — internal only". -->

## Reviewer checklist

- [ ] If this touches `@Entity` definitions, the matching `app/schemas/<n>.json`
      is in this PR (or a follow-up is filed). The CI `schema-guard` job
      enforces this automatically but worth checking before merge.
- [ ] If this touches `AndroidManifest.xml`, `gradle.properties`, or any
      `build.gradle.kts`, the change is consistent with the release-readiness
      docs:
        - `docs/dev-setup.md`
        - `docs/android-15-checklist.md`
        - `docs/security-runbook.md`
- [ ] If this adds a new API provider, the secret is documented in
      `docs/security-runbook.md` § 2 and a probe is added to
      `scripts/api-health.sh`.
- [ ] If this is a release-critical change (DB migration, AndroidManifest,
      `targetSdk`, key handling), the corresponding section of
      `docs/release-checklist.md` is updated.

## Validation

```bash
./gradlew :app:assembleDebug
./gradlew test
./gradlew :app:lintDebug
```

<!-- For source code changes, also describe what manual testing you did. -->

## Follow-ups created

<!-- If this PR exposed work you deliberately did not do in this scope,
     list the new MT-### ids and a one-line description here. They go
     into `docs/follow-ups.md` separately. -->

- None
