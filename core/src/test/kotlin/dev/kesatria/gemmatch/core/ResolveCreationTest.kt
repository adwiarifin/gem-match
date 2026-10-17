package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResolveCreationTest {
    /** The shared diagonal board with gems written over it by character, for example `3 to 1` cells in green. */
    private fun patterned(vararg overrides: Pair<Pos, Char>): Board {
        val pieces = overrides.map { (pos, char) -> pos to Piece(gemOf(char)) }
        return patternedBoard(pieces)
    }

    private fun greens(vararg cells: Pair<Int, Int>) = cells.map { Pos(it.first, it.second) to 'G' }.toTypedArray()

    private fun creates(steps: List<Step>) = steps.filterIsInstance<Step.Create>()

    /** The steps up to the first Fall: the first clear of the move and what it made. */
    private fun firstClear(steps: List<Step>) = steps.takeWhile { it !is Step.Fall }

    @Test
    fun aRunOfFourLeavesABlastAtTheSwappedCell() {
        val board = patterned(*greens(3 to 1, 3 to 2, 3 to 4, 4 to 3))
        val result = resolveSwap(board, Pos(4, 3), Pos(3, 3), Rng(1L))

        assertEquals(listOf(Step.Create(Pos(3, 3), Piece(Gem.GREEN, Special.BLAST))), creates(result.steps))
    }

    @Test
    fun aRunOfFiveLeavesAColourGem() {
        val board = patterned(*greens(3 to 0, 3 to 1, 3 to 3, 3 to 4, 4 to 2))
        val result = resolveSwap(board, Pos(4, 2), Pos(3, 2), Rng(1L))

        assertEquals(listOf(Step.Create(Pos(3, 2), Piece(null, Special.COLOUR))), creates(result.steps))
    }

    @Test
    fun anLLeavesACrossGem() {
        val board = patterned(*greens(3 to 0, 3 to 1, 4 to 0, 5 to 0, 4 to 2))
        val result = resolveSwap(board, Pos(4, 2), Pos(3, 2), Rng(1L))

        assertEquals(listOf(Step.Create(Pos(3, 2), Piece(Gem.GREEN, Special.CROSS))), creates(result.steps))
    }

    @Test
    fun aShortTLeavesAShortXGem() {
        val board = patterned(*greens(3 to 1, 3 to 3, 4 to 2, 5 to 2, 2 to 2))
        val result = resolveSwap(board, Pos(2, 2), Pos(3, 2), Rng(1L))

        assertEquals(listOf(Step.Create(Pos(3, 2), Piece(Gem.GREEN, Special.X_SHORT))), creates(result.steps))
    }

    @Test
    fun aSwapFormingTwoGroupsCanLeaveTwoSpecials() {
        val board =
            patterned(
                Pos(4, 0) to 'G',
                Pos(4, 1) to 'G',
                Pos(4, 2) to 'B',
                Pos(4, 3) to 'G',
                Pos(3, 0) to 'B',
                Pos(3, 1) to 'B',
                Pos(3, 2) to 'G',
                Pos(3, 3) to 'B',
            )
        val result = resolveSwap(board, Pos(3, 2), Pos(4, 2), Rng(1L))

        assertEquals(
            setOf(
                Step.Create(Pos(4, 2), Piece(Gem.GREEN, Special.BLAST)),
                Step.Create(Pos(3, 2), Piece(Gem.BLUE, Special.BLAST)),
            ),
            creates(result.steps).toSet(),
        )
    }

    @Test
    fun aShortLLeavesALineGemAndTakesTheHangingGem() {
        val board = patterned(*greens(3 to 1, 3 to 2, 4 to 3, 4 to 1))
        val result = resolveSwap(board, Pos(4, 3), Pos(3, 3), Rng(1L))

        val made = creates(firstClear(result.steps))
        assertEquals(listOf(Step.Create(Pos(3, 3), Piece(Gem.GREEN, Special.LINE_ROW))), made)
        val clear = result.steps.filterIsInstance<Step.Clear>().first()
        assertEquals(cells(3 to 1, 3 to 2, 3 to 3, 4 to 1), clear.cells)
        assertEquals(POINTS_RUN_OF_4, clear.points)
    }

    @Test
    fun aGemHangingOffTheMiddleStaysAPlainMatchAndStaysOnTheBoard() {
        val board = patterned(*greens(3 to 1, 3 to 2, 4 to 3, 4 to 2))
        val result = resolveSwap(board, Pos(4, 3), Pos(3, 3), Rng(1L))

        assertEquals(emptyList(), creates(firstClear(result.steps)))
        assertEquals(
            cells(3 to 1, 3 to 2, 3 to 3),
            result.steps
                .filterIsInstance<Step.Clear>()
                .first()
                .cells,
        )
    }

    @Test
    fun aLongTLeavesAFullXGemEvenWithARunOfFive() {
        val board = patterned(*greens(3 to 0, 3 to 1, 3 to 3, 3 to 4, 4 to 2, 5 to 2, 2 to 2))
        val result = resolveSwap(board, Pos(2, 2), Pos(3, 2), Rng(1L))

        val made = creates(firstClear(result.steps))
        assertEquals(listOf(Step.Create(Pos(3, 2), Piece(Gem.GREEN, Special.X_FULL))), made)
    }

    @Test
    fun aFireInAClearDoesNotStopTheMatchesOfThatClearFromMakingSpecials() {
        // The swap makes a run of five and a run of three that holds an old blast, which fires. The yellow gem at
        // (5, 0) hangs off the end of the second run, so that group is a short L and makes a line gem too.
        val board =
            patterned(*greens(3 to 0, 3 to 1, 3 to 3, 3 to 4, 4 to 2))
                .with(Pos(4, 0), Piece(Gem.YELLOW, Special.BLAST))
        val result = resolveSwap(board, Pos(4, 2), Pos(3, 2), Rng(1L))

        assertTrue(result.steps.any { it is Step.Activate })
        val expected =
            listOf(
                Step.Create(Pos(3, 2), Piece(null, Special.COLOUR)),
                Step.Create(Pos(4, 2), Piece(Gem.YELLOW, Special.LINE_ROW)),
            )
        assertEquals(expected, creates(firstClear(result.steps)))
    }

    private fun whites(vararg cells: Pair<Int, Int>): Array<Pair<Pos, Piece>> =
        cells.map { (row, col) -> Pos(row, col) to Piece(Gem.WHITE) }.toTypedArray()

    private fun madeBySwappingIn(
        special: Special,
        from: Pos,
        to: Pos,
        vararg others: Pair<Int, Int>,
    ): Pair<List<Step.Create>, List<Step.Activate>> {
        val piece = Piece(if (special == Special.COLOUR) null else Gem.WHITE, special)
        val board = patterned(*whites(*others), from to piece)
        val steps = resolveSwap(board, from, to, Rng(1L)).steps
        return creates(firstClear(steps)) to steps.takeWhile { it !is Step.Fall }.filterIsInstance<Step.Activate>()
    }

    @Test
    fun aSpecialSwappedIntoARunOfFiveStillMakesAColourGem() {
        // The owner's case: white, white, column white, white, white.
        for (special in Special.entries.filter { it != Special.COLOUR }) {
            val (made, fired) = madeBySwappingIn(special, Pos(4, 2), Pos(3, 2), 3 to 0, 3 to 1, 3 to 3, 3 to 4)

            assertEquals(listOf(Step.Create(Pos(3, 2), Piece(null, Special.COLOUR))), made, "$special")
            assertTrue(fired.isNotEmpty(), "$special still fires")
        }
    }

    @Test
    fun aSpecialSwappedIntoARunOfFourStillMakesABlast() {
        for (special in Special.entries.filter { it != Special.COLOUR }) {
            val (made, fired) = madeBySwappingIn(special, Pos(4, 3), Pos(3, 3), 3 to 1, 3 to 2, 3 to 4)

            assertEquals(listOf(Step.Create(Pos(3, 3), Piece(Gem.WHITE, Special.BLAST))), made, "$special")
            assertTrue(fired.isNotEmpty(), "$special still fires")
        }
    }

    @Test
    fun aSpecialSwappedIntoTheCornerOfAnLStillMakesACrossGem() {
        for (special in Special.entries.filter { it != Special.COLOUR }) {
            val (made, fired) = madeBySwappingIn(special, Pos(2, 2), Pos(3, 2), 3 to 0, 3 to 1, 4 to 2, 5 to 2)

            assertEquals(listOf(Step.Create(Pos(3, 2), Piece(Gem.WHITE, Special.CROSS))), made, "$special")
            assertTrue(fired.isNotEmpty(), "$special still fires")
        }
    }

    @Test
    fun aSpecialSwappedIntoAShortTStillMakesAShortX() {
        for (special in Special.entries.filter { it != Special.COLOUR }) {
            val (made, fired) = madeBySwappingIn(special, Pos(2, 2), Pos(3, 2), 3 to 1, 3 to 3, 4 to 2, 5 to 2)

            assertEquals(listOf(Step.Create(Pos(3, 2), Piece(Gem.WHITE, Special.X_SHORT))), made, "$special")
            assertTrue(fired.isNotEmpty(), "$special still fires")
        }
    }

    @Test
    fun aPlainRunOfThreeLeavesNothing() {
        val board = patterned(*greens(3 to 1, 3 to 2, 4 to 3))
        val result = resolveSwap(board, Pos(4, 3), Pos(3, 3), Rng(1L))

        assertEquals(emptyList(), creates(result.steps))
    }

    @Test
    fun stepOrderIsSwapClearCreateFallSpawn() {
        val board = patterned(*greens(3 to 1, 3 to 2, 3 to 4, 4 to 3))
        val result = resolveSwap(board, Pos(4, 3), Pos(3, 3), Rng(1L))

        assertTrue(result.steps[0] is Step.Swap)
        assertTrue(result.steps[1] is Step.Clear)
        assertTrue(result.steps[2] is Step.Create)
        assertTrue(result.steps[3] is Step.Fall)
        assertTrue(result.steps[4] is Step.Spawn)
    }

    @Test
    fun aSpecialMadeByAVerticalRunFallsOnceTheCellsBelowClear() {
        val board = patterned(*greens(2 to 3, 3 to 3, 5 to 3, 4 to 2))
        val result = resolveSwap(board, Pos(4, 2), Pos(4, 3), Rng(1L))

        val blast = Piece(Gem.GREEN, Special.BLAST)
        assertEquals(listOf(Step.Create(Pos(4, 3), blast)), creates(result.steps))
        val fall = result.steps.filterIsInstance<Step.Fall>().first()
        assertTrue(GemMove(blast, Pos(4, 3), Pos(5, 3)) in fall.moves)
    }

    @Test
    fun everyQualifyingGroupOfAMoveMakesASpecialAtAnyDepth() {
        var checked = 0
        var cascadeMade = 0
        for (seed in 1L..SEEDS) {
            val board = randomStableBoard(seed)
            val move = findValidMoves(board).firstOrNull() ?: continue

            val result = resolveSwap(board, move.from, move.to, Rng(seed))

            val expected = expectedCreates(board, result.steps)
            assertEquals(expected, creates(result.steps).size, "seed $seed")
            assertTrue(isFull(result.board), "seed $seed")
            assertEquals(result.board, replay(board, result.steps), "seed $seed")
            cascadeMade += creates(result.steps.drop(result.steps.indexOfFirst { it is Step.Fall } + 1)).size
            checked++
        }
        assertTrue(checked > SEEDS / 2, "too few boards had a move: $checked")
        assertTrue(cascadeMade > 0, "no cascade ever made a special")
    }

    @Test
    fun withAnyKindOfSpecialOnTheBoardEveryMatchGroupStillMakesItsSpecial() {
        var checked = 0
        for (special in Special.entries.filter { it != Special.COLOUR }) {
            for (seed in 1L..SPECIAL_SEEDS) {
                val base = randomStableBoard(seed)
                val cell = Pos((seed % BOARD_SIZE).toInt(), (seed / 2 % BOARD_SIZE).toInt())
                val board = base.with(cell, Piece(base[cell]?.gem, special))
                if (findRuns(board).isNotEmpty()) continue

                for (move in findMatchMoves(board).take(6)) {
                    val result = resolveSwap(board, move.from, move.to, Rng(seed))

                    assertEquals(expectedCreates(board, result.steps), creates(result.steps).size, "$special seed $seed $move")
                    assertEquals(result.board, replay(board, result.steps), "$special seed $seed $move")
                    checked++
                }
            }
        }
        assertTrue(checked > SPECIAL_SEEDS, "too few moves were checked: $checked")
    }

    /** Counts the qualifying groups on the board each Clear is applied to. A fire in the same clear does not matter. */
    private fun expectedCreates(
        start: Board,
        steps: List<Step>,
    ): Int {
        var board = start
        var total = 0
        steps.forEachIndexed { index, step ->
            if (step is Step.Clear) total += findMatchGroups(board).count { classify(it) != null }
            board = replay(start, steps.take(index + 1))
        }
        return total
    }

    private companion object {
        const val SEEDS = 300L
        const val SPECIAL_SEEDS = 120L
    }
}
