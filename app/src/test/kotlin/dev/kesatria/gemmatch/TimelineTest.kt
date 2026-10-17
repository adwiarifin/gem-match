package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.GemMove
import dev.kesatria.gemmatch.core.Placed
import dev.kesatria.gemmatch.core.Pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TimelineTest {
    private val board = Game.new(41L).board
    private val scene = Scene().also { it.load(board) }
    private val a = Pos(2, 2)
    private val b = Pos(2, 3)

    private fun sprite(pos: Pos) = assertNotNull(scene[pos])

    @Test
    fun anEmptyTimelineIsAlreadyFinished() {
        assertTrue(Timeline(emptyList()).isFinished)
    }

    @Test
    fun timelineStaysUnfinishedUntilItsAnimationHasHadItsTime() {
        val timeline = Timeline(listOf(SwapAnimation(a, b)))

        timeline.advance(0.1f, scene)
        assertFalse(timeline.isFinished)

        timeline.advance(0.09f, scene)
        assertTrue(timeline.isFinished)
    }

    @Test
    fun timeLeftOverAtTheEndOfOneAnimationCarriesIntoTheNext() {
        val timeline = Timeline(listOf(SwapAnimation(a, b), SwapAnimation(a, b)))

        timeline.advance(0.25f, scene)
        assertFalse(timeline.isFinished, "0.25 s is 0.18 for the first plus 0.07 into the second")

        timeline.advance(0.12f, scene)
        assertTrue(timeline.isFinished)
    }

    @Test
    fun aVeryLongFrameFinishesEverythingInOneAdvance() {
        val timeline = Timeline(listOf(SwapAnimation(a, b), SwapAnimation(a, b), SwapAnimation(a, b)))

        timeline.advance(5f, scene)

        assertTrue(timeline.isFinished)
        assertEquals(board[b], sprite(a).piece, "three swaps leave the pair swapped")
    }

    @Test
    fun aSwapMeetsInTheMiddleThenTradesPlaces() {
        val gemA = board[a]
        val gemB = board[b]
        val timeline = Timeline(listOf(SwapAnimation(a, b)))

        timeline.advance(0.09f, scene)
        assertNear(2.5f, sprite(a).col, "first gem is half way")
        assertNear(2.5f, sprite(b).col, "second gem is half way")

        timeline.advance(0.2f, scene)
        assertEquals(gemB, sprite(a).piece)
        assertEquals(gemA, sprite(b).piece)
        assertNear(2f, sprite(a).col, "settled in the first cell")
        assertNear(3f, sprite(b).col, "settled in the second cell")
    }

    @Test
    fun aSwapStartedFromAHalfDraggedGemContinuesWithoutAJump() {
        sprite(a).col = 2.4f
        sprite(b).col = 3f - 0.4f
        val timeline = Timeline(listOf(SwapAnimation(a, b)))

        timeline.advance(0.001f, scene)

        assertNear(2.4f, sprite(a).col, "no jump at the start")
        assertNear(2.6f, sprite(b).col, "no jump at the start")
    }

    @Test
    fun aSettleBringsAHalfDraggedPairBackWithoutSwappingSlots() {
        val gemA = board[a]
        sprite(a).col = 2.4f
        sprite(a).scale = 1.2f
        sprite(b).col = 2.6f
        val timeline = Timeline(listOf(SettleAnimation(a, b)))

        timeline.advance(1f, scene)

        assertEquals(gemA, sprite(a).piece)
        assertNear(2f, sprite(a).col, "held gem back home")
        assertNear(1f, sprite(a).scale, "held gem back to normal size")
        assertNear(3f, sprite(b).col, "neighbour back home")
    }

    @Test
    fun aClearShrinksAndFadesThenRemovesTheGemsAndReportsThePointsOnce() {
        val cells = setOf(a, b)
        var heard = 0
        var calls = 0
        val timeline =
            Timeline(
                listOf(
                    ClearAnimation(cells, 60) {
                        heard += it
                        calls++
                    },
                ),
            )

        timeline.advance(0.1f, scene)
        assertNear(0.5f, sprite(a).scale, "half shrunk")
        assertNear(0.5f, sprite(a).alpha, "half faded")
        assertEquals(0, calls, "points are not awarded until the gems are gone")

        timeline.advance(0.2f, scene)
        assertNull(scene[a])
        assertNull(scene[b])
        assertEquals(1, calls)
        assertEquals(60, heard)
        assertNotNull(scene[Pos(0, 0)], "other gems are untouched")
    }

    @Test
    fun gemsFallTheirColumnAndLandInTheirNewSlots() {
        val gem = board[Pos(1, 0)]
        scene.remove(Pos(3, 0))
        val timeline = Timeline(listOf(FallAnimation(listOf(GemMove(gem!!, Pos(1, 0), Pos(3, 0))))))

        timeline.advance(0.1f, scene)
        val midRow = sprite(Pos(1, 0)).row
        assertTrue(midRow > 1f && midRow < 3f, "falling, now at row $midRow")

        timeline.advance(1f, scene)
        assertEquals(gem, sprite(Pos(3, 0)).piece)
        assertNear(3f, sprite(Pos(3, 0)).row, "landed")
        assertNull(scene[Pos(1, 0)])
    }

    @Test
    fun aLongerFallTakesLonger() {
        val short = FallAnimation(listOf(GemMove(board[Pos(0, 0)]!!, Pos(0, 0), Pos(1, 0))))
        val long = FallAnimation(listOf(GemMove(board[Pos(0, 0)]!!, Pos(0, 0), Pos(6, 0))))

        assertTrue(long.seconds > short.seconds)
    }

    @Test
    fun newGemsEnterFromAboveTheBoardAndLandOnTheirRows() {
        scene.remove(Pos(0, 5))
        scene.remove(Pos(1, 5))
        val arrivals = listOf(Placed(board[Pos(0, 5)]!!, Pos(0, 5)), Placed(board[Pos(1, 5)]!!, Pos(1, 5)))
        val timeline = Timeline(listOf(SpawnAnimation(arrivals)))

        timeline.advance(0.001f, scene)
        assertTrue(sprite(Pos(0, 5)).row < 0f, "the top gem starts above the board")

        timeline.advance(2f, scene)
        assertNear(0f, sprite(Pos(0, 5)).row, "landed in row 0")
        assertNear(1f, sprite(Pos(1, 5)).row, "landed in row 1")
        assertEquals(board[Pos(1, 5)], sprite(Pos(1, 5)).piece)
    }

    @Test
    fun aReshuffleAnnouncesItselfRattlesThenShowsTheNewBoard() {
        val rearranged = Game.new(42L).board
        val timeline = Timeline(listOf(ReshuffleAnimation(rearranged)))

        timeline.advance(0.4f, scene)
        assertEquals("No moves left - reshuffling", scene.message)
        assertTrue(scene.messageAlpha > 0.9f, "the notice has faded in")
        var rattling = false
        scene.forEach { if (it.dx != 0f) rattling = true }
        assertTrue(rattling, "the gems are shaking")

        timeline.advance(0.5f, scene)
        scene.forEach { assertTrue(it.scale < 1f, "shrinking away") }
        scene.forEach { assertEquals(0f, it.dx, "the shake has stopped") }

        timeline.advance(0.3f, scene)
        assertNull(scene.message, "the notice is gone once the new gems appear")

        timeline.advance(2f, scene)
        assertTrue(timeline.isFinished)
        assertSceneShows(rearranged, scene)
    }

    @Test
    fun aReshuffleTakesAboutOneAndAHalfSeconds() {
        assertNear(1.5f, ReshuffleAnimation(board).seconds, "total length")
    }

    @Test
    fun easingStartsAtZeroEndsAtOneAndNeverGoesBackwards() {
        val curves = listOf<(Float) -> Float>(Easing::inOut, Easing::out, Easing::fall)
        for (curve in curves) {
            assertNear(0f, curve(0f), "start")
            assertNear(1f, curve(1f), "end")
            var last = 0f
            for (step in 1..100) {
                val value = curve(step / 100f)
                assertTrue(value >= last, "curve went backwards at step $step")
                last = value
            }
        }
    }
}
