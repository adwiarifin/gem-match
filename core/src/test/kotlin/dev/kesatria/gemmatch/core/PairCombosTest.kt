package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins how many cells every pair of special gems clears, so changing an effect fails here and shows which pair
 * moved. The table is in `docs/special-gem-rules.md`. Both gems sit in the middle of the board (cells (3, 3) and
 * either (3, 4) or (4, 3)); `HORIZONTAL` is the swap along a row, `VERTICAL` along a column.
 */
class PairCombosTest {
    private val blast = Special.BLAST
    private val row = Special.LINE_ROW
    private val col = Special.LINE_COL
    private val cross = Special.CROSS
    private val shortX = Special.X_SHORT
    private val fullX = Special.X_FULL

    /** The pair, then the cells cleared when swapped sideways and when swapped up or down. */
    private val table =
        listOf(
            Triple(blast to blast, 12, 12),
            Triple(blast to row, 14, 14),
            Triple(blast to col, 14, 14),
            Triple(blast to cross, 19, 19),
            Triple(blast to shortX, 15, 15),
            Triple(blast to fullX, 20, 20),
            Triple(row to row, 8, 16),
            Triple(row to col, 15, 15),
            Triple(row to cross, 15, 22),
            Triple(row to shortX, 16, 15),
            Triple(row to fullX, 21, 20),
            Triple(col to col, 16, 8),
            Triple(col to cross, 22, 15),
            Triple(col to shortX, 15, 16),
            Triple(col to fullX, 20, 21),
            Triple(cross to cross, 22, 22),
            Triple(cross to shortX, 21, 21),
            Triple(cross to fullX, 26, 26),
            Triple(shortX to shortX, 18, 18),
            Triple(shortX to fullX, 23, 23),
            Triple(fullX to fullX, 28, 28),
        )

    private fun cleared(
        first: Special,
        second: Special,
        partner: Pos,
    ): Step.Clear {
        val a = Pos(3, 3)
        val board = patterned(a to Piece(Gem.WHITE, first), partner to Piece(Gem.BLUE, second))
        return resolveSwap(board, a, partner, Rng(1L)).steps.filterIsInstance<Step.Clear>().first()
    }

    @Test
    fun everyPairOfSpecialsClearsTheCellsInTheTable() {
        for ((pair, horizontal, vertical) in table) {
            val sideways = cleared(pair.first, pair.second, Pos(3, 4))
            val upDown = cleared(pair.first, pair.second, Pos(4, 3))

            assertEquals(horizontal, sideways.cells.size, "${pair.first} + ${pair.second} swapped along a row")
            assertEquals(vertical, upDown.cells.size, "${pair.first} + ${pair.second} swapped along a column")
            val rowCells = effectCells(Pos(3, 4), pair.first) + effectCells(Pos(3, 3), pair.second)
            val columnCells = effectCells(Pos(4, 3), pair.first) + effectCells(Pos(3, 3), pair.second)
            assertEquals(rowCells, sideways.cells, "${pair.first} + ${pair.second} cells along a row")
            assertEquals(columnCells, upDown.cells, "${pair.first} + ${pair.second} cells along a column")
            assertEquals(horizontal * POINTS_PER_EFFECT_GEM, sideways.points, "${pair.first} + ${pair.second} points")
            assertEquals(vertical * POINTS_PER_EFFECT_GEM, upDown.points, "${pair.first} + ${pair.second} points")
        }
    }

    @Test
    fun theTableCoversEveryPairOnce() {
        val kinds = Special.entries.filter { it != Special.COLOUR }
        val expected = kinds.indices.flatMap { i -> kinds.drop(i).map { setOf(kinds[i], it) } }.toSet()

        assertEquals(expected, table.map { setOf(it.first.first, it.first.second) }.toSet())
        assertEquals(expected.size, table.size)
    }

    @Test
    fun theOrderOfAPairDoesNotChangeTheCells() {
        for ((pair, horizontal, vertical) in table) {
            val sideways = cleared(pair.second, pair.first, Pos(3, 4))
            val upDown = cleared(pair.second, pair.first, Pos(4, 3))

            assertEquals(horizontal, sideways.cells.size, "${pair.second} + ${pair.first} swapped along a row")
            assertEquals(vertical, upDown.cells.size, "${pair.second} + ${pair.first} swapped along a column")
        }
    }
}
