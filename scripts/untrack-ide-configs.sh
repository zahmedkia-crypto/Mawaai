#!/usr/bin/env bash
# scripts/untrack-ide-configs.sh
#
# MT-020 part 2. Untracks editor / AI-tool configuration directories that PR #15
# added to .gitignore. The .gitignore change only blocks NEW additions; this
# script removes already-tracked copies from the git index without deleting
# anyone's local files.
#
# Run from the repo root after PR #15 has merged.
#
# Usage:
#   scripts/untrack-ide-configs.sh         # dry-run by default
#   scripts/untrack-ide-configs.sh --apply # actually run `git rm --cached`

set -euo pipefail

DRY_RUN=true
if [ "${1:-}" = "--apply" ]; then
    DRY_RUN=false
fi

# Sanity check — must be at repo root.
if [ ! -f .gitignore ] || [ ! -d .git ]; then
    echo "error: run from repo root (the .git directory must be present)" >&2
    exit 1
fi

# Targets — these match the .gitignore additions made in PR #15.
TARGETS=(
    ".idea"
    ".vscode"
    ".cursor"
    ".cursorrules"
    ".aiassistant"
    ".devin"
)

# Allow-list — keep these inside .idea/ since the team may share them.
KEEP_INSIDE_IDEA=(
    ".idea/runConfigurations"
    ".idea/codeStyles"
)

tracked_targets=()
for t in "${TARGETS[@]}"; do
    if [ -e "$t" ] && git ls-files --error-unmatch -- "$t" >/dev/null 2>&1; then
        tracked_targets+=("$t")
    elif [ -e "$t" ]; then
        # Directory exists but is fully untracked already.
        :
    fi
    # Also catch the case where individual children are tracked even if the
    # parent dir's `git ls-files --error-unmatch` returns 0.
    if [ -d "$t" ]; then
        if git ls-files --error-unmatch -- "$t/*" >/dev/null 2>&1; then
            tracked_targets+=("$t")
        fi
    fi
done

# De-duplicate.
mapfile -t tracked_targets < <(printf "%s\n" "${tracked_targets[@]}" | sort -u)

if [ "${#tracked_targets[@]}" -eq 0 ]; then
    echo "Nothing to do. None of the MT-020 targets are tracked."
    exit 0
fi

echo "Tracked IDE / AI-tool paths that will be untracked:"
for t in "${tracked_targets[@]}"; do
    echo "  $t"
done
echo ""
echo "Allow-listed (kept tracked because they belong to the team, not one developer):"
for t in "${KEEP_INSIDE_IDEA[@]}"; do
    if [ -e "$t" ]; then
        echo "  $t"
    fi
done

if [ "$DRY_RUN" = "true" ]; then
    echo ""
    echo "Dry run only. Re-run with --apply to execute."
    exit 0
fi

echo ""
echo "Applying..."

# Untrack the targets but exclude the allow-listed paths.
for t in "${tracked_targets[@]}"; do
    if [ "$t" = ".idea" ]; then
        # Untrack everything inside .idea/ except the allow-list.
        git ls-files -- ".idea" | while read -r f; do
            keep=false
            for k in "${KEEP_INSIDE_IDEA[@]}"; do
                case "$f" in
                    "$k"/*) keep=true; break ;;
                esac
            done
            if [ "$keep" = "false" ]; then
                git rm --cached --quiet -- "$f"
            fi
        done
    else
        git rm -r --cached --quiet -- "$t"
    fi
done

git status --porcelain | sed -n '1,20p'
echo ""
echo "Done. Review with \`git status\`, then commit:"
echo "  git commit -m 'chore: untrack IDE/AI config dirs (MT-020 part 2)'"
echo "  git push"
