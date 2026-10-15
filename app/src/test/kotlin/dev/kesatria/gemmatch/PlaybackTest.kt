package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.Step
import dev.kesatria.gemmatch.core.SwapOutcome
import dev.kesatria.gemmatch.core.findValidMoves
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

/** The JVM version of the on-device check: the scene always ends up showing exactly the core's board. */
class PlaybackTest {
    @Test
    fun theSceneEndsOnTheResolvedBoardAfterEveryMoveOfManyGames() {
        val seen = mutableSetOf<KClass<out Step>>()
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
                outcome.steps.forEach { seen += it::class }
                game = outcome.game
            }
        }
        assertEquals(
            setOf(Step.Swap::class, Step.Clear::class, Step.Fall::class, Step.Spawn::class, Step.Reshuffle::class),
            seen,
            "every kind of step must be played at least once",
        )
    }
}
