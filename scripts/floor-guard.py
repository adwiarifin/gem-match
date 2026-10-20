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
from datetime import date

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


HUNK = re.compile(r"^@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@")


def parse_diff(diff):
    """Return (added, removed, deleted_files, added_at, removed_at).

    added and removed are (file, text) pairs. added_at and removed_at are (file, line number, text) triples: the line
    number is in the new file for an added line and in the base file for a removed one, both counted from 1.
    """
    added, removed, deleted, added_at, removed_at = [], [], [], [], []
    file = old_file = ""
    in_header = False
    old_no = new_no = 0
    for line in diff.split("\n"):
        if line.startswith("diff "):
            in_header = True
        elif line.startswith("@@"):
            in_header = False
            hunk = HUNK.match(line)
            if hunk:
                old_no, new_no = int(hunk.group(1)), int(hunk.group(2))
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
            added_at.append((file, new_no, line[1:]))
            new_no += 1
        elif line.startswith("-"):
            removed.append((file, line[1:]))
            removed_at.append((file, old_no, line[1:]))
            old_no += 1
    return added, removed, deleted, added_at, removed_at


def untracked_diff():
    names = git("ls-files", "--others", "--exclude-standard").split("\n")
    parts = []
    for name in filter(None, names):
        result = subprocess.run(
            ["git", "diff", "--no-index", "--unified=0", "--text", "/dev/null", name],
            capture_output=True,
            text=True,
        )
        if result.returncode not in (0, 1):
            print(f"floor-guard: could not diff {name}", file=sys.stderr)
            sys.exit(2)
        parts.append(result.stdout)
    return "\n".join(parts)


LOOKBACK = 12
NEUTERED = re.compile(r"\|\|\s*true\b|&&\s*false\b|\bif\s*\(\s*false\s*\)|\bwhile\s*\(\s*false\s*\)")
# The only wrapper and member chain the project retypes assertions with (`Gem.RED` to `Piece(Gem.RED)`, `board[p]` to
# `board[p]?.gem`). Anything else around an argument can change what is checked, so it needs a human.
KEPT_WRAPPER = r"Piece\({o}\)"
KEPT_CHAIN = r"{o}(?:(?:\?\.|\.)(?:gem|special|piece|ordinal|isSpecial))+"
HIDES_DIFF = re.compile(r"(^|\s)(-diff|!diff|diff=|binary|-text)(\s|$)")
IDENTIFIER = re.compile(r"[A-Za-z_]\w*")
NUMBER_LITERAL = re.compile(r"[\d_.]+[LlFfDd]?")


def strip_comment(text):
    """The line without its // comment (outside strings) and without any /* ... */ on it."""
    out, quote, i = "", None, 0
    while i < len(text):
        char = text[i]
        if quote:
            out += char
            if char == "\\" and i + 1 < len(text):
                out += text[i + 1]
                i += 1
            elif char == quote:
                quote = None
        elif char in "\"'":
            quote = char
            out += char
        elif text.startswith("//", i):
            break
        else:
            out += char
        i += 1
    return re.sub(r"/\*.*?\*/", "", out)


def is_assertion(line):
    return bool(ASSERTIONS.search(strip_comment(line)))


def call_arguments(text):
    """The top-level arguments of the first assertion call in the text, or None if the call does not close in it."""
    start = ASSERTIONS.search(text).end()
    args, current, depth, quote, i = [], "", 0, None, start
    while i < len(text):
        char = text[i]
        if quote:
            current += char
            if char == "\\" and i + 1 < len(text):
                current += text[i + 1]
                i += 1
            elif char == quote:
                quote = None
        elif char in "\"'":
            quote = char
            current += char
        elif char in "([{":
            depth += 1
            current += char
        elif char in ")]}":
            if depth == 0 and char == ")":
                args.append(current.strip())
                return [a for a in args[:-1]] + ([args[-1]] if args[-1] else [])
            depth -= 1
            current += char
        elif char == "," and depth == 0:
            args.append(current.strip())
            current = ""
        else:
            current += char
        i += 1
    return None


