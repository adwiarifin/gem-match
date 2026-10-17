package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.Board
import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.Gem
import dev.kesatria.gemmatch.core.Piece
import dev.kesatria.gemmatch.core.Pos
import dev.kesatria.gemmatch.core.Rng
import dev.kesatria.gemmatch.core.Step
import dev.kesatria.gemmatch.core.SwapOutcome
import dev.kesatria.gemmatch.core.ensureMoveExists
import dev.kesatria.gemmatch.core.findValidMoves
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

/** The JVM version of the on-device check: the scene always ends up showing exactly the core's board. */
class PlaybackTest {
    @Test
    fun theSceneEndsOnTheResolvedBoardAfterEveryMoveOfManyGames() {
        val seen = mutableSetOf<KClass<out Step>>()
        val addedBySpecials = mutableSetOf<KClass<out Step>>()
        val original =
            setOf(
                Step.Swap::class,
                Step.Clear::class,
                Step.Fall::class,
                Step.Spawn::class,
                Step.Reshuffle::class,
            )
        for (seed in 1L..40L) {
            var game = Game.new(seed)
            val scene = Scene().also { it.load(game.board) }
            repeat(60) { turn ->
                val candidates = findValidMoves(game.board)
                val move = candidates[(turn * 7) % candidates.size]
                val outcome = game.trySwap(move.from, move.to) as SwapOutcome.Accepted
                var awarded = 0
                playToEnd(Timeline.forSteps(outcome.steps) { awarded += it }, scene)

                assertSceneShows(outcome.game.board, scene, "seed $seed move $turn")
                assertEquals(outcome.game.score - game.score, awarded, "seed $seed move $turn: points heard")
                outcome.steps.forEach { if (it::class in original) seen += it::class else addedBySpecials += it::class }
                game = outcome.game
            }
        }
        seen += playedReshuffle()
        assertEquals(
            setOf(Step.Swap::class, Step.Clear::class, Step.Fall::class, Step.Spawn::class, Step.Reshuffle::class),
            seen,
            "every kind of step must be played at least once",
        )
        assertEquals(
            setOf(Step.Activate::class, Step.Create::class),
            addedBySpecials,
            "and so must the steps that special gems add",
        )
    }

    /**
     * A board holding a special gem always has a move, so a reshuffle needs a dead board with none. Specials are now
     * made often enough that random play rarely gets there, so the reshuffle is played from a dead diagonal board.
     */
    private fun playedReshuffle(): KClass<out Step> {
        val diagonal = listOf(Gem.RED, Gem.GREEN, Gem.BLUE)
        var dead = Board.empty()
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                dead = dead.with(Pos(row, col), Piece(diagonal[(row + col) % diagonal.size]))
            }
        }
        val (rearranged, reshuffle) = ensureMoveExists(dead, Rng(5L))
        val scene = Scene().also { it.load(dead) }

        playToEnd(Timeline.forSteps(listOf(checkNotNull(reshuffle))) { }, scene)

        assertSceneShows(rearranged, scene, "after a reshuffle")
        return reshuffle::class
    }
}
