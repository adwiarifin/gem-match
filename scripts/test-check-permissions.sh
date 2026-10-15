#!/usr/bin/env bash
# Self-test for check-permissions.sh: it must pass a clean app and fail, loudly, on every way a permission
# (or a broken tool) could otherwise slip through. Runs in about a second, with a fake aapt2 and fixture manifests.
set -uo pipefail

cd "$(dirname "$0")/.."

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
failures=0

printf '<manifest xmlns:android="http://schemas.android.com/apk/res/android"/>\n' >"$work/clean.xml"
printf '<manifest xmlns:android="http://schemas.android.com/apk/res/android">\n<uses-permission android:name="android.permission.INTERNET"/>\n</manifest>\n' >"$work/bad.xml"
: >"$work/fake.apk"

fake_aapt2() { printf '#!/usr/bin/env bash\n%s\n' "$1" >"$work/aapt2" && chmod +x "$work/aapt2"; }

expect() {
    local name=$1 want=$2
    shift 2
    "$@" >/dev/null 2>&1
    local got=$?
    if [ "$got" -eq "$want" ]; then
        echo "ok   $name"
    else
        echo "FAIL $name: wanted exit $want, got $got"
        failures=$((failures + 1))
    fi
}

fake_aapt2 'echo "package: name=\"x\""'
expect "clean manifest, clean APK passes" 0 env APK="$work/fake.apk" AAPT2="$work/aapt2" scripts/check-permissions.sh "$work/clean.xml"
expect "a permission in the source manifest fails" 1 env APK="$work/fake.apk" AAPT2="$work/aapt2" scripts/check-permissions.sh "$work/bad.xml"
expect "a missing manifest is an error" 2 env APK="$work/fake.apk" AAPT2="$work/aapt2" scripts/check-permissions.sh "$work/nope.xml"

fake_aapt2 'echo "package: name=\"x\""; echo "uses-permission: name='"'"'android.permission.INTERNET'"'"'"'
expect "a permission in the APK fails" 1 env APK="$work/fake.apk" AAPT2="$work/aapt2" scripts/check-permissions.sh "$work/clean.xml"

fake_aapt2 'exit 1'
expect "aapt2 failing is an error, never a pass" 2 env APK="$work/fake.apk" AAPT2="$work/aapt2" scripts/check-permissions.sh "$work/clean.xml"

fake_aapt2 'echo "something unexpected"'
expect "unrecognised aapt2 output is an error, never a pass" 2 env APK="$work/fake.apk" AAPT2="$work/aapt2" scripts/check-permissions.sh "$work/clean.xml"

expect "no APK built skips only the APK half" 0 env APK="$work/none.apk" scripts/check-permissions.sh "$work/clean.xml"

[ "$failures" -eq 0 ] && echo "check-permissions self-test: all passed"
exit "$failures"
