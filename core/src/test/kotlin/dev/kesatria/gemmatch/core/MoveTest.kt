package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoveTest {
    @Test
    fun swapCompletingAHorizontalRunIsValid() {
        val board = boardOf("RRBR")

        assertTrue(isValidSwap(board, Pos(0, 2), Pos(0, 3)))
    }

    @Test
    fun swapCompletingAVerticalRunIsValid() {
        val board =
            boardOf(
                "R",
                "R",
                "B",
                "R",
            )

        assertTrue(isValidSwap(board, Pos(2, 0), Pos(3, 0)))
    }

    @Test
    fun swapThatMakesNoRunIsInvalid() {
        val board = boardOf("RGBY")

        assertFalse(isValidSwap(board, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun nonAdjacentSwapIsInvalidEvenIfItWouldMatch() {
        val board = boardOf("RRBBR")

        assertFalse(isValidSwap(board, Pos(0, 2), Pos(0, 4)))
    }

    @Test
    fun diagonalSwapIsInvalid() {
        val board =
            boardOf(
                "RR",
                "BR",
            )

        assertFalse(isValidSwap(board, Pos(0, 0), Pos(1, 1)))
    }

    @Test
    fun swapOutsideTheBoardIsInvalidWithoutThrowing() {
        val board = boardOf("RRBR")

        assertFalse(isValidSwap(board, Pos(0, BOARD_SIZE - 1), Pos(0, BOARD_SIZE)))
        assertFalse(isValidSwap(board, Pos(-1, 0), Pos(0, 0)))
    }

    @Test
    fun swapWithAnEmptyCellIsInvalid() {
        val board = boardOf("RR.R")

        assertFalse(isValidSwap(board, Pos(0, 2), Pos(0, 3)))
    }

    @Test
    fun validityDoesNotDependOnSwapOrder() {
        val board = boardOf("RRBR")

        assertEquals(isValidSwap(board, Pos(0, 2), Pos(0, 3)), isValidSwap(board, Pos(0, 3), Pos(0, 2)))
    }

    @Test
    fun findValidMovesListsEachMoveOnce() {
        val board = boardOf("RRBR")

        assertEquals(listOf(Move(Pos(0, 2), Pos(0, 3))), findValidMoves(board))
    }

    @Test
    fun emptyBoardHasNoMoves() {
        assertTrue(findValidMoves(Board.empty()).isEmpty())
        assertFalse(hasValidMove(Board.empty()))
    }

    @Test
    fun threeColourDiagonalPatternHasNoMoves() {
        val board =
            boardOf(
                "ROYROYRO",
                "OYROYROY",
                "YROYROYR",
                "ROYROYRO",
                "OYROYROY",
                "YROYROYR",
                "ROYROYRO",
                "OYROYROY",
            )

        assertTrue(findRuns(board).isEmpty())
        assertFalse(hasValidMove(board))
        assertTrue(findValidMoves(board).isEmpty())
    }

    @Test
    fun twoColourCheckerboardStillHasMoves() {
        val board =
            boardOf(
                "RGRGRGRG",
                "GRGRGRGR",
                "RGRGRGRG",
                "GRGRGRGR",
                "RGRGRGRG",
                "GRGRGRGR",
                "RGRGRGRG",
                "GRGRGRGR",
            )

        assertTrue(hasValidMove(board))
    }

    @Test
    fun hasValidMoveAgreesWithFindValidMoves() {
        val board = boardOf("RRBR")

        assertTrue(hasValidMove(board))
    }

    @Test
    fun findValidMovesMatchesBruteForceOnManyRandomBoards() {
        for (seed in 1L..SEEDS) {
            val board = randomStableBoard(seed)

            val found = findValidMoves(board).map { setOf(it.from, it.to) }.toSet()

            assertEquals(bruteForceMoves(board), found, "seed $seed")
            assertEquals(found.isNotEmpty(), hasValidMove(board), "seed $seed")
        }
    }

    /** Independent reference: try every ordered adjacent pair and look for any run after swapping. */
    private fun bruteForceMoves(board: Board): Set<Set<Pos>> {
        val cells = (0 until BOARD_SIZE).flatMap { row -> (0 until BOARD_SIZE).map { col -> Pos(row, col) } }
        return cells
            .flatMap { a -> neighboursOf(a).map { b -> a to b } }
            .filter { (a, b) -> findRuns(board.swap(a, b)).isNotEmpty() }
            .map { (a, b) -> setOf(a, b) }
            .toSet()
    }

    private fun neighboursOf(pos: Pos): List<Pos> =
        listOf(
            Pos(pos.row + 1, pos.col),
            Pos(pos.row - 1, pos.col),
            Pos(pos.row, pos.col + 1),
            Pos(pos.row, pos.col - 1),
        ).filter { it.isInBounds() }

    private companion object {
        const val SEEDS = 300L
    }

    private val special = Piece(Gem.RED, Special.BLAST)

    @Test
    fun swapTouchingASpecialIsValidWithNoMatch() {
        val board = boardOf("RGBY").with(Pos(0, 1), special)

        assertTrue(isValidSwap(board, Pos(0, 0), Pos(0, 1)))
        assertTrue(isValidSwap(board, Pos(0, 1), Pos(0, 2)))
    }

    @Test
    fun swapOfTwoSpecialsIsValid() {
        val board = boardOf("RG").with(Pos(0, 0), special).with(Pos(0, 1), Piece(null, Special.COLOUR))

        assertTrue(isValidSwap(board, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun swapOfTwoPlainGemsStillNeedsAMatch() {
        val board = boardOf("RGBY").with(Pos(0, 3), special)

        assertFalse(isValidSwap(board, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun specialSwapWithAnEmptyCellIsInvalid() {
        val board = boardOf("R.").with(Pos(0, 0), special)

        assertFalse(isValidSwap(board, Pos(0, 0), Pos(0, 1)))
    }

    @Test
    fun specialSwapMustStillBeAdjacent() {
        val board = boardOf("RGB").with(Pos(0, 0), special)

        assertFalse(isValidSwap(board, Pos(0, 0), Pos(0, 2)))
    }

    @Test
    fun aBoardHoldingASpecialAlwaysHasAValidMove() {
        for (seed in 1L..SEEDS) {
            val base = randomStableBoard(seed)
            val board = base.with(Pos((seed % BOARD_SIZE).toInt(), (seed / 3 % BOARD_SIZE).toInt()), special)

            assertTrue(hasValidMove(board), "seed $seed")
            assertTrue(findValidMoves(board).isNotEmpty(), "seed $seed")
        }
    }

    @Test
    fun findMatchMovesAgreesWithTheOldRuleOnPlainBoards() {
        for (seed in 1L..SEEDS) {
            val board = randomStableBoard(seed)

            assertEquals(bruteForceMoves(board), findMatchMoves(board).map { setOf(it.from, it.to) }.toSet(), "seed $seed")
            assertEquals(findMatchMoves(board), findValidMoves(board), "seed $seed")
        }
    }

    @Test
    fun findMatchMovesIgnoresSwapsThatOnlyWorkThroughASpecial() {
        val board = boardOf("RGBY", "YBGR").with(Pos(0, 1), special)

        assertEquals(emptyList(), findMatchMoves(board))
        assertTrue(findValidMoves(board).isNotEmpty())
    }

    @Test
    fun findMatchMovesKeepsAMatchBesideASpecial() {
        val board = boardOf("BRRBR").with(Pos(0, 0), Piece(Gem.WHITE, Special.BLAST))

        assertEquals(listOf(Move(Pos(0, 3), Pos(0, 4))), findMatchMoves(board))
    }
}
