#!/usr/bin/env bash
# Builds the unsigned release APK and signs it with the owner's release key.
# Run it from your own terminal, never from an agent session: apksigner prompts for the
# key passwords, so they are never written to disk, shell history or a transcript.
#
# Usage: scripts/sign-release.sh [--no-build]
# Env:   GEMMATCH_KEYSTORE  keystore path (default ~/Private/keys/gemmatch/gemmatch-release.jks)
#        GEMMATCH_KEY_ALIAS key alias     (default gemmatch)
set -euo pipefail

cd "$(dirname "$0")/.."

KEYSTORE=${GEMMATCH_KEYSTORE:-$HOME/Private/keys/gemmatch/gemmatch-release.jks}
KEY_ALIAS=${GEMMATCH_KEY_ALIAS:-gemmatch}
UNSIGNED=app/build/outputs/apk/release/app-release-unsigned.apk
SIGNED=app/build/outputs/apk/release/gemmatch-release.apk

# The signed APK is published as the build of a revision, so it must be built from exactly that revision.
if [ -z "${GEMMATCH_ALLOW_DIRTY:-}" ] && [ -n "$(git status --porcelain)" ]; then
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

sdk=${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' local.properties)}
tools=$(ls -d "$sdk"/build-tools/*/ | sort -V | tail -1)
apksigner=${tools}apksigner
zipalign=${tools}zipalign

if [ "${1:-}" != "--no-build" ]; then
    ./gradlew :app:assembleRelease
fi

if [ ! -f "$UNSIGNED" ]; then
    echo "ERROR: $UNSIGNED not found, build it first" >&2
    exit 2
fi

# Gradle records the git revision it built from inside the APK; refuse an APK built from any other revision.
embedded=$(unzip -p "$UNSIGNED" META-INF/version-control-info.textproto 2>/dev/null | sed -n 's/.*revision: "\([0-9a-f]*\)".*/\1/p')
head=$(git rev-parse HEAD)
if [ -z "${GEMMATCH_ALLOW_DIRTY:-}" ] && [ "$embedded" != "$head" ]; then
    echo "ERROR: the unsigned APK was built from ${embedded:-an unknown revision}, but HEAD is $head" >&2
    echo "       run without --no-build to rebuild it" >&2
    exit 2
fi

"$zipalign" -c -P 16 4 "$UNSIGNED"

"$apksigner" sign --ks "$KEYSTORE" --ks-key-alias "$KEY_ALIAS" --out "$SIGNED" "$UNSIGNED"
"$apksigner" verify --verbose "$SIGNED"
echo "signed: $SIGNED ($(wc -c <"$SIGNED" | tr -d ' ') bytes), built from $(git rev-parse --short HEAD)"
"$apksigner" verify --print-certs "$SIGNED" | grep "certificate SHA-256 digest" | head -1
