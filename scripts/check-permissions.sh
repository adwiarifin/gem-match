#!/usr/bin/env bash
# Fails if the app asks for any Android permission (SPEC: zero permissions, no network).
# Checks the source manifest and, if a release APK has been built, the merged manifest inside it,
# which is where a library or a build change would sneak one in.
# Usage: scripts/check-permissions.sh [manifest]   (default: app/src/main/AndroidManifest.xml)
set -euo pipefail

cd "$(dirname "$0")/.."

MANIFEST=${1:-app/src/main/AndroidManifest.xml}
APK=app/build/outputs/apk/release/app-release-unsigned.apk
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
    sdk=${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' local.properties)}
    aapt2=$(ls -d "$sdk"/build-tools/*/ | sort -V | tail -1)aapt2
    if "$aapt2" dump permissions "$APK" | grep -E '^(uses-permission|permission)'; then
        echo "FAIL: the release APK's merged manifest declares a permission" >&2
        status=1
    else
        echo "release APK: no permissions: ok"
    fi
else
    echo "note: no release APK built, merged-manifest check skipped"
fi

exit $status
