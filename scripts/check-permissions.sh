#!/usr/bin/env bash
# Fails if the app asks for any Android permission (SPEC: zero permissions, no network).
# Checks the source manifest and, if a release APK has been built, the merged manifest inside it,
# which is where a library or a build change would sneak one in. A tool failure is an error, never a pass.
# Usage: scripts/check-permissions.sh [manifest]   (default: app/src/main/AndroidManifest.xml)
# Env:   APK   the APK to inspect (default: the unsigned release build)
#        AAPT2 the aapt2 binary (default: the newest one in the Android SDK); both exist so tests can fake them
set -euo pipefail

cd "$(dirname "$0")/.."

MANIFEST=${1:-app/src/main/AndroidManifest.xml}
APK=${APK:-app/build/outputs/apk/release/app-release-unsigned.apk}
status=0

if [ ! -f "$MANIFEST" ]; then
    echo "ERROR: $MANIFEST not found" >&2
    exit 2
fi

if grep -nE '<uses-permission' "$MANIFEST"; then
    echo "FAIL: $MANIFEST declares a permission" >&2
    status=1
else
    echo "source manifest: no permissions: ok"
fi

if [ -f "$APK" ]; then
    if [ -z "${AAPT2:-}" ]; then
        sdk=${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' local.properties 2>/dev/null || true)}
        if [ -z "$sdk" ] || [ ! -d "$sdk/build-tools" ]; then
            echo "ERROR: Android SDK not found (set ANDROID_HOME or sdk.dir in local.properties)" >&2
            exit 2
        fi
        AAPT2=$(ls -d "$sdk"/build-tools/*/ | sort -V | tail -1)aapt2
    fi
    out=$("$AAPT2" dump permissions "$APK") || {
        echo "ERROR: aapt2 failed on $APK, so its permissions were not checked" >&2
        exit 2
    }
    if ! grep -q '^package:' <<<"$out"; then
        echo "ERROR: aapt2 gave output that does not look like a manifest dump, so nothing was checked" >&2
        exit 2
    fi
    if grep -E '^(uses-permission|permission)' <<<"$out"; then
        echo "FAIL: the release APK's merged manifest declares a permission" >&2
        status=1
    else
        echo "release APK: no permissions: ok"
    fi
else
    echo "note: no release APK built, merged-manifest check skipped"
fi

exit $status
