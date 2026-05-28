#!/usr/bin/env bash
# scripts/audit-buildconfig-keys.sh
#
# MT-027 prep. Lists every direct `BuildConfig.<KEY>` read in app/src/ that
# should be migrated to `keyVault.get(ApiKeyId.<KEY>)` once PR #11 (the
# KeyVault seam) has merged. The migration template is in
# docs/security-runbook.md § 3.
#
# Usage:
#   scripts/audit-buildconfig-keys.sh

set -euo pipefail

if [ ! -d app/src ]; then
    echo "error: run from repo root" >&2
    exit 1
fi

KEYS=(
    GEMINI_API_KEY
    HUGGINGFACE_API_KEY
    CLOUDFLARE_ACCOUNT_ID
    CLOUDFLARE_API_TOKEN
    REMOVE_BG_API_KEY
    OPENROUTER_API_KEY
    GROQ_API_KEY
    PEXELS_API_KEY
)

echo "Direct BuildConfig.<key> reads in app/src — each is a MT-027 migration point."
echo ""

total=0
for k in "${KEYS[@]}"; do
    hits="$(grep -rnH "BuildConfig\\.${k}" app/src/ || true)"
    if [ -n "$hits" ]; then
        count="$(printf "%s\n" "$hits" | wc -l)"
        total=$((total + count))
        echo "─── BuildConfig.${k}  (${count} read$( [ "$count" -gt 1 ] && echo s))"
        printf "%s\n" "$hits" | sed 's/^/    /'
        echo ""
    fi
done

echo "Total reads to migrate: $total"
echo ""
echo "Migration template for each call site (from docs/security-runbook.md § 3):"
cat <<'EOF'

  Before:
      val key = BuildConfig.GEMINI_API_KEY
  After:
      // inject in the constructor: private val keyVault: KeyVault
      val key = keyVault.get(ApiKeyId.GEMINI)

EOF
echo "Once every reference is migrated, the BuildConfig fields can stay (they"
echo "remain the fallback inside KeyVault.get when no override is set). Removing"
echo "the BuildConfig fields entirely is a *separate* decision tracked as MT-029."
