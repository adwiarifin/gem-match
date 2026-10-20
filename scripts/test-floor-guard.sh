#!/usr/bin/env bash
# Self-test for floor-guard.py: it must let a retyped or re-aimed assertion through, and flag a deleted one, an emptied
# one, one swapped for a different kind, and one "replaced" by an unrelated assertion elsewhere in the file.
# Runs in about a second in a throwaway git repository.
set -uo pipefail

cd "$(dirname "$0")/.."
guard=$PWD/scripts/floor-guard.py

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
failures=0

git -C "$work" init -q
git -C "$work" config user.email test@example.com
git -C "$work" config user.name test
mkdir -p "$work/core/src/test/kotlin"
test_file=core/src/test/kotlin/FooTest.kt

reset() {
    cat >"$work/$test_file" <<'KT'
class FooTest {
    @Test
    fun one() {
        assertEquals(Gem.RED, board[Pos(1, 1)])
        assertEquals(expected, compute(input), "the result")
        assertTrue(board.isFull())
        assertNull(Game.decode(text))
        assertEquals(Special.LINE_ROW, classify(group))
        assertEquals(
            actualValue,
            expectedValue,
            "the multi-line result",
        )
    }
}
KT
    git -C "$work" add -A && git -C "$work" commit -q --allow-empty -m base
}

# check <name> <expected exit code> <sed expression applied to the test file>
check() {
    local name=$1 want=$2 edit=$3
    reset
    sed -i.bak "$edit" "$work/$test_file" && rm -f "$work/$test_file.bak"
    (cd "$work" && python3 "$guard" >/dev/null 2>&1)
    local got=$?
    if [ "$got" -eq "$want" ]; then
        echo "ok   $name"
    else
        echo "FAIL $name (exit $got, wanted $want)"
        failures=$((failures + 1))
    fi
}

cp "$guard" "$work/floor-guard.py"
guard=$work/floor-guard.py

check "an unchanged file is clean" 0 's/^$/&/'
check "a deleted assertion is flagged" 1 '/assertTrue(board.isFull())/d'
check "a retyped assertion is allowed (Gem becomes Piece)" 0 's/assertEquals(Gem.RED, board\[Pos(1, 1)\])/assertEquals(Piece(Gem.RED), board[Pos(1, 1)])/'
check "a re-aimed expected value of the same kind is allowed" 0 's/assertEquals(Special.LINE_ROW, classify(group))/assertEquals(Special.CROSS, classify(group))/'
check "a re-aim to an unrelated name is flagged" 1 's/assertEquals(expected, compute(input), "the result")/assertEquals(otherExpected, compute(input), "the result")/'
check "an assertion swapped for a trivial one is flagged" 1 's/assertEquals(expected, compute(input), "the result")/assertEquals(a, b, "the result")/'
check "an assertion swapped for a different kind is flagged" 1 's/assertNull(Game.decode(text))/assertTrue(Game.decode(text) == null)/'
check "a changed message is flagged" 1 's/"the result"/"another result"/'
check "an unrelated new assertion cannot cancel a deleted one" 1 's/assertTrue(board.isFull())/assertEquals(1, 1)/'
check "an argument dropped from an assertion is flagged" 1 's/assertEquals(expected, compute(input), "the result")/assertEquals(compute(input), "the result")/'
# The marker is written in two pieces so this file never trips the guard it tests.
check "a new suppression is flagged" 1 "s/^class FooTest {/@Supp""ress(\"x\") class FooTest {/"

# Ways a test can be neutered while keeping an assertion-shaped line. Each must be flagged.
check "an assertion that compares a value to itself is flagged" 1 's/assertEquals(expected, compute(input), "the result")/assertEquals(expected, expected, "the result")/'
check "an always-true assertion is flagged" 1 's/assertTrue(board.isFull())/assertTrue(board.isFull() || true)/'
check "a commented-out assertion is flagged" 1 's/^        assertTrue(board.isFull())/        \/\/ assertTrue(board.isFull())/'
check "an assertion behind if (false) is flagged" 1 's/assertTrue(board.isFull())/if (false) assertTrue(board.isFull())/'
check "a multi-line assertion with an argument swapped is flagged" 1 's/^            actualValue,/            expectedValue,/'
check "a multi-line assertion that only moves is allowed" 0 's/^            "the multi-line result",/            "the multi-line result"/'
check "a removed test marker is flagged" 1 '/@Test/d'

