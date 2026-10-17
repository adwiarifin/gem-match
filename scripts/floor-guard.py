#!/usr/bin/env python3
"""Diff-scoped enforcement of the CONSTRAINTS.md floor.

Looks at added and removed lines between a base ref and the working tree (staged,
unstaged and untracked files), and fails on moves that lower the bar: new suppression
comments, unfinished stubs, skipped or deleted tests, removed assertions, and weakened
rules or new exceptions in CONSTRAINTS.md. Tightening a rule is silent.

Usage: scripts/floor-guard.py [--base REF]    (default base: HEAD)
Exit:  0 clean, 1 floor violation, 2 the guard could not run.
"""

import os
import re
import subprocess
import sys

CODE_FILES = re.compile(r"\.(kt|kts|java|xml|gradle|sh|py)$")
TEST_FILES = re.compile(r"(^|/)src/[^/]*[tT]est/|Test\.kt$")

# Patterns are written with a bracketed character so this file never matches itself.
SUPPRESSIONS = re.compile(
    r"@[S]uppress|@file:[S]uppress|@[S]uppressLint|ktlint-[d]isable|detekt:[s]uppress|lint-[b]aseline"
)
STUBS = re.compile(r"\b[T]ODO\(|NotImplemented[E]rror|catch\s*\([^)]*\)\s*\{\s*\}")
SKIPS = re.compile(r"@[I]gnore|@[D]isabled")
ASSERTIONS = re.compile(r"\b(assert\w*|fail)\(")

EXCEPTION_ROW = re.compile(r"^\|\s*[WE]\d+\s*\|")
MIN_WORDS = re.compile(r"(≥|>=|at least|must not fall|or more)", re.I)
MAX_WORDS = re.compile(r"(≤|<=|at most|must not grow|or less|under)", re.I)
NUMBER = re.compile(r"\d[\d,]*(?:\.\d+)?")
NOISE = re.compile(r"\d{4}-\d{2}-\d{2}|\(\s*\d+\s*[KMG]i?B\s*\)")


def git(*args):
    result = subprocess.run(["git", *args], capture_output=True, text=True)
    if result.returncode != 0:
        print(f"floor-guard: git {' '.join(args)} failed: {result.stderr.strip()}", file=sys.stderr)
        sys.exit(2)
    return result.stdout


def parse_diff(diff):
    """Return (added, removed, deleted_files); added/removed are (file, text) pairs."""
    added, removed, deleted = [], [], []
    file = old_file = ""
    in_header = False
    for line in diff.split("\n"):
        if line.startswith("diff "):
            in_header = True
        elif line.startswith("@@"):
            in_header = False
        elif in_header:
            if line.startswith("--- "):
                old_file = re.sub(r"^[ab]/", "", line[4:])
            elif line.startswith("+++ "):
                new_file = re.sub(r"^[ab]/", "", line[4:])
                file = old_file if new_file == "/dev/null" else new_file
                if new_file == "/dev/null":
                    deleted.append(file)
        elif line.startswith("+"):
            added.append((file, line[1:]))
        elif line.startswith("-"):
            removed.append((file, line[1:]))
    return added, removed, deleted


def untracked_diff():
    names = git("ls-files", "--others", "--exclude-standard").split("\n")
    parts = []
    for name in filter(None, names):
        result = subprocess.run(
            ["git", "diff", "--no-index", "--unified=0", "/dev/null", name],
            capture_output=True,
            text=True,
        )
        if result.returncode not in (0, 1):
            print(f"floor-guard: could not diff {name}", file=sys.stderr)
            sys.exit(2)
        parts.append(result.stdout)
    return "\n".join(parts)


def assertion_signature(text):
    """What kind of check a line is: the assertion function and its message, ignoring the arguments.

    Retyping an assertion (a Gem becomes a Piece) keeps its signature. Deleting it, or swapping it for a
    different kind of check, does not.
    """
    call = ASSERTIONS.search(text)
    message = re.findall(r'"[^"]*"\s*\)?\s*$', text.strip())
    return (call.group(1), message[0].rstrip(") \t") if message else "")


def rule_key(text):
    stripped = text.strip()
    if stripped.startswith("|"):
        cells = [c.strip() for c in stripped.split("|") if c.strip()]
        return cells[0] if cells else None
    if re.match(r"^[-*] ", stripped):
        return stripped[2:].split(":")[0].strip()
    return None


