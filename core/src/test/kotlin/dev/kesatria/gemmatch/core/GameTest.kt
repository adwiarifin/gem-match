package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
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
    fun aGameAtTheScoreCeilingStaysThereInsteadOfWrappingNegative() {
        val fresh = Game.new(8L)
        val atCeiling = assertNotNull(Game.decode(fresh.encode().replace(";0;", ";${Int.MAX_VALUE};")))
        val move = findValidMoves(atCeiling.board).first()

        val outcome = atCeiling.trySwap(move.from, move.to) as SwapOutcome.Accepted

        assertEquals(Int.MAX_VALUE, outcome.game.score)
        assertNotNull(Game.decode(outcome.game.encode()), "the saved game must still load")
    }

    @Test
    fun aMoveLeavesTheOriginalGamesRandomStateAlone() {
        val game = Game.new(5L)
        val before = game.encode()
        val move = findValidMoves(game.board).first()

        game.trySwap(move.from, move.to)

        assertEquals(before, game.encode())
    }

    @Test
    fun tryingTheSameMoveTwiceGivesTheSameOutcome() {
        val game = Game.new(6L)
        val move = findValidMoves(game.board).first()

        val first = game.trySwap(move.from, move.to) as SwapOutcome.Accepted
        val second = game.trySwap(move.from, move.to) as SwapOutcome.Accepted

        assertEquals(first.game.encode(), second.game.encode())
        assertEquals(first.steps, second.steps)
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
    fun anAcceptedMoveLeavesAMoveUnlessTheGameIsOverAcrossLongPlay() {
        for (seed in 1L..SEEDS) {
            var game = Game.new(seed)
            val chooser = Rng(seed)
            for (turn in 0 until MOVES_PER_GAME) {
                if (game.isOver) break
                val moves = findValidMoves(game.board)
                val move = moves[chooser.nextInt(moves.size)]

                val outcome = game.trySwap(move.from, move.to)

                assertIs<SwapOutcome.Accepted>(outcome, "seed $seed turn $turn")
                assertEquals(outcome.game.board, replay(game.board, outcome.steps), "seed $seed turn $turn")
                if (!outcome.game.isOver) assertPlayable(outcome.game, "seed $seed turn $turn")
                assertTrue(outcome.game.score > game.score, "seed $seed turn $turn")
                game = outcome.game
            }
        }
    }

    @Test
    fun aNewGameIsNeverOver() {
        for (seed in 1L..SEEDS) assertFalse(Game.new(seed).isOver, "seed $seed")
    }

    @Test
    fun aBoardWithNoValidMoveIsOver() {
        assertTrue(gameOf(patterned()).isOver)
    }

    @Test
    fun aBoardHoldingASpecialIsNotOver() {
        for (special in Special.entries) {
            val piece = Piece(if (special == Special.COLOUR) null else Gem.GREEN, special)

            assertFalse(gameOf(patterned(Pos(3, 3) to piece)).isOver, "$special")
        }
    }

    @Test
    fun anOverGameRejectsEverySwap() {
        val over = gameOf(patterned())
        val neighbours =
            ALL_CELLS
                .flatMap { cell -> listOf(Pos(cell.row, cell.col + 1), Pos(cell.row + 1, cell.col)).map { cell to it } }
                .filter { (_, other) -> other.isInBounds() }

        for ((a, b) in neighbours) assertEquals(SwapOutcome.Rejected, over.trySwap(a, b), "$a $b")
        assertEquals(BOARD_SIZE * (BOARD_SIZE - 1) * 2, neighbours.size, "the loop must cover every adjacent pair")
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
