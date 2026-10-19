package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The refill that follows a clear: new gems avoid matching a neighbour, so fewer pairs and fewer moves appear. */
class SpawnTest {
    private fun withHole(columns: IntRange): Board =
        columns.fold(patterned()) { board, col ->
            (0 until BOARD_SIZE).fold(board) { acc, row -> acc.with(Pos(row, col), null) }
        }

    private fun emptyCells(board: Board): List<Pos> = ALL_CELLS.filter { board[it] == null }

    @Test
    fun everyEmptyCellIsFilledWhateverTheRetries() {
        for (retries in listOf(0, SPAWN_RETRIES, 50)) {
            val (filled, spawn) = refill(withHole(2..4), Rng(3L), retries)

            assertTrue(ALL_CELLS.all { filled[it] != null }, "retries $retries")
            assertEquals(3 * BOARD_SIZE, spawn.gems.size, "retries $retries")
        }
    }

    @Test
    fun withNoRetriesEachEmptyCellTakesTheNextGemFromTheGenerator() {
        val board = withHole(1..2)
        val rng = Rng(9L)
        val reference = Rng(9L)

        val (filled, _) = refill(board, rng, retries = 0)

        for (col in 1..2) {
            for (row in 0 until BOARD_SIZE) {
                assertEquals(reference.nextGem(), filled[Pos(row, col)]?.gem, "row $row col $col")
            }
        }
        assertEquals(reference.state, rng.state)
    }

    @Test
    fun withManyRetriesANewGemNeverMatchesAnOrthogonalNeighbour() {
        for (seed in 1L..SEEDS) {
            val board = withHole(2..5)
            val (filled, _) = refill(board, Rng(seed), retries = MANY)

            for (pos in emptyCells(board)) {
                val gem = assertNotNull(filled[pos]?.gem)
                val neighbours =
                    listOf(
                        Pos(pos.row - 1, pos.col),
                        Pos(pos.row + 1, pos.col),
                        Pos(pos.row, pos.col - 1),
                        Pos(
                            pos.row,
                            pos.col + 1,
                        ),
                    )
                for (next in neighbours.filter { it.isInBounds() }) {
                    assertTrue(filled[next]?.gem != gem, "seed $seed: $pos and $next are both $gem")
                }
            }
        }
    }

    @Test
    fun theGeneratorAdvancesByAtLeastOneDrawPerGemAndAtMostOnePlusRetries() {
        val board = withHole(0..3)
        val gems = emptyCells(board).size
        val rng = Rng(11L)
        val before = Rng(11L)

        refill(board, rng, retries = SPAWN_RETRIES)

        val draws =
            generateSequence(0) { it + 1 }.first { count ->
                Rng(before.state).also { r -> repeat(count) { r.nextGem() } }.state ==
                    rng.state
            }
        assertTrue(draws in gems..gems * (1 + SPAWN_RETRIES), "$draws draws for $gems gems")
    }

    @Test
    fun theSameSeedGivesTheSameRefill() {
        val board = withHole(0..7)

        assertEquals(refill(board, Rng(21L), SPAWN_RETRIES), refill(board, Rng(21L), SPAWN_RETRIES))
    }

    @Test
    fun theDefaultRetriesAreOnAndTunable() {
        assertTrue(SPAWN_RETRIES > 0)
    }

    private companion object {
        const val SEEDS = 100L
        const val MANY = 100
    }
}