# Wrappers and chains that change what is checked. Only constructors and members the project retypes with are kept.
check "an assertion wrapped in a helper that decides the result is flagged" 1 's/assertTrue(board.isFull())/assertTrue(alwaysTrue(board.isFull()))/'
check "an assertion negated by a chain member is flagged" 1 's/assertTrue(board.isFull())/assertTrue(board.isFull().not())/'
check "an argument replaced by a lambda result is flagged" 1 's/assertEquals(Gem.RED, board\[Pos(1, 1)\])/assertEquals(Gem.RED, board[Pos(1, 1)].let { true })/'
check "a different second argument beside a kept first one is flagged" 1 's/assertEquals(expected, compute(input), "the result")/assertEquals(expected, 42, "the result")/'
check "a Piece wrapper and a member chain are still allowed" 0 's/assertEquals(Gem.RED, board\[Pos(1, 1)\])/assertEquals(Piece(Gem.RED), board[Pos(1, 1)]?.piece)/'

# git can be told to treat a file as binary, which shows no changed lines at all.
reset
sed -i.bak '/assertTrue(board.isFull())/d' "$work/$test_file" && rm -f "$work/$test_file.bak"
printf '*Test.kt -diff\n' >"$work/.gitattributes"
(cd "$work" && python3 "$guard" >/dev/null 2>&1)
if [ $? -eq 1 ]; then echo "ok   a test hidden from the diff by .gitattributes is flagged"; else echo "FAIL a test hidden from the diff by .gitattributes is flagged"; failures=$((failures + 1)); fi
rm -f "$work/.gitattributes"

# The test-count floor may go up but not down, and a number may not be dropped.
floor_case() {
    local name=$1 want=$2 edit=$3
    reset
    mkdir -p "$work/scripts"
    printf '# floor\ncore 10\napp 5\n' >"$work/scripts/test-count-floor.txt"
    git -C "$work" add -A && git -C "$work" commit -q -m floor
    sed -i.bak "$edit" "$work/scripts/test-count-floor.txt" && rm -f "$work/scripts/test-count-floor.txt.bak"
    (cd "$work" && python3 "$guard" >/dev/null 2>&1)
    local got=$?
    if [ "$got" -eq "$want" ]; then echo "ok   $name"; else echo "FAIL $name (exit $got, wanted $want)"; failures=$((failures + 1)); fi
}
floor_case "raising the test-count floor is allowed" 0 's/^core 10/core 12/'
floor_case "lowering the test-count floor is flagged" 1 's/^core 10/core 9/'
floor_case "dropping a module from the test-count floor is flagged" 1 '/^app /d'

# An owner exception: a committed row waives named rules on one path until it expires; an uncommitted one waives nothing.
waiver_case() {
    local name=$1 want=$2 row=$3 commit=$4
    reset
    base_sha=$(git -C "$work" rev-parse HEAD)
    printf '## Exceptions\n\n| ID | Rule | Path | Reason | Owner | Expires |\n|----|------|------|--------|-------|---------|\n%s\n' "$row" >"$work/CONSTRAINTS.md"
    if [ "$commit" = yes ]; then git -C "$work" add -A && git -C "$work" commit -q -m waiver; fi
    sed -i.bak '/assertTrue(board.isFull())/d' "$work/$test_file" && rm -f "$work/$test_file.bak"
    (cd "$work" && python3 "$guard" --base "$base_sha" >/dev/null 2>&1)
    local got=$?
    if [ "$got" -eq "$want" ]; then echo "ok   $name"; else echo "FAIL $name (exit $got, wanted $want)"; failures=$((failures + 1)); fi
}
waiver_case "a committed exception waives its rule on its path" 0 "| W1 | assertion-removed | $test_file | reason | owner | 2999-01-01 |" yes
waiver_case "an exception for another rule waives nothing" 1 "| W1 | test-removed | $test_file | reason | owner | 2999-01-01 |" yes
waiver_case "an exception for another path waives nothing" 1 "| W1 | assertion-removed | core/src/test/kotlin/BarTest.kt | reason | owner | 2999-01-01 |" yes
waiver_case "an expired exception waives nothing" 1 "| W1 | assertion-removed | $test_file | reason | owner | 2000-01-01 |" yes
waiver_case "an uncommitted exception waives nothing" 1 "| W1 | assertion-removed | $test_file | reason | owner | 2999-01-01 |" no

# A test file moved out of the test tree has no removed lines, so it needs its own check.
reset
mkdir -p "$work/docs"
git -C "$work" mv "$test_file" docs/Foo.txt
(cd "$work" && python3 "$guard" >/dev/null 2>&1)
if [ $? -eq 1 ]; then echo "ok   a test file moved out of the test tree is flagged"; else echo "FAIL a test file moved out of the test tree is flagged"; failures=$((failures + 1)); fi

if [ "$failures" -gt 0 ]; then
    echo "floor-guard self-test: $failures failed"
    exit 1
fi
echo "floor-guard self-test: all passed"
