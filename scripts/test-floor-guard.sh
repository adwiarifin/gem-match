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
check "a re-aimed expected value is allowed" 0 's/assertEquals(expected, compute(input), "the result")/assertEquals(otherExpected, compute(input), "the result")/'
check "an assertion swapped for a trivial one is flagged" 1 's/assertEquals(expected, compute(input), "the result")/assertEquals(a, b, "the result")/'
check "an assertion swapped for a different kind is flagged" 1 's/assertNull(Game.decode(text))/assertTrue(Game.decode(text) == null)/'
check "a changed message is flagged" 1 's/"the result"/"another result"/'
check "an unrelated new assertion cannot cancel a deleted one" 1 's/assertTrue(board.isFull())/assertEquals(1, 1)/'
check "an argument dropped from an assertion is flagged" 1 's/assertEquals(expected, compute(input), "the result")/assertEquals(compute(input), "the result")/'
# The marker is written in two pieces so this file never trips the guard it tests.
check "a new suppression is flagged" 1 "s/^class FooTest {/@Supp""ress(\"x\") class FooTest {/"

if [ "$failures" -gt 0 ]; then
    echo "floor-guard self-test: $failures failed"
    exit 1
fi
echo "floor-guard self-test: all passed"
