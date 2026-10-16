package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BoardLayout
import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.Move
import dev.kesatria.gemmatch.core.Point
import dev.kesatria.gemmatch.core.Pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DragGestureTest {
    private val layout = BoardLayout(originX = 0f, originY = 0f, cellSize = 100f)
    private val game = Game.new(21L)
    private val scene = Scene().also { it.load(game.board) }
    private val drag = DragGesture(scene)

    /** The centre of a cell, in pixels. */
    private fun centre(
        row: Int,
        col: Int,
    ) = Point(col * 100f + 50f, row * 100f + 50f)

    private fun sprite(
        row: Int,
        col: Int,
    ) = assertNotNull(scene[Pos(row, col)])

    @Test
    fun touchingAGemLiftsItAndDrawsItOnTop() {
        drag.begin(centre(3, 3), layout)

        assertTrue(drag.isActive)
        assertEquals(1.2f, sprite(3, 3).scale)
        assertSame(sprite(3, 3), scene.raised)
    }

    @Test
    fun aNewTouchWhileAGemIsStillHeldPutsTheFirstGemDown() {
        drag.begin(centre(3, 3), layout)
        drag.follow(Point(centre(3, 3).x + 40f, centre(3, 3).y), layout)

        // The finger-up never arrived (lost event), and another touch lands on a different gem.
        drag.begin(centre(5, 5), layout)

        assertEquals(1f, sprite(3, 3).scale)
        assertEquals(3f, sprite(3, 3).row)
        assertEquals(3f, sprite(3, 3).col)
        assertEquals(4f, sprite(3, 4).col)
        assertEquals(1.2f, sprite(5, 5).scale)
        assertSame(sprite(5, 5), scene.raised)
    }

    @Test
    fun noStartPointMeansNoGestureAndNothingLifted() {
        drag.begin(null, layout)

        assertFalse(drag.isActive)
        assertNull(scene.raised)
        assertNull(drag.release(centre(3, 3), layout))
        assertNull(drag.settle())
    }

    @Test
    fun aTouchOutsideTheGridStartsAGestureThatDoesNothing() {
        drag.begin(Point(-40f, -40f), layout)
        drag.follow(Point(60f, 60f), layout)

        assertTrue(drag.isActive, "the finger is down, so the board is not idle")
        assertNull(scene.raised)
        assertNull(drag.release(Point(60f, 60f), layout))
        assertNull(drag.settle())
    }

    @Test
    fun theHeldGemFollowsTheFingerAndItsNeighbourSlidesTheOtherWay() {
        drag.begin(centre(3, 3), layout)

        drag.follow(Point(centre(3, 3).x + 50f, centre(3, 3).y), layout)

        assertNear(3.5f, sprite(3, 3).col, "held gem column")
        assertNear(3f, sprite(3, 3).row, "held gem row")
        assertNear(3.5f, sprite(3, 4).col, "neighbour column (it moved the opposite way)")
        assertNear(3.22f, sprite(3, 4).row, "neighbour sidesteps at the half-way point")
    }

    @Test
    fun theGemMovesNoFurtherThanOneCellEvenIfTheFingerDoes() {
        drag.begin(centre(3, 3), layout)

        drag.follow(Point(centre(3, 3).x + 400f, centre(3, 3).y), layout)

        assertNear(4f, sprite(3, 3).col, "held gem column")
        assertNear(3f, sprite(3, 4).col, "neighbour column")
        assertNear(3f, sprite(3, 4).row, "neighbour row: no sidestep at the ends")
    }

    @Test
    fun draggingBackToWhereItStartedPutsBothGemsBack() {
        drag.begin(centre(3, 3), layout)
        drag.follow(Point(centre(3, 3).x + 40f, centre(3, 3).y), layout)

        drag.follow(centre(3, 3), layout)

        assertNear(3f, sprite(3, 3).col, "held gem column")
        assertNear(4f, sprite(3, 4).col, "neighbour column")
        assertNear(3f, sprite(3, 4).row, "neighbour row")
    }

    @Test
    fun switchingTheDragDirectionRestsTheFirstNeighbour() {
        drag.begin(centre(3, 3), layout)
        drag.follow(Point(centre(3, 3).x + 40f, centre(3, 3).y), layout)

        drag.follow(Point(centre(3, 3).x, centre(3, 3).y + 40f), layout)

        assertNear(4f, sprite(3, 4).col, "the right neighbour is back")
        assertNear(3f, sprite(3, 4).row, "the right neighbour is back")
        assertTrue(sprite(4, 3).row < 4f, "the lower neighbour is moving up")
    }

    @Test
    fun releasingShortOfThirtyFivePercentOfACellIsNotAMove() {
        drag.begin(centre(3, 3), layout)

        val move = drag.release(Point(centre(3, 3).x + 34f, centre(3, 3).y), layout)

        assertNull(move)
    }

    @Test
    fun releasingPastThirtyFivePercentSwapsWithTheNeighbourInThatDirection() {
        drag.begin(centre(3, 3), layout)

        val move = drag.release(Point(centre(3, 3).x + 36f, centre(3, 3).y), layout)

        assertEquals(Move(Pos(3, 3), Pos(3, 4)), move)
    }

    @Test
    fun theFourDirectionsPickTheRightNeighbour() {
        val cases =
            mapOf(
                Point(40f, 0f) to Pos(3, 4),
                Point(-40f, 0f) to Pos(3, 2),
                Point(0f, 40f) to Pos(4, 3),
                Point(0f, -40f) to Pos(2, 3),
            )
        for ((offset, expected) in cases) {
            val gesture = DragGesture(Scene().also { it.load(game.board) })
            gesture.begin(centre(3, 3), layout)

            val move = gesture.release(Point(centre(3, 3).x + offset.x, centre(3, 3).y + offset.y), layout)

            assertEquals(Move(Pos(3, 3), expected), move, "drag by $offset")
        }
    }

    @Test
    fun aTinyWobbleOrATapIsNotAMove() {
        drag.begin(centre(3, 3), layout)

        assertNull(drag.release(Point(centre(3, 3).x + 5f, centre(3, 3).y + 3f), layout))
    }

    @Test
    fun draggingOffTheEdgeOfTheBoardIsNotAMove() {
        drag.begin(centre(0, 0), layout)

        assertNull(drag.release(Point(centre(0, 0).x, centre(0, 0).y - 90f), layout))
    }

    @Test
    fun aDragThatCameBackBeforeReleaseIsNotAMove() {
        drag.begin(centre(3, 3), layout)
        drag.follow(Point(centre(3, 3).x + 80f, centre(3, 3).y), layout)

        assertNull(drag.release(Point(centre(3, 3).x + 10f, centre(3, 3).y), layout))
    }

    @Test
    fun letGoShortOfTheCommitPointTheGemsSpringBackToRest() {
        drag.begin(centre(3, 3), layout)
        drag.follow(Point(centre(3, 3).x + 30f, centre(3, 3).y), layout)
        assertNull(drag.release(Point(centre(3, 3).x + 30f, centre(3, 3).y), layout))

        val settle = assertNotNull(drag.settle())
        playToEnd(Timeline(listOf(settle)), scene)
        drag.clear()
        scene.raised = null // the controller does this when the timeline finishes

        assertSceneShows(game.board, scene)
        assertFalse(drag.isActive)
    }

    @Test
    fun aClearedGestureIsInactiveAndHasNothingToSettle() {
        drag.begin(centre(3, 3), layout)
        drag.clear()

        assertFalse(drag.isActive)
        assertNull(drag.settle())
    }
}
