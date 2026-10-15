package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReshuffleTest {
    private val deadBoard =
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

    private fun gemCounts(board: Board): Map<Gem, Int> =
        (0 until BOARD_SIZE)
            .flatMap { row -> (0 until BOARD_SIZE).map { col -> board[Pos(row, col)] } }
            .filterNotNull()
            .groupingBy { it }
            .eachCount()

    @Test
    fun generatedBoardsAreFullStableAndPlayable() {
        for (seed in 1L..SEEDS) {
            val board = generateBoard(Rng(seed))

            assertTrue(isFull(board), "seed $seed")
            assertTrue(findRuns(board).isEmpty(), "seed $seed")
            assertTrue(hasValidMove(board), "seed $seed")
        }
    }

    @Test
    fun sameSeedGeneratesTheSameBoard() {
        assertEquals(generateBoard(Rng(9L)), generateBoard(Rng(9L)))
    }

    @Test
    fun boardThatStillHasAMoveIsLeftAlone() {
        val board = randomStableBoard(1L).let { b -> if (hasValidMove(b)) b else randomStableBoard(2L) }

        val (result, step) = ensureMoveExists(board, Rng(1L))

        assertEquals(board, result)
        assertNull(step)
    }

    @Test
    fun deadBoardIsReshuffledIntoAPlayableOne() {
        val (result, step) = ensureMoveExists(deadBoard, Rng(1L))

        assertNotNull(step)
        assertEquals(result, step.board)
        assertTrue(isFull(result))
        assertTrue(findRuns(result).isEmpty())
        assertTrue(hasValidMove(result))
        assertEquals(gemCounts(deadBoard), gemCounts(result))
    }

    @Test
    fun reshuffleIsDeterministic() {
        assertEquals(ensureMoveExists(deadBoard, Rng(4L)), ensureMoveExists(deadBoard, Rng(4L)))
    }

    @Test
    fun boardThatCanNeverBePlayableStillEndsUpPlayable() {
        val allRed = (0 until BOARD_SIZE).map { "RRRRRRRR" }.toTypedArray()

        val (result, step) = ensureMoveExists(boardOf(*allRed), Rng(1L))

        assertNotNull(step)
        assertTrue(isFull(result))
        assertTrue(findRuns(result).isEmpty())
        assertTrue(hasValidMove(result))
    }

    private companion object {
        const val SEEDS = 1000L
    }
}
