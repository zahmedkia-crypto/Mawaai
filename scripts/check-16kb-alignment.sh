#!/usr/bin/env bash
# scripts/check-16kb-alignment.sh
#
# MT-025 helper. Reports whether each native .so file inside the project's
# AAB or APK is page-aligned for 16 KB-page-size Android 15 devices.
#
# Usage:
#   ./gradlew :app:bundleRelease   # produces app/build/outputs/bundle/release/app-release.aab
#   scripts/check-16kb-alignment.sh app/build/outputs/bundle/release/app-release.aab
#
#   ./gradlew :app:assembleRelease # produces app/build/outputs/apk/release/app-release.apk
#   scripts/check-16kb-alignment.sh app/build/outputs/apk/release/app-release.apk
#
# Exit codes:
#   0  every .so file is 16 KB-aligned (good)
#   1  at least one .so is 4 KB-aligned only (action needed)
#   2  usage error / missing tooling

set -euo pipefail

if [ $# -ne 1 ]; then
    echo "usage: $0 <path-to-aab-or-apk>" >&2
    exit 2
fi

archive="$1"
if [ ! -f "$archive" ]; then
    echo "error: $archive does not exist" >&2
    exit 2
fi

# Locate readelf (Android NDK ships its own; fall back to the system one).
if command -v readelf >/dev/null 2>&1; then
    READELF=readelf
elif [ -n "${ANDROID_NDK_HOME:-}" ] && [ -x "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf" ]; then
    READELF="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
else
    echo "error: neither system readelf nor NDK llvm-readelf is available" >&2
    exit 2
fi

# Extract every .so from the archive (works for both AAB and APK since both are zips).
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT

echo "Extracting .so files from $archive ..."
unzip -q "$archive" '*.so' -d "$workdir" || true

fail_count=0
ok_count=0
echo ""
printf "%-50s  %-12s  %s\n" "FILE" "ALIGNMENT" "STATUS"
printf "%-50s  %-12s  %s\n" "----" "---------" "------"

while IFS= read -r so; do
    # Read the largest alignment of any LOAD program header. 0x4000 = 16 KB, 0x1000 = 4 KB.
    align_hex="$("$READELF" -lW "$so" \
        | awk '/^  LOAD/ { print $NF }' \
        | sort -u \
        | tail -n 1)"
    if [ -z "$align_hex" ]; then
        printf "%-50s  %-12s  %s\n" "$(basename "$so")" "?" "could not parse"
        fail_count=$((fail_count+1))
        continue
    fi
    align_dec=$((align_hex))
    rel="${so#$workdir/}"
    if [ "$align_dec" -ge 16384 ]; then
        printf "%-50s  %-12s  %s\n" "$rel" "$align_hex" "✓ 16 KB-aligned"
        ok_count=$((ok_count+1))
    else
        printf "%-50s  %-12s  %s\n" "$rel" "$align_hex" "✗ NOT 16 KB-aligned"
        fail_count=$((fail_count+1))
    fi
done < <(find "$workdir" -name '*.so' | sort)

echo ""
echo "OK:   $ok_count"
echo "FAIL: $fail_count"
if [ "$fail_count" -gt 0 ]; then
    echo ""
    echo "Action: each failing .so file blocks 16 KB-page Android 15 devices from loading"
    echo "        the app. Options:"
    echo "          - Bump the source AAR / dependency to a version published >= 2024-09"
    echo "            (when Google's 16 KB-page guidance landed). For OpenCV that means"
    echo "            >= 4.10."
    echo "          - If no compatible version exists, vendor the .so and re-link with"
    echo "            \`-Wl,-z,max-page-size=16384\` per https://developer.android.com/guide/practices/page-sizes"
    exit 1
fi
exit 0