def squash(text):
    return re.sub(r"\s+", "", text)


def assertion_signature(text):
    """The assertion function and its message (a string literal last argument), ignoring the other arguments."""
    function = ASSERTIONS.search(text).group(1)
    args = call_arguments(text)
    message = args[-1] if args and args[-1].startswith('"') else ""
    return (function, message)


def joined_call(lines, index):
    """The assertion starting at lines[index] joined into one string, and how many lines it spans."""
    text = ""
    for count in range(1, LOOKBACK + 1):
        if index + count > len(lines):
            break
        text += " " + strip_comment(lines[index + count - 1]).strip()
        if call_arguments(text.strip()) is not None:
            return text.strip(), count
    return strip_comment(lines[index]).strip(), 1


def assertion_owner(lines, number):
    """The index of the assertion that the changed line number (from 1) belongs to, or None."""
    index = number - 1
    if index >= len(lines):
        return None
    if is_assertion(lines[index]):
        return index
    for start in range(index - 1, max(index - LOOKBACK, 0) - 1, -1):
        if is_assertion(lines[start]):
            _, span = joined_call(lines, start)
            return start if start + span > index else None
    return None


def kept(old_arg, new_arg):
    """True if the new argument is the old one as it was, or wrapped or followed by a member the project retypes with
    (see KEPT_WRAPPER and KEPT_CHAIN). Anything else drops or changes what the old argument checked."""
    old, new = squash(old_arg), squash(new_arg)
    if new == old:
        return True
    escaped = re.escape(old)
    return bool(re.fullmatch(KEPT_WRAPPER.format(o=escaped), new) or re.fullmatch(KEPT_CHAIN.format(o=escaped), new))


def re_aimed(old_arg, new_arg):
    """True if a changed argument is plausibly the same check aimed at a new expectation: two number literals, or
    two expressions that share a name (`Special.LINE_ROW` to `Special.CROSS`). An argument that merely wraps the old
    one in something else is not."""
    old, new = squash(old_arg), squash(new_arg)
    if old in new:
        return False
    if NUMBER_LITERAL.fullmatch(old) and NUMBER_LITERAL.fullmatch(new):
        return True
    return bool(set(IDENTIFIER.findall(old_arg)) & set(IDENTIFIER.findall(new_arg)))


def same_assertion(removed, added):
    """True if `added` can stand in for `removed`: a retyped or re-aimed assertion, not a different or emptied one.

    Same function and message, the same number of arguments, at least one non-string argument kept (see `kept`) and
    every other one either kept or re-aimed (see `re_aimed`), no argument repeated that was not repeated before
    (`assertEquals(x, x)`), and nothing that always passes (`|| true`, `if (false)`). Both sides are whole calls,
    joined across lines.
    """
    if assertion_signature(removed) != assertion_signature(added):
        return False
    if NEUTERED.search(added) and not NEUTERED.search(removed):
        return False
    old, new = call_arguments(removed), call_arguments(added)
    if old is None or new is None:
        return squash(removed) == squash(added)
    if len(old) != len(new):
        return False
    plain_new = [squash(a) for a in new if not a.startswith(('"', "'"))]
    plain_old = [squash(a) for a in old if not a.startswith(('"', "'"))]
    if len(set(plain_new)) < len(plain_new) and len(set(plain_old)) == len(plain_old):
        return False
    pairs = [(o, n) for o, n in zip(old, new) if not o.startswith(('"', "'"))]
    if not any(kept(o, n) for o, n in pairs):
        return False
    return all(kept(o, n) or re_aimed(o, n) for o, n in pairs)


def read_new(file):
    try:
        with open(file, encoding="utf-8") as handle:
            return handle.read().split("\n")
    except OSError:
        return []


