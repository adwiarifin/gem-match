package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BoardTest {
    @Test
    fun emptyBoardHasNoGems() {
        val board = Board.empty()

        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                assertNull(board[Pos(row, col)])
            }
        }
    }

    @Test
    fun withPlacesAGemAndLeavesTheOriginalUntouched() {
        val original = Board.empty()

        val changed = original.with(Pos(2, 5), Gem.entries[1])

        assertEquals(Gem.entries[1], changed[Pos(2, 5)])
        assertNull(original[Pos(2, 5)])
    }

    @Test
    fun withNullClearsACell() {
        val board = Board.empty().with(Pos(0, 0), Gem.entries[0])

        assertNull(board.with(Pos(0, 0), null)[Pos(0, 0)])
    }

    @Test
    fun swapExchangesTwoCells() {
        val a = Pos(1, 1)
        val b = Pos(1, 2)
        val board = Board.empty().with(a, Gem.entries[0]).with(b, Gem.entries[1])

        val swapped = board.swap(a, b)

        assertEquals(Gem.entries[1], swapped[a])
        assertEquals(Gem.entries[0], swapped[b])
    }

    @Test
    fun readingOutsideTheBoardGivesNull() {
        val board = Board.empty().with(Pos(0, 0), Gem.entries[0])

        assertNull(board[Pos(-1, 0)])
        assertNull(board[Pos(0, BOARD_SIZE)])
    }

    @Test
    fun writingOutsideTheBoardIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            Board.empty().with(Pos(BOARD_SIZE, 0), Gem.entries[0])
        }
    }

    @Test
    fun equalBoardsAreEqual() {
        val a = Board.empty().with(Pos(3, 3), Gem.entries[2])
        val b = Board.empty().with(Pos(3, 3), Gem.entries[2])

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
