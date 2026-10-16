#!/usr/bin/env bash
# Signs the already built unsigned release APK inside the release workflow, where the key comes from the
# protected `release` GitHub Environment. Locally use scripts/sign-release.sh, which prompts for the password.
#
# Usage: scripts/sign-ci.sh <version> <out-dir>
# Env:   CI=true                 refuses to run anywhere else
#        EXPECTED_SHA            the commit the release was cut from; the APK must record exactly this revision
#        KEYSTORE_PATH           decoded keystore file
#        KEY_ALIAS               key alias
#        EXPECTED_APK_SHA256     SHA-256 of the unsigned APK the build job produced; it must not have changed since
#        EXPECTED_CERT_SHA256    SHA-256 of the signing certificate (public); a wrong keystore must not publish an APK
#                                that installed copies reject
#        UNSIGNED_APK            the unsigned APK (default app/build/outputs/apk/release/app-release-unsigned.apk)
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
for name in EXPECTED_SHA EXPECTED_APK_SHA256 EXPECTED_CERT_SHA256 KEYSTORE_PATH KEY_ALIAS KEYSTORE_PASSWORD; do
    if [ -z "${!name:-}" ]; then
        echo "ERROR: $name is not set" >&2
        exit 2
    fi
done
if ! [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "ERROR: '$version' is not a plain x.y.z version" >&2
    exit 2
fi

UNSIGNED=${UNSIGNED_APK:-app/build/outputs/apk/release/app-release-unsigned.apk}
SIGNED="$out_dir/gemmatch-$version.apk"

if [ ! -f "$UNSIGNED" ]; then
    echo "ERROR: $UNSIGNED not found; build the release APK first" >&2
    exit 2
fi

if [ ! -f "$KEYSTORE_PATH" ] || [ ! -s "$KEYSTORE_PATH" ]; then
    echo "ERROR: keystore $KEYSTORE_PATH is missing or empty (is the KEYSTORE_BASE64 secret set?)" >&2
    exit 2
fi

tools=$(ls -d "${ANDROID_HOME:?ANDROID_HOME is not set}"/build-tools/*/ 2>/dev/null | sort -V | tail -1 || true)
if [ -z "$tools" ]; then
    echo "ERROR: no build-tools under $ANDROID_HOME" >&2
    exit 2
fi

# The APK being signed is the one the build job checked and hashed, not something swapped in between.
actual_apk=$(shasum -a 256 "$UNSIGNED" | cut -d' ' -f1)
if [ "$actual_apk" != "$EXPECTED_APK_SHA256" ]; then
    echo "ERROR: the unsigned APK hash is $actual_apk, but the build job produced $EXPECTED_APK_SHA256" >&2
    exit 2
fi

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
certs=$("${tools}apksigner" verify --print-certs "$SIGNED")
# The line prefix varies by apksigner version ("Signer #1 ...", "V3.0 Signer: ..."), so match only the digest.
cert=$(sed -n 's/^.*certificate SHA-256 digest: //p' <<<"$certs" | head -1)
echo "certificate SHA-256: ${cert:-'(none)'}"
if [ "$cert" != "$EXPECTED_CERT_SHA256" ]; then
    rm -f "$SIGNED" "$SIGNED.sha256"
    echo "$certs" >&2
    echo "ERROR: signed with certificate ${cert:-'(none)'}, expected $EXPECTED_CERT_SHA256; nothing was kept" >&2
    exit 2
fi
