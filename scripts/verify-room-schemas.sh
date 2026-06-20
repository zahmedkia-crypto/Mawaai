#!/usr/bin/env bash
# scripts/verify-room-schemas.sh
#
# Post-merge helper for P0-A (PR #7). After the @Database version is bumped,
# the Room KSP processor emits a fresh `app/schemas/<...>/<version>.json` on
# the next clean build. That file must be committed; CI's schema-guard
# (PR #13) enforces this once the workflow is installed.
#
# This script runs the regen locally and reports whether the developer needs
# to `git add app/schemas/` and commit.
#
# Usage:
#   scripts/verify-room-schemas.sh

set -euo pipefail

if [ ! -d .git ] || [ ! -f settings.gradle.kts ]; then
    echo "error: run from repo root" >&2
    exit 1
fi

GRADLEW="./gradlew"
if [ ! -x "$GRADLEW" ]; then
    echo "error: ./gradlew is not executable" >&2
    exit 1
fi

echo "Regenerating Room schemas (kspDebugKotlin)…"
"$GRADLEW" :app:kspDebugKotlin --quiet

# Compare working tree against HEAD for the schemas dir only.
diff_output="$(git status --porcelain app/schemas/ || true)"
if [ -z "$diff_output" ]; then
    echo "✓ schemas are up to date with master."
    exit 0
fi

echo ""
echo "Schemas changed during regen. Files affected:"
echo "$diff_output"
echo ""
echo "Action:"
echo "  git add app/schemas/"
echo "  git commit -m 'db: commit auto-generated Room schema'"
echo "  git push"
exit 1