def numbers_with_direction(text):
    """Numbers whose direction (a minimum or a maximum) the words around them state. Others are prose."""
    text = NOISE.sub(" ", text)
    cells = [c.strip() for c in text.strip().split("|") if c.strip()]
    if text.strip().startswith("|") and len(cells) == 3:
        # A measured-metric row: | Metric | Today | Direction |. The direction word sits in its own cell.
        direction = "min" if MIN_WORDS.search(cells[2]) else "max" if MAX_WORDS.search(cells[2]) else None
        first = NUMBER.search(cells[1])
        if direction and first:
            return [(float(first.group().replace(",", "")), direction)]
    found = []
    for match in NUMBER.finditer(text):
        before = text[max(0, match.start() - 24) : match.start()]
        after = text[match.end() : match.end() + 40]
        if MIN_WORDS.search(before) or MIN_WORDS.search(after):
            direction = "min"
        elif MAX_WORDS.search(before) or MAX_WORDS.search(after):
            direction = "max"
        else:
            continue
        found.append((float(match.group().replace(",", "")), direction))
    return found


def weakened(old, new):
    """Name the way `new` loosens `old`, or None when it tightens or is unchanged."""
    if re.search(r"\bblock\b", old, re.I) and not re.search(r"\bblock\b", new, re.I) and re.search(r"\bwarn\b", new, re.I):
        return "severity-lowered"
    for direction in ("min", "max"):
        was = [n for n, d in numbers_with_direction(old) if d == direction]
        now = [n for n, d in numbers_with_direction(new) if d == direction]
        for i, before in enumerate(was):
            if i >= len(now):
                return "threshold-removed"
            after = now[i]
            if after == before:
                continue
            if direction == "min" and after < before:
                return "threshold-loosened"
            if direction == "max" and after > before:
                return "threshold-loosened"
    return None


def main():
    base = "HEAD"
    if "--base" in sys.argv:
        base = sys.argv[sys.argv.index("--base") + 1]
    if base.startswith("-"):
        print(f"floor-guard: refusing a base that looks like a git option: {base}", file=sys.stderr)
        return 2

    top = git("rev-parse", "--show-toplevel").strip()
    if not top:
        print("floor-guard: not inside a git work tree", file=sys.stderr)
        return 2
    os.chdir(top)

    diff = git("diff", "--unified=0", base, "--") + "\n" + untracked_diff()
    added, removed, deleted = parse_diff(diff)

    findings = []

    def flag(rule, file, text):
        findings.append((rule, file, text.strip()[:120]))

    for file, text in added:
        if CODE_FILES.search(file):
            if SUPPRESSIONS.search(text):
                flag("silenced-checker", file, text)
            if STUBS.search(text):
                flag("unfinished-work", file, text)
            if SKIPS.search(text):
                flag("test-made-easier", file, text)
        if file.endswith("CONSTRAINTS.md") and EXCEPTION_ROW.match(text.strip()):
            flag("new-exception", file, text)

    for file in deleted:
        if TEST_FILES.search(file):
            flag("test-deleted", file, "file deleted")
    # A removed assertion is forgiven only when the same file adds one of the same kind in its place,
    # one for one. That lets a type migration retype assertions, and still catches a deleted one.
    replacements = {}
    for file, text in added:
        if TEST_FILES.search(file) and ASSERTIONS.search(text):
            key = (file, assertion_signature(text))
            replacements[key] = replacements.get(key, 0) + 1
    for file, text in removed:
        if TEST_FILES.search(file) and file not in deleted and ASSERTIONS.search(text):
            key = (file, assertion_signature(text))
            if replacements.get(key, 0) > 0:
                replacements[key] -= 1
            else:
                flag("assertion-removed", file, text)

    removed_rules = [(f, t) for f, t in removed if f.endswith("CONSTRAINTS.md") and rule_key(t)]
    added_rules = [(f, t) for f, t in added if f.endswith("CONSTRAINTS.md") and rule_key(t)]
    for file, text in removed_rules:
        counterpart = next((t for _, t in added_rules if rule_key(t) == rule_key(text)), None)
        if counterpart is None:
            if not EXCEPTION_ROW.match(text.strip()):
                flag("rule-removed", file, text)
            continue
        verdict = weakened(text, counterpart)
        if verdict:
            flag(verdict, file, f"{text.strip()}  ->  {counterpart.strip()}")

    if not findings:
        print("floor-guard: clean")
        return 0
    print(f"floor-guard: {len(findings)} floor violation(s):", file=sys.stderr)
    for rule, file, text in findings:
        print(f"  [{rule}] {file}: {text}", file=sys.stderr)
    print(
        "\nEach is a move that lowers the bar. Fix the code, or route it through a tracked exception.",
        file=sys.stderr,
    )
    return 1


if __name__ == "__main__":
    sys.exit(main())
