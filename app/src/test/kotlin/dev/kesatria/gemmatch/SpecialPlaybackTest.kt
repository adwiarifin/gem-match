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

    /** The same diagonal board with the two-character [overrides] written over it, for example `3B` or `-W`. */
    private fun gameWithCells(vararg overrides: Pair<Pair<Int, Int>, String>): Game {
        val changed = overrides.toMap()
        val cells =
            (0 until BOARD_SIZE).joinToString("") { row ->
                (0 until BOARD_SIZE).joinToString("") { col -> changed[row to col] ?: "${(row + col) % 3}-" }
            }
        return assertNotNull(Game.decode("GM2;0;1;$cells"))
    }

    private fun swap(
        game: Game,
        from: Pos,
        to: Pos,
    ) = assertIs<SwapOutcome.Accepted>(game.trySwap(from, to))

    @Test
    fun anEffectShowsWhilePlayingAndIsGoneAtTheEnd() {
        val game = gameWithCells((4 to 4) to "3B")
        val outcome = swap(game, Pos(4, 4), Pos(4, 5))
        val scene = Scene().also { it.load(game.board) }
        val timeline = Timeline.forSteps(outcome.steps) { }

        var seen = 0
        while (!timeline.isFinished) {
            timeline.advance(1f / 60, scene)
            seen = maxOf(seen, scene.effects.size)
        }

        assertEquals(1, seen)
        assertTrue(scene.effects.isEmpty())
    }

    @Test
    fun chainedEffectsStartInStepOrderAndOverlap() {
        val game = gameWithCells((4 to 4) to "3B", (3 to 5) to "4R", (5 to 6) to "5X")
        val outcome = swap(game, Pos(4, 4), Pos(4, 5))
        val fired = outcome.steps.filterIsInstance<Step.Activate>()
        val scene = Scene().also { it.load(game.board) }
        val timeline = Timeline.forSteps(outcome.steps) { }

        val started = mutableListOf<Effect>()
        var most = 0
        while (!timeline.isFinished) {
            timeline.advance(1f / 60, scene)
            scene.effects.filter { it !in started }.forEach { started += it }
            most = maxOf(most, scene.effects.size)
        }

        assertEquals(fired.map { it.origin }, started.map { it.origin })
        assertTrue(fired.size >= 3)
        assertTrue(most > 1, "chained effects overlap")
        assertSceneShows(outcome.game.board, scene, "after the chain")
    }

    @Test
    fun aLongChainTakesLittleLongerThanItsFirstEffect() {
        val origin = Pos(4, 4)
        val one = ActivateAnimation(listOf(Step.Activate(origin, Special.BLAST, setOf(origin))))
        val ten = ActivateAnimation(List(10) { Step.Activate(origin, Special.BLAST, setOf(origin)) })

        assertTrue(ten.seconds > one.seconds)
        assertTrue(ten.seconds <= one.seconds + ActivateAnimation.CHAIN_SPREAD + 0.0001f, "was ${ten.seconds}")
    }

    @Test
    fun anActivateRunInOneClearIsOneAnimationAndTheNextClearStartsFresh() {
        val a = Pos(1, 1)
        val steps =
            listOf(
                Step.Activate(a, Special.BLAST, setOf(a)),
                Step.Activate(a, Special.BLAST, setOf(a)),
                Step.Clear(setOf(a), 10, 1),
                Step.Activate(a, Special.BLAST, setOf(a)),
                Step.Clear(setOf(a), 10, 2),
            )

        val timeline = Timeline.forSteps(steps) { }
        val scene = Scene()
        var effectsAtOnce = 0
        while (!timeline.isFinished) {
            timeline.advance(1f / 60, scene)
            effectsAtOnce = maxOf(effectsAtOnce, scene.effects.size)
        }

        assertEquals(2, effectsAtOnce)
    }

    @Test
    fun aTapSkipsToTheEndOfTheMove() {
        val game = gameWithCells((4 to 4) to "3B", (3 to 5) to "4R", (5 to 6) to "5X")
        val outcome = swap(game, Pos(4, 4), Pos(4, 5))
        val scene = Scene().also { it.load(game.board) }
        val timeline = Timeline.forSteps(outcome.steps) { }
        timeline.advance(0.1f, scene)

        timeline.skip(scene)

        assertTrue(timeline.isFinished)
        assertTrue(scene.effects.isEmpty())
    }

    @Test
    fun aGemTheColourGemConvertsShowsItsNewKindWhenItsEffectStarts() {
        val game = gameWithCells((4 to 4) to "-W", (4 to 5) to "3B", (0 to 0) to "3-", (6 to 6) to "3-")
        val outcome = swap(game, Pos(4, 4), Pos(4, 5))
        val scene = Scene().also { it.load(game.board) }
        val timeline = Timeline.forSteps(outcome.steps) { }
        assertEquals(null, scene[Pos(0, 0)]?.piece?.special)

        var converted = false
        while (!timeline.isFinished) {
            timeline.advance(1f / 60, scene)
            if (scene[Pos(0, 0)]?.piece?.special == Special.BLAST) converted = true
        }

        assertTrue(converted)
    }

    @Test
    fun theClockWrapIsAWholeNumberOfShimmerPeriods() {
        val periods = CLOCK_WRAP_SECONDS / SHIMMER_SECONDS

        assertNear(Math.round(periods).toFloat(), periods, "periods per wrap")
    }

    @Test
    fun colourWithColourEndsOnTheCoreBoard() {
        val game = gameWithCells((4 to 4) to "-W", (4 to 5) to "-W")
        val outcome = swap(game, Pos(4, 4), Pos(4, 5))
        val scene = Scene().also { it.load(game.board) }

        playToEnd(Timeline.forSteps(outcome.steps) { }, scene)

        assertSceneShows(outcome.game.board, scene, "after colour and colour")
        assertTrue(scene.effects.isEmpty())
    }

    @Test
    fun colourWithASpecialEndsOnTheCoreBoard() {
        val game = gameWithCells((4 to 4) to "-W", (4 to 5) to "3B", (0 to 0) to "3-", (6 to 6) to "3-")
        val outcome = swap(game, Pos(4, 4), Pos(4, 5))
        val scene = Scene().also { it.load(game.board) }

        playToEnd(Timeline.forSteps(outcome.steps) { }, scene)

        assertSceneShows(outcome.game.board, scene, "after colour and blast")
    }

    @Test
    fun aNewGameDropsAnyEffectStillPlaying() {
        val game = gameWithCells((4 to 4) to "3B")
        val outcome = swap(game, Pos(4, 4), Pos(4, 5))
        val scene = Scene().also { it.load(game.board) }
        val timeline = Timeline.forSteps(outcome.steps) { }
        timeline.advance(0.2f, scene)
        assertTrue(scene.effects.isNotEmpty())

        scene.load(game.board)

        assertTrue(scene.effects.isEmpty())
    }
}
