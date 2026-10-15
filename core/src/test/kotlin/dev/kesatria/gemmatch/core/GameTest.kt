package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameTest {
    private fun assertPlayable(
        game: Game,
        message: String,
    ) {
        assertTrue(isFull(game.board), message)
        assertTrue(findRuns(game.board).isEmpty(), message)
        assertTrue(hasValidMove(game.board), message)
    }

    @Test
    fun newGameStartsPlayableWithZeroScore() {
        for (seed in 1L..300L) {
            val game = Game.new(seed)

            assertPlayable(game, "seed $seed")
            assertEquals(0, game.score, "seed $seed")
        }
    }

    @Test
    fun sameSeedGivesTheSameGame() {
        assertEquals(Game.new(11L).board, Game.new(11L).board)
    }

    @Test
    fun invalidSwapIsRejectedAndChangesNothing() {
        val game = Game.new(3L)
        val boardBefore = game.board

        val outcome = game.trySwap(Pos(0, 0), Pos(3, 3))

        assertEquals(SwapOutcome.Rejected, outcome)
        assertEquals(boardBefore, game.board)
        assertEquals(0, game.score)
    }

    @Test
    fun validSwapScoresAndLeavesAPlayableBoard() {
        val game = Game.new(5L)
        val move = findValidMoves(game.board).first()

        val outcome = game.trySwap(move.from, move.to)

        assertIs<SwapOutcome.Accepted>(outcome)
        val clearPoints = outcome.steps.filterIsInstance<Step.Clear>().sumOf { it.points }
        assertTrue(clearPoints > 0)
        assertEquals(clearPoints, outcome.game.score)
        assertPlayable(outcome.game, "after first move")
        assertEquals(outcome.game.board, replay(game.board, outcome.steps))
    }

    @Test
    fun theOriginalGameIsNotMutatedByAMove() {
        val game = Game.new(5L)
        val boardBefore = game.board
        val move = findValidMoves(game.board).first()

        game.trySwap(move.from, move.to)

        assertEquals(boardBefore, game.board)
        assertEquals(0, game.score)
    }

    @Test
    fun anAcceptedMoveAlwaysLeavesAMoveAcrossLongPlay() {
        for (seed in 1L..SEEDS) {
            var game = Game.new(seed)
            val chooser = Rng(seed)
            repeat(MOVES_PER_GAME) { turn ->
                val moves = findValidMoves(game.board)
                val move = moves[chooser.nextInt(moves.size)]

                val outcome = game.trySwap(move.from, move.to)

                assertIs<SwapOutcome.Accepted>(outcome, "seed $seed turn $turn")
                assertEquals(outcome.game.board, replay(game.board, outcome.steps), "seed $seed turn $turn")
                assertPlayable(outcome.game, "seed $seed turn $turn")
                assertTrue(outcome.game.score > game.score, "seed $seed turn $turn")
                game = outcome.game
            }
        }
    }

    @Test
    fun samePlayFromTheSameSeedGivesTheSameFuture() {
        fun play(seed: Long): Game {
            var game = Game.new(seed)
            repeat(10) {
                val move = findValidMoves(game.board).first()
                game = (game.trySwap(move.from, move.to) as SwapOutcome.Accepted).game
            }
            return game
        }

        val a = play(21L)
        val b = play(21L)

        assertEquals(a.board, b.board)
        assertEquals(a.score, b.score)
        assertEquals(a.rngState, b.rngState)
    }

    private companion object {
        const val SEEDS = 200L
        const val MOVES_PER_GAME = 30
    }
}
