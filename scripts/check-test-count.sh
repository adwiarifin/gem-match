#!/usr/bin/env bash
# Fails if fewer tests ran than scripts/test-count-floor.txt says. Reads the Gradle XML reports of the last test run, so
# run it after `./gradlew :core:test :app:testDebugUnitTest`. It fails closed: a missing report is an error, not a pass.
# Usage: scripts/check-test-count.sh [ROOT]   (default: the repository root)
set -uo pipefail

root=${1:-$(cd "$(dirname "$0")/.." && pwd)}
floor_file=$root/scripts/test-count-floor.txt
[ -f "$floor_file" ] || { echo "test count: $floor_file is missing" >&2; exit 1; }

reports() {
    case $1 in
        core) echo "$root/core/build/test-results/test" ;;
        app) echo "$root/app/build/test-results/testDebugUnitTest" ;;
        *) echo "" ;;
    esac
}

failures=0
while read -r module floor; do
    case $module in '' | '#'*) continue ;; esac
    dir=$(reports "$module")
    if [ -z "$dir" ] || ! ls "$dir"/*.xml >/dev/null 2>&1; then
        echo "test count: no $module test reports in ${dir:-?}, run the tests first" >&2
        failures=$((failures + 1))
        continue
    fi
    ran=0
    for report in "$dir"/*.xml; do
        n=$(head -c 3000 "$report" | grep -o 'tests="[0-9]*"' | head -1 | tr -dc '0-9')
        ran=$((ran + ${n:-0}))
    done
    if [ "$ran" -lt "$floor" ]; then
        echo "test count: $module ran $ran tests, the floor is $floor" >&2
        failures=$((failures + 1))
    else
        echo "test count: $module ran $ran tests (floor $floor): ok"
    fi
done <"$floor_file"

[ "$failures" -eq 0 ]
