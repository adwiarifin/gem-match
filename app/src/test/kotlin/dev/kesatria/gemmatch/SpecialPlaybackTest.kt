package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.Gem
import dev.kesatria.gemmatch.core.GemMove
import dev.kesatria.gemmatch.core.Piece
import dev.kesatria.gemmatch.core.Pos
import dev.kesatria.gemmatch.core.Special
import dev.kesatria.gemmatch.core.Step
import dev.kesatria.gemmatch.core.SwapOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SpecialPlaybackTest {
    /** A saved game on the red, orange, yellow diagonal (no runs, no moves) with green gems placed by [greens]. */
    private fun gameWith(vararg greens: Pair<Int, Int>): Game {
        val cells =
            (0 until BOARD_SIZE).joinToString("") { row ->
                (0 until BOARD_SIZE).joinToString("") { col ->
                    val gem = if (row to col in greens) 3 else (row + col) % 3
                    "$gem-"
                }
            }
        return assertNotNull(Game.decode("GM2;0;1;$cells"))
    }

    // Green gems at (3,1), (3,2), (3,4) and one below the gap at (4,3): swapping it up makes a run of four.
    private fun fourRunGame() = gameWith(3 to 1, 3 to 2, 3 to 4, 4 to 3)

    private fun accepted(game: Game): SwapOutcome.Accepted {
        val outcome = game.trySwap(Pos(4, 3), Pos(3, 3))
        return assertIs<SwapOutcome.Accepted>(outcome)
    }

    @Test
    fun aCreatedSpecialPopsInOnTheClearedCell() {
        val scene = Scene().also { it.load(fourRunGame().board) }
        scene.remove(Pos(3, 3))
        val blast = Piece(Gem.GREEN, Special.BLAST)
        val timeline = Timeline(listOf(CreateAnimation(Pos(3, 3), blast)))

        timeline.advance(0.01f, scene)
        val popping = assertNotNull(scene[Pos(3, 3)])
        assertTrue(popping.scale < 1f, "starts small")

        playToEnd(timeline, scene)
        assertEquals(blast, scene[Pos(3, 3)]?.piece)
        assertNear(1f, scene[Pos(3, 3)]!!.scale, "full size at the end")
    }

    @Test
    fun aSpecialFallsAsAUnit() {
        val special = Piece(Gem.GREEN, Special.CROSS)
        val scene = Scene().also { it.load(fourRunGame().board) }
        scene.place(Pos(1, 0), Sprite(special, 1f, 0f))
        scene.remove(Pos(3, 0))
        val timeline = Timeline(listOf(FallAnimation(listOf(GemMove(special, Pos(1, 0), Pos(3, 0))))))

        playToEnd(timeline, scene)

        assertEquals(special, scene[Pos(3, 0)]?.piece)
    }

    @Test
    fun aRunOfFourPlaysToTheCoreBoardWithItsSpecialAndItSurvivesResume() {
        val outcome = accepted(fourRunGame())
        val scene = Scene().also { it.load(fourRunGame().board) }

        playToEnd(Timeline.forSteps(outcome.steps) { }, scene)

        assertSceneShows(outcome.game.board, scene, "after the move")
        val created = outcome.steps.filterIsInstance<Step.Create>().single()
        assertEquals(Special.BLAST, created.piece.special)
        val resumed = assertNotNull(Game.decode(outcome.game.encode()))
        val resumedScene = Scene().also { it.load(resumed.board) }
        assertSceneShows(outcome.game.board, resumedScene, "after save and resume")
    }
}
