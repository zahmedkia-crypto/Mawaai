#!/usr/bin/env bash
# scripts/api-health.sh
#
# Smoke-tests every external API the app talks to using the keys in
# `local.properties`. Designed for a pre-release go/no-go gate.
#
# Closes the "no automated smoke test for Gemini, HuggingFace, Cloudflare,
# Remove.bg, Aladhan, ZenQuotes" gap from API_HEALTH_2026-05-22.md.
#
# This is a LOCAL script — keys are read from local.properties and never
# leave the host. Output never includes the key value, only the provider
# and the HTTP status. Suitable to run before tagging a release; not
# suitable for CI (would burn quota on every push).
#
# Usage:
#   scripts/api-health.sh                    # check every provider
#   scripts/api-health.sh --only gemini      # check only the named provider
#   scripts/api-health.sh --json             # machine-readable output
#
# Exit codes:
#   0  every configured provider returned a 2xx
#   1  at least one configured provider failed
#   2  usage error

set -euo pipefail

LOCAL_PROPS=local.properties
ONLY=""
JSON=false

while [ $# -gt 0 ]; do
    case "$1" in
        --only)  ONLY="$2"; shift 2 ;;
        --json)  JSON=true; shift ;;
        -h|--help)
            sed -n '3,18p' "$0"; exit 0 ;;
        *) echo "unknown arg: $1" >&2; exit 2 ;;
    esac
done

if [ ! -f "$LOCAL_PROPS" ]; then
    echo "error: $LOCAL_PROPS not found (run from repo root)" >&2
    exit 2
fi

# Pull a property from local.properties without sourcing the file.
get_prop() {
    local k="$1"
    awk -F= -v k="$k" '$1 == k { sub(/^[^=]*=/, ""); print; exit }' "$LOCAL_PROPS"
}

results=()  # "name|configured|http|note"

# Helper: probe an HTTP endpoint and record the status.
probe() {
    local name="$1" url="$2" auth="$3" body="${4:-}" method="${5:-GET}" content_type="${6:-}"
    local code
    local curl_args=(-sS -o /dev/null -w "%{http_code}" -m 20 -X "$method" -H "$auth")
    if [ -n "$content_type" ]; then curl_args+=(-H "Content-Type: $content_type"); fi
    if [ -n "$body" ]; then curl_args+=(--data-raw "$body"); fi
    code="$(curl "${curl_args[@]}" "$url" 2>/dev/null || echo "ERR")"
    case "$code" in
        2*) results+=("$name|true|$code|ok") ;;
        4*) results+=("$name|true|$code|auth/quota — inspect dashboard") ;;
        5*) results+=("$name|true|$code|provider down") ;;
        ERR) results+=("$name|true|—|network or timeout") ;;
        *)  results+=("$name|true|$code|unexpected") ;;
    esac
}

# Skip if the named provider was excluded.
included() {
    if [ -z "$ONLY" ]; then return 0; fi
    [ "$1" = "$ONLY" ]
}

# ─── Probes ──────────────────────────────────────────────────────────────

if included gemini; then
    key="$(get_prop GEMINI_API_KEY)"
    if [ -z "$key" ]; then
        results+=("gemini|false|—|no key in local.properties")
    else
        probe gemini \
            "https://generativelanguage.googleapis.com/v1beta/models?key=$key" \
            "X-Goog-Api-Key: $key" \
            "" GET
    fi
fi

if included huggingface; then
    key="$(get_prop HUGGINGFACE_API_KEY)"
    if [ -z "$key" ]; then
        results+=("huggingface|false|—|no key in local.properties")
    else
        probe huggingface \
            "https://huggingface.co/api/whoami-v2" \
            "Authorization: Bearer $key" \
            "" GET
    fi
fi

if included cloudflare; then
    account="$(get_prop CLOUDFLARE_ACCOUNT_ID)"
    token="$(get_prop CLOUDFLARE_API_TOKEN)"
    if [ -z "$account" ] || [ -z "$token" ]; then
        results+=("cloudflare|false|—|missing CLOUDFLARE_ACCOUNT_ID or CLOUDFLARE_API_TOKEN")
    else
        probe cloudflare \
            "https://api.cloudflare.com/client/v4/accounts/$account/ai/run/@cf/meta/llama-3.1-8b-instruct" \
            "Authorization: Bearer $token" \
            '{"messages":[{"role":"user","content":"ping"}],"max_tokens":4}' \
            POST application/json
    fi
fi

if included removebg; then
    key="$(get_prop REMOVE_BG_API_KEY)"
    if [ -z "$key" ]; then
        results+=("removebg|false|—|no key in local.properties")
    else
        probe removebg \
            "https://api.remove.bg/v1.0/account" \
            "X-Api-Key: $key" \
            "" GET
    fi
fi

if included openrouter; then
    key="$(get_prop OPENROUTER_API_KEY)"
    if [ -z "$key" ]; then
        results+=("openrouter|false|—|no key in local.properties")
    else
        probe openrouter \
            "https://openrouter.ai/api/v1/auth/key" \
            "Authorization: Bearer $key" \
            "" GET
    fi
fi

if included groq; then
    key="$(get_prop GROQ_API_KEY)"
    if [ -z "$key" ]; then
        results+=("groq|false|—|no key in local.properties")
    else
        probe groq \
            "https://api.groq.com/openai/v1/models" \
            "Authorization: Bearer $key" \
            "" GET
    fi
fi

if included aladhan; then
    # Aladhan does not require auth.
    probe aladhan \
        "https://api.aladhan.com/v1/timingsByCity/01-01-2026?city=Khartoum&country=Sudan" \
        "Accept: application/json" \
        "" GET
fi

if included zenquotes; then
    # ZenQuotes does not require auth.
    probe zenquotes \
        "https://zenquotes.io/api/random" \
        "Accept: application/json" \
        "" GET
fi

# ─── Output ──────────────────────────────────────────────────────────────

if [ "$JSON" = "true" ]; then
    echo "["
    n=${#results[@]}; i=0
    for r in "${results[@]}"; do
        IFS='|' read -r name configured http note <<< "$r"
        sep=","; [ $((++i)) -eq "$n" ] && sep=""
        printf '  {"provider":"%s","configured":%s,"http":"%s","note":"%s"}%s\n' \
            "$name" "$configured" "$http" "$note" "$sep"
    done
    echo "]"
else
    printf "%-14s  %-12s  %-8s  %s\n" "PROVIDER" "CONFIGURED" "HTTP" "NOTE"
    printf "%-14s  %-12s  %-8s  %s\n" "--------" "----------" "----" "----"
    for r in "${results[@]}"; do
        IFS='|' read -r name configured http note <<< "$r"
        printf "%-14s  %-12s  %-8s  %s\n" "$name" "$configured" "$http" "$note"
    done
fi

# Exit 1 if any configured provider didn't 2xx.
failure=false
for r in "${results[@]}"; do
    IFS='|' read -r _ configured http _ <<< "$r"
    if [ "$configured" = "true" ] && [[ ! "$http" =~ ^2 ]]; then
        failure=true
    fi
done
if [ "$failure" = "true" ]; then exit 1; fi
exit 0
