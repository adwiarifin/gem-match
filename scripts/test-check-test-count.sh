#!/usr/bin/env bash
# Self-test for check-test-count.sh with fake Gradle reports: meeting the floor passes, falling below it fails, and a
# missing report fails closed.
set -uo pipefail

cd "$(dirname "$0")/.."
script=$PWD/scripts/check-test-count.sh

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
failures=0

setup() {
    rm -rf "$work/root"
    mkdir -p "$work/root/scripts" "$work/root/core/build/test-results/test" "$work/root/app/build/test-results/testDebugUnitTest"
    printf '# floor\ncore 5\napp 3\n' >"$work/root/scripts/test-count-floor.txt"
    printf '<testsuite name="A" tests="3" skipped="0" failures="0">\n' >"$work/root/core/build/test-results/test/A.xml"
    printf '<testsuite name="B" tests="2" skipped="0" failures="0">\n' >"$work/root/core/build/test-results/test/B.xml"
    printf '<testsuite name="C" tests="3" skipped="0" failures="0">\n' >"$work/root/app/build/test-results/testDebugUnitTest/C.xml"
}

expect() {
    local name=$1 want=$2
    "$script" "$work/root" >/dev/null 2>&1
    local got=$?
    if [ "$got" -eq "$want" ]; then
        echo "ok   $name"
    else
        echo "FAIL $name (exit $got, wanted $want)"
        failures=$((failures + 1))
    fi
}

setup && expect "meeting the floor passes" 0
setup && printf '<testsuite name="B" tests="1" skipped="0" failures="0">\n' >"$work/root/core/build/test-results/test/B.xml" && expect "one test fewer in core fails" 1
setup && rm "$work/root/app/build/test-results/testDebugUnitTest/C.xml" && expect "a missing app report fails closed" 1
setup && printf '<testsuite name="B" tests="9" skipped="0" failures="0">\n' >"$work/root/core/build/test-results/test/B.xml" && expect "more tests than the floor passes" 0
setup && rm "$work/root/scripts/test-count-floor.txt" && expect "a missing floor file fails" 1

if [ "$failures" -gt 0 ]; then
    echo "check-test-count self-test: $failures failed"
    exit 1
fi
echo "check-test-count self-test: all passed"
