package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResolveCreationTest {
    /** The red, orange, yellow diagonal has no runs and no moves; only the cells passed in are overwritten. */
    private fun patterned(vararg overrides: Pair<Pos, Char>): Board {
        val rows =
            (0 until BOARD_SIZE).map { row ->
                CharArray(BOARD_SIZE) { col -> "ROY"[(row + col) % 3] }
            }
        overrides.forEach { (pos, char) -> rows[pos.row][pos.col] = char }
        return boardOf(*rows.map { String(it) }.toTypedArray())
    }

    private fun greens(vararg cells: Pair<Int, Int>) = cells.map { Pos(it.first, it.second) to 'G' }.toTypedArray()

    private fun creates(steps: List<Step>) = steps.filterIsInstance<Step.Create>()

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
    fun anLLeavesALineGemAlongTheLongerArm() {
        val board = patterned(*greens(3 to 0, 3 to 1, 4 to 0, 5 to 0, 4 to 2))
        val result = resolveSwap(board, Pos(4, 2), Pos(3, 2), Rng(1L))

        assertEquals(listOf(Step.Create(Pos(3, 2), Piece(Gem.GREEN, Special.LINE_ROW))), creates(result.steps))
    }

    @Test
    fun aTLeavesACrossGem() {
        val board = patterned(*greens(3 to 1, 3 to 3, 4 to 2, 5 to 2, 2 to 2))
        val result = resolveSwap(board, Pos(2, 2), Pos(3, 2), Rng(1L))

        assertEquals(listOf(Step.Create(Pos(3, 2), Piece(Gem.GREEN, Special.CROSS))), creates(result.steps))
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
    fun onlyTheFirstMatchOfAMoveMakesSpecialsAndEveryBoardStaysFull() {
        var checked = 0
        for (seed in 1L..SEEDS) {
            val board = randomStableBoard(seed)
            val move = findValidMoves(board).firstOrNull() ?: continue

            val result = resolveSwap(board, move.from, move.to, Rng(seed))

            val expected = findMatchGroups(board.swap(move.from, move.to)).count { classify(it) != null }
            assertEquals(expected, creates(result.steps).size, "seed $seed")
            assertTrue(isFull(result.board), "seed $seed")
            assertEquals(result.board, replay(board, result.steps), "seed $seed")
            checked++
        }
        assertTrue(checked > SEEDS / 2, "too few boards had a move: $checked")
    }

    private companion object {
        const val SEEDS = 300L
    }
}
