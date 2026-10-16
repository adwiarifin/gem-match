#!/usr/bin/env bash
# Self-test for sign-ci.sh: it must sign a good build and refuse, before or after signing, every build that does not
# match the release. Runs in about a second with fake Android build tools and a fake APK, so no key is involved.
set -uo pipefail

cd "$(dirname "$0")/.."
script=$PWD/scripts/sign-ci.sh

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
failures=0

sha=0123456789abcdef0123456789abcdef01234567
cert=aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899

# An APK that records revision $sha, plus fake build tools that behave like the real ones.
mkdir -p "$work/apk/META-INF" "$work/sdk/build-tools/1.0.0"
printf 'revision: "%s"\n' "$sha" >"$work/apk/META-INF/version-control-info.textproto"
(cd "$work/apk" && zip -q ../unsigned.apk META-INF/version-control-info.textproto)
apk_hash=$(shasum -a 256 "$work/unsigned.apk" | cut -d' ' -f1)
printf 'fake keystore\n' >"$work/release.jks"
: >"$work/empty.jks"

tool() { printf '#!/usr/bin/env bash\n%s\n' "$2" >"$work/sdk/build-tools/1.0.0/$1" && chmod +x "$work/sdk/build-tools/1.0.0/$1"; }
tool zipalign 'exit 0'
tool aapt2 'echo "package: name='"'"'x'"'"' versionCode='"'"'100'"'"' versionName='"'"'0.1.0'"'"'"'
tool apksigner 'echo "$@" >>"'"$work"'/apksigner.args"
case "$1" in
    sign) while [ $# -gt 0 ]; do [ "$1" = "--out" ] && cp "${@: -1}" "$2"; shift; done ;;
    verify) if [ "${2:-}" = "--print-certs" ]; then echo "Signer #1 certificate SHA-256 digest: '"$cert"'"; fi ;;
esac
exit 0'

run() { # run <version> [VAR=value ...]; the environment is a good release unless overridden
    local version=$1
    shift
    rm -rf "$work/out"
    env CI=true EXPECTED_SHA="$sha" EXPECTED_APK_SHA256="$apk_hash" EXPECTED_CERT_SHA256="$cert" \
        KEYSTORE_PATH="$work/release.jks" KEY_ALIAS=gemmatch KEYSTORE_PASSWORD=hunter2hunter2 \
        ANDROID_HOME="$work/sdk" UNSIGNED_APK="$work/unsigned.apk" "$@" "$script" "$version" "$work/out"
}

expect() { # expect <name> <exit> <text the error must contain, or empty> -- <command...>
    local name=$1 want=$2 text=$3
    shift 4
    local output got
    output=$("$@" 2>&1)
    got=$?
    if [ "$got" -ne "$want" ]; then
        echo "FAIL $name: wanted exit $want, got $got"
        failures=$((failures + 1))
    elif [ -n "$text" ] && ! grep -qF -- "$text" <<<"$output"; then
        echo "FAIL $name: output lacks '$text'"
        failures=$((failures + 1))
    else
        echo "ok   $name"
    fi
}

expect "a good build is signed" 0 "built from 0123456" -- run 0.1.0
ok_files=$(ls "$work/out" 2>/dev/null | tr '\n' ' ')
if [ "$ok_files" = "gemmatch-0.1.0.apk gemmatch-0.1.0.apk.sha256 " ]; then
    echo "ok   the signed APK and its hash are written"
else
    echo "FAIL the signed APK and its hash are written: got '$ok_files'"
    failures=$((failures + 1))
fi
if grep -qF hunter2hunter2 "$work/apksigner.args"; then
    echo "FAIL the password never appears in apksigner's arguments"
    failures=$((failures + 1))
else
    echo "ok   the password never appears in apksigner's arguments"
fi

expect "no arguments is a usage error" 2 "usage" -- env CI=true "$script"
expect "outside CI is refused" 2 "release workflow" -- run 0.1.0 CI=false
expect "a missing variable is refused" 2 "EXPECTED_CERT_SHA256 is not set" -- run 0.1.0 EXPECTED_CERT_SHA256=
expect "a version with a suffix is refused" 2 "plain x.y.z" -- run 0.1.0-rc1
expect "a version the APK does not carry is refused" 2 "versionName is not 0.2.0" -- run 0.2.0
expect "a different release commit is refused" 2 "records revision $sha" -- run 0.1.0 EXPECTED_SHA=deadbeef
expect "an APK changed since the build job is refused" 2 "unsigned APK hash" -- run 0.1.0 EXPECTED_APK_SHA256=00
expect "a missing APK is refused" 2 "not found" -- run 0.1.0 UNSIGNED_APK="$work/none.apk"
expect "an empty keystore is refused" 2 "missing or empty" -- run 0.1.0 KEYSTORE_PATH="$work/empty.jks"
expect "no build tools is refused" 2 "no build-tools" -- run 0.1.0 ANDROID_HOME="$work/nowhere"
expect "the wrong certificate is refused" 2 "expected 1111" -- run 0.1.0 EXPECTED_CERT_SHA256=1111
if [ -e "$work/out/gemmatch-0.1.0.apk" ]; then
    echo "FAIL the wrong certificate leaves no APK behind"
    failures=$((failures + 1))
else
    echo "ok   the wrong certificate leaves no APK behind"
fi

[ "$failures" -eq 0 ] && echo "sign-ci self-test: all passed"
exit "$failures"
