package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
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

        val changed = original.with(Pos(2, 5), Piece(Gem.entries[1]))

        assertEquals(Piece(Gem.entries[1]), changed[Pos(2, 5)])
        assertNull(original[Pos(2, 5)])
    }

    @Test
    fun withNullClearsACell() {
        val board = Board.empty().with(Pos(0, 0), Piece(Gem.entries[0]))

        assertNull(board.with(Pos(0, 0), null)[Pos(0, 0)])
    }

    @Test
    fun swapExchangesTwoCells() {
        val a = Pos(1, 1)
        val b = Pos(1, 2)
        val board = Board.empty().with(a, Piece(Gem.entries[0])).with(b, Piece(Gem.entries[1]))

        val swapped = board.swap(a, b)

        assertEquals(Piece(Gem.entries[1]), swapped[a])
        assertEquals(Piece(Gem.entries[0]), swapped[b])
    }

    @Test
    fun readingOutsideTheBoardGivesNull() {
        val board = Board.empty().with(Pos(0, 0), Piece(Gem.entries[0]))

        assertNull(board[Pos(-1, 0)])
        assertNull(board[Pos(0, BOARD_SIZE)])
    }

    @Test
    fun writingOutsideTheBoardIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            Board.empty().with(Pos(BOARD_SIZE, 0), Piece(Gem.entries[0]))
        }
    }

    @Test
    fun equalBoardsAreEqual() {
        val a = Board.empty().with(Pos(3, 3), Piece(Gem.entries[2]))
        val b = Board.empty().with(Pos(3, 3), Piece(Gem.entries[2]))

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun pieceRoundTripsThroughTheBoard() {
        val special = Piece(Gem.GREEN, Special.CROSS)
        val board = Board.empty().with(Pos(4, 4), special)

        assertEquals(special, board[Pos(4, 4)])
        assertEquals(Gem.GREEN, board[Pos(4, 4)]?.gem)
        assertNull(board[Pos(0, 0)])
        assertNull(board[Pos(-1, 0)])
    }

    @Test
    fun withNullClearsAColourGemCell() {
        val board = Board.empty().with(Pos(1, 1), Piece(null, Special.COLOUR))

        assertNull(board.with(Pos(1, 1), null)[Pos(1, 1)])
    }

    @Test
    fun writingAPieceOutsideTheBoardIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            Board.empty().with(Pos(0, BOARD_SIZE), Piece(Gem.RED))
        }
    }

    @Test
    fun swapMovesSpecialsAsUnits() {
        val a = Pos(2, 2)
        val b = Pos(2, 3)
        val board =
            Board
                .empty()
                .with(a, Piece(Gem.RED, Special.BLAST))
                .with(b, Piece(null, Special.COLOUR))

        val swapped = board.swap(a, b)

        assertEquals(Piece(null, Special.COLOUR), swapped[a])
        assertEquals(Piece(Gem.RED, Special.BLAST), swapped[b])
    }

    @Test
    fun boardsWithDifferentSpecialsAreNotEqual() {
        val plain = Board.empty().with(Pos(0, 0), Piece(Gem.RED))
        val special = Board.empty().with(Pos(0, 0), Piece(Gem.RED, Special.BLAST))

        assertNotEquals(plain, special)
    }
}
