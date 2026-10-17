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
    fun anyFireInAClearStopsEveryGroupOfThatClearFromMakingASpecial() {
        // The swap makes a run of five and a run of three that holds an old blast, which fires.
        val board =
            patterned(*greens(3 to 0, 3 to 1, 3 to 3, 3 to 4, 4 to 2))
                .with(Pos(4, 0), Piece(Gem.YELLOW, Special.BLAST))
        val result = resolveSwap(board, Pos(4, 2), Pos(3, 2), Rng(1L))

        assertTrue(result.steps.any { it is Step.Activate })
        assertEquals(emptyList(), creates(firstClear(result.steps)))
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

    /** Counts qualifying groups on the board each Clear is applied to, skipping any Clear that follows an Activate. */
    private fun expectedCreates(
        start: Board,
        steps: List<Step>,
    ): Int {
        var board = start
        var fired = false
        var total = 0
        steps.forEachIndexed { index, step ->
            when (step) {
                is Step.Activate -> fired = true
                is Step.Clear -> {
                    if (!fired) total += findMatchGroups(board).count { classify(it) != null }
                    fired = false
                }
                else -> Unit
            }
            board = replay(start, steps.take(index + 1))
        }
        return total
    }

    private companion object {
        const val SEEDS = 300L
    }
}
