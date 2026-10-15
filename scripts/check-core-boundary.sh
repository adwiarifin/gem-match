#!/usr/bin/env bash
# Fails if the core module depends on Android or the JVM class library.
# Keeps core KMP-ready (see CONSTRAINTS.md, "Core boundary").
set -euo pipefail

cd "$(dirname "$0")/.."

status=0

if grep -rnE '^\s*import\s+(android|java|javax)\.' core/src; then
    echo "FAIL: core must not import android.*, java.* or javax.*" >&2
    status=1
fi

if grep -nE 'com\.android|android\.' core/build.gradle.kts; then
    echo "FAIL: core must not apply an Android Gradle plugin" >&2
    status=1
fi

if [ "$status" -eq 0 ]; then
    echo "core boundary: ok"
fi
exit "$status"
