package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReshuffleTest {
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

    private companion object {
        const val SEEDS = 1000L
    }
}
