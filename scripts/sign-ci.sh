#!/usr/bin/env bash
# Signs the already built unsigned release APK inside the release workflow, where the key comes from the
# protected `release` GitHub Environment. Locally use scripts/sign-release.sh, which prompts for the password.
#
# Usage: scripts/sign-ci.sh <version> <out-dir>
# Env:   CI=true                 refuses to run anywhere else
#        EXPECTED_SHA            the commit the release was cut from; the APK must record exactly this revision
#        KEYSTORE_PATH           decoded keystore file
#        KEY_ALIAS               key alias
#        KEYSTORE_PASSWORD       store and key password (they are the same); apksigner reads it from the environment,
#                                never from arguments
set -euo pipefail

cd "$(dirname "$0")/.."

if [ $# -ne 2 ]; then
    echo "usage: scripts/sign-ci.sh <version> <out-dir>" >&2
    exit 2
fi
version=$1
out_dir=$2

if [ "${CI:-}" != "true" ]; then
    echo "ERROR: this script is for the release workflow; use scripts/sign-release.sh locally" >&2
    exit 2
fi
for name in EXPECTED_SHA KEYSTORE_PATH KEY_ALIAS KEYSTORE_PASSWORD; do
    if [ -z "${!name:-}" ]; then
        echo "ERROR: $name is not set" >&2
        exit 2
    fi
done
if ! [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "ERROR: '$version' is not a plain x.y.z version" >&2
    exit 2
fi

UNSIGNED=app/build/outputs/apk/release/app-release-unsigned.apk
SIGNED="$out_dir/gemmatch-$version.apk"

if [ ! -f "$UNSIGNED" ]; then
    echo "ERROR: $UNSIGNED not found; build the release APK first" >&2
    exit 2
fi

tools=$(ls -d "${ANDROID_HOME:?ANDROID_HOME is not set}"/build-tools/*/ | sort -V | tail -1)

# The release is the build of one commit: the APK must record it, and its version must match the release.
embedded=$(unzip -p "$UNSIGNED" META-INF/version-control-info.textproto 2>/dev/null | sed -n 's/.*revision: "\([0-9a-f]*\)".*/\1/p' || true)
if [ "$embedded" != "$EXPECTED_SHA" ]; then
    echo "ERROR: the APK records revision ${embedded:-'(none)'}, but the release is $EXPECTED_SHA" >&2
    exit 2
fi
badging=$("${tools}aapt2" dump badging "$UNSIGNED")
if ! grep -q "versionName='$version'" <<<"$badging"; then
    echo "ERROR: the APK's versionName is not $version" >&2
    exit 2
fi

mkdir -p "$out_dir"
"${tools}zipalign" -c -P 16 4 "$UNSIGNED"
"${tools}apksigner" sign \
    --ks "$KEYSTORE_PATH" --ks-key-alias "$KEY_ALIAS" \
    --ks-pass env:KEYSTORE_PASSWORD --key-pass env:KEYSTORE_PASSWORD --v4-signing-enabled false \
    --out "$SIGNED" "$UNSIGNED"
"${tools}apksigner" verify --verbose "$SIGNED"
(cd "$out_dir" && shasum -a 256 "gemmatch-$version.apk" >"gemmatch-$version.apk.sha256")
echo "signed: $SIGNED ($(wc -c <"$SIGNED" | tr -d ' ') bytes), built from ${EXPECTED_SHA:0:7}"
"${tools}apksigner" verify --print-certs "$SIGNED" | grep "certificate SHA-256 digest" | head -1
