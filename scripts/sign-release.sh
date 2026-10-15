#!/usr/bin/env bash
# Builds the unsigned release APK from the committed source and signs it with the owner's release key.
# Run it from your own terminal, never from an agent session: apksigner prompts for the
# key passwords, so they are never written to disk, shell history or a transcript.
#
# The signed APK is published as the build of a revision, so the script refuses to run with uncommitted
# changes and always rebuilds after that check. There is deliberately no way to sign an existing APK.
#
# Usage: scripts/sign-release.sh
# Env:   GEMMATCH_KEYSTORE   keystore path (default ~/Private/keys/gemmatch/gemmatch-release.jks)
#        GEMMATCH_KEY_ALIAS  key alias     (default gemmatch)
#        GEMMATCH_ALLOW_DIRTY=1  skip the clean-tree and revision checks, for a build you will NOT publish
set -euo pipefail

cd "$(dirname "$0")/.."

KEYSTORE=${GEMMATCH_KEYSTORE:-$HOME/Private/keys/gemmatch/gemmatch-release.jks}
KEY_ALIAS=${GEMMATCH_KEY_ALIAS:-gemmatch}
UNSIGNED=app/build/outputs/apk/release/app-release-unsigned.apk
SIGNED=app/build/outputs/apk/release/gemmatch-release.apk

if [ $# -gt 0 ]; then
    echo "ERROR: unexpected argument '$1'; this script takes none" >&2
    exit 2
fi

unverified=0
if [ "${GEMMATCH_ALLOW_DIRTY:-}" = "1" ]; then
    unverified=1
    echo "WARNING: GEMMATCH_ALLOW_DIRTY=1, so this build is UNVERIFIED. DO NOT PUBLISH IT." >&2
fi

# The signed APK is published as the build of a revision, so it must be built from exactly that revision.
if [ "$unverified" -eq 0 ] && [ -n "$(git status --porcelain)" ]; then
    echo "ERROR: uncommitted changes in the working tree; commit them first so the APK matches a revision" >&2
    echo "       (GEMMATCH_ALLOW_DIRTY=1 overrides, for a build that will not be published)" >&2
    exit 2
fi

if [ ! -t 0 ]; then
    echo "ERROR: needs an interactive terminal to prompt for the password" >&2
    exit 2
fi

if [ ! -f "$KEYSTORE" ]; then
    echo "ERROR: keystore not found: $KEYSTORE (set GEMMATCH_KEYSTORE)" >&2
    exit 2
fi

sdk=${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' local.properties 2>/dev/null || true)}
if [ -z "$sdk" ] || [ ! -d "$sdk/build-tools" ]; then
    echo "ERROR: Android SDK not found (set ANDROID_HOME or sdk.dir in local.properties)" >&2
    exit 2
fi
tools=$(ls -d "$sdk"/build-tools/*/ | sort -V | tail -1)
apksigner=${tools}apksigner
zipalign=${tools}zipalign

./gradlew :app:assembleRelease

if [ ! -f "$UNSIGNED" ]; then
    echo "ERROR: $UNSIGNED not found after the build" >&2
    exit 2
fi

# Gradle records the git revision it built from inside the APK; it must be the one we just checked is clean.
embedded=$(unzip -p "$UNSIGNED" META-INF/version-control-info.textproto 2>/dev/null | sed -n 's/.*revision: "\([0-9a-f]*\)".*/\1/p' || true)
head=$(git rev-parse HEAD)
if [ "$unverified" -eq 0 ] && [ "$embedded" != "$head" ]; then
    echo "ERROR: the APK records revision ${embedded:-'(none)'}, but HEAD is $head" >&2
    echo "       refusing to sign a build that cannot be tied to this commit" >&2
    exit 2
fi

"$zipalign" -c -P 16 4 "$UNSIGNED"

"$apksigner" sign --ks "$KEYSTORE" --ks-key-alias "$KEY_ALIAS" --out "$SIGNED" "$UNSIGNED"
"$apksigner" verify --verbose "$SIGNED"
echo "signed: $SIGNED ($(wc -c <"$SIGNED" | tr -d ' ') bytes), built from $(git rev-parse --short HEAD)"
"$apksigner" verify --print-certs "$SIGNED" | grep "certificate SHA-256 digest" | head -1
if [ "$unverified" -eq 1 ]; then
    echo "WARNING: UNVERIFIED BUILD. DO NOT PUBLISH IT." >&2
fi
