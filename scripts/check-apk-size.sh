#!/usr/bin/env bash
# Fails if an APK is over its size cap (see CONSTRAINTS.md, "APK size").
# Usage: scripts/check-apk-size.sh [release|debug|all]   (default: all)
set -euo pipefail

cd "$(dirname "$0")/.."

RELEASE_CAP=1048576   # 1 MiB: measured 53,686 unsigned on 2026-10-15, rounded up for headroom
DEBUG_CAP=2097152     # 2 MiB: measured 1,053,372 on 2026-10-15, rounded up for headroom
RELEASE_APK=app/build/outputs/apk/release/app-release-unsigned.apk
DEBUG_APK=app/build/outputs/apk/debug/app-debug.apk

status=0

check() {
    local name=$1 apk=$2 cap=$3
    if [ ! -f "$apk" ]; then
        echo "ERROR: $apk not found, build it first" >&2
        status=2
        return
    fi
    local size
    size=$(wc -c <"$apk" | tr -d ' ')
    if [ "$size" -gt "$cap" ]; then
        echo "FAIL: $name APK is $size bytes, cap is $cap" >&2
        [ "$status" -eq 0 ] && status=1
    else
        echo "$name APK: $size bytes (cap $cap): ok"
    fi
}

case "${1:-all}" in
    release) check release "$RELEASE_APK" "$RELEASE_CAP" ;;
    debug) check debug "$DEBUG_APK" "$DEBUG_CAP" ;;
    all)
        check release "$RELEASE_APK" "$RELEASE_CAP"
        check debug "$DEBUG_APK" "$DEBUG_CAP"
        ;;
    *)
        echo "usage: $0 [release|debug|all]" >&2
        exit 2
        ;;
esac
exit "$status"