def read_old(base, file):
    result = subprocess.run(["git", "show", f"{base}:{file}"], capture_output=True, text=True)
    return result.stdout.split("\n") if result.returncode == 0 else []


def changed_assertions(changes, lines):
    """The whole assertion calls that the changed lines belong to, once each."""
    owners = sorted({o for _, number, _ in changes if (o := assertion_owner(lines, number)) is not None})
    return [joined_call(lines, o)[0] for o in owners]


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


def waivers():
    """Exception rows of the committed CONSTRAINTS.md as (rules, path) pairs, expired ones left out.

    Row: `| W1 | rule, rule | path | reason | owner | YYYY-MM-DD |`. Only the committed file counts, so a row added in
    the working tree is flagged as a new exception and waives nothing until its owner commits it (a committed row is
    not "new", so a release check over a longer range does not flag it).
    """
    found = []
    for line in read_old("HEAD", "CONSTRAINTS.md"):
        if not EXCEPTION_ROW.match(line.strip()):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 6:
            continue
        try:
            live = date.fromisoformat(cells[5]) >= date.today()
        except ValueError:
            live = False
        if live:
            found.append(({r.strip() for r in cells[1].split(",")}, cells[2]))
    return found


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

    diff = git("diff", "--unified=0", "--no-renames", "--text", base, "--") + "\n" + untracked_diff()
    added, removed, deleted, added_at, removed_at = parse_diff(diff)

    findings = []
    committed_rows = set(read_old("HEAD", "CONSTRAINTS.md"))

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
        if file.endswith("CONSTRAINTS.md") and EXCEPTION_ROW.match(text.strip()) and text not in committed_rows:
            flag("new-exception", file, text)
        if file.endswith(".gitattributes") and HIDES_DIFF.search(text):
            flag("hidden-diff", file, text)

    for file in deleted:
        if TEST_FILES.search(file):
            flag("test-deleted", file, "file deleted")
    # A removed assertion is forgiven only when the same file adds one that can stand in for it, one for one
    # (see same_assertion). That lets a type migration retype assertions, and still catches a deleted, emptied,
    # commented-out, neutered or swapped-for-something-trivial one.
    for file in sorted({f for f, _, _ in removed_at} | {f for f, _, _ in added_at}):
        if not TEST_FILES.search(file) or file in deleted:
            continue
        old_lines = read_old(base, file)
        new_lines = read_new(file)
        gone = changed_assertions([c for c in removed_at if c[0] == file], old_lines)
        stand_ins = changed_assertions([c for c in added_at if c[0] == file], new_lines)
        for text in gone:
            match = next((i for i, cand in enumerate(stand_ins) if same_assertion(text, cand)), None)
            if match is None:
                flag("assertion-removed", file, text)
            else:
                stand_ins.pop(match)
        markers_removed = sum(1 for f, _, t in removed_at if f == file and re.match(r"\s*@Test\b", t))
        markers_added = sum(1 for f, _, t in added_at if f == file and re.match(r"\s*@Test\b", t))
        if markers_removed > markers_added:
            flag("test-removed", file, f"{markers_removed - markers_added} @Test marker(s) removed")

    # The test-count floor may be raised, never lowered or dropped.
    floor_line = re.compile(r"^\s*(\w+)\s+(\d+)\s*$")
    for file, text in removed:
        old = floor_line.match(text)
        if file.endswith("test-count-floor.txt") and old:
            now = [floor_line.match(t) for f, t in added if f == file]
            match = next((m for m in now if m and m.group(1) == old.group(1)), None)
            if match is None or int(match.group(2)) < int(old.group(2)):
                flag("test-floor-lowered", file, text)

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

    allowed = waivers()
    waived = [f for f in findings if any(f[0] in rules and f[1] == path for rules, path in allowed)]
    findings = [f for f in findings if f not in waived]
    if waived:
        print(f"floor-guard: {len(waived)} finding(s) waived by an owner exception in CONSTRAINTS.md")
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
