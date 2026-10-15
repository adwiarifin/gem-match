package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SwipeTest {
    /** 100-pixel cells with the board's top-left corner at (50, 200). */
    private val layout = BoardLayout(originX = 50f, originY = 200f, cellSize = 100f)

    private fun centreX(col: Int) = 50f + col * 100f + 50f

    private fun centreY(row: Int) = 200f + row * 100f + 50f

    private fun swipe(
        fromRow: Int,
        fromCol: Int,
        dx: Float,
        dy: Float,
    ): Move? =
        resolveSwipe(
            layout,
            down = Point(centreX(fromCol), centreY(fromRow)),
            current = Point(centreX(fromCol) + dx, centreY(fromRow) + dy),
        )

    @Test
    fun cellAtFindsTheCellUnderAPoint() {
        assertEquals(Pos(0, 0), layout.cellAt(60f, 210f))
        assertEquals(Pos(2, 3), layout.cellAt(centreX(3), centreY(2)))
        assertEquals(Pos(7, 7), layout.cellAt(centreX(7), centreY(7)))
    }

    @Test
    fun cellAtTreatsTheTopLeftEdgeAsInsideAndTheFarEdgeAsOutside() {
        assertEquals(Pos(0, 0), layout.cellAt(50f, 200f))
        assertNull(layout.cellAt(50f + BOARD_SIZE * 100f, 250f))
        assertNull(layout.cellAt(100f, 200f + BOARD_SIZE * 100f))
    }

    @Test
    fun cellAtIsNullOutsideTheBoard() {
        assertNull(layout.cellAt(49f, 250f))
        assertNull(layout.cellAt(100f, 199f))
        assertNull(layout.cellAt(-500f, -500f))
    }

    @Test
    fun swipingInEachDirectionPicksTheNeighbour() {
        assertEquals(Move(Pos(3, 3), Pos(3, 4)), swipe(3, 3, dx = 50f, dy = 0f))
        assertEquals(Move(Pos(3, 3), Pos(3, 2)), swipe(3, 3, dx = -50f, dy = 0f))
        assertEquals(Move(Pos(3, 3), Pos(2, 3)), swipe(3, 3, dx = 0f, dy = -50f))
        assertEquals(Move(Pos(3, 3), Pos(4, 3)), swipe(3, 3, dx = 0f, dy = 50f))
    }

    @Test
    fun dragBelowTheThresholdDoesNothingYet() {
        assertNull(swipe(3, 3, dx = 29f, dy = 0f))
        assertNull(swipe(3, 3, dx = 0f, dy = -20f))
    }

    @Test
    fun dragJustPastTheThresholdCounts() {
        assertNotNull(swipe(3, 3, dx = 36f, dy = 0f))
    }

    @Test
    fun theLongerAxisWins() {
        assertEquals(Move(Pos(3, 3), Pos(3, 4)), swipe(3, 3, dx = 40f, dy = 20f))
        assertEquals(Move(Pos(3, 3), Pos(2, 3)), swipe(3, 3, dx = 20f, dy = -40f))
    }

    @Test
    fun aPerfectDiagonalIsAmbiguousSoNothingHappens() {
        assertNull(swipe(3, 3, dx = 40f, dy = 40f))
    }

    @Test
    fun touchingOutsideTheBoardDoesNothing() {
        assertNull(resolveSwipe(layout, down = Point(10f, 10f), current = Point(200f, 10f)))
    }

    @Test
    fun swipingTowardTheBoardEdgeIsIgnored() {
        assertNull(swipe(0, 3, dx = 0f, dy = -60f))
        assertNull(swipe(BOARD_SIZE - 1, 3, dx = 0f, dy = 60f))
        assertNull(swipe(3, 0, dx = -60f, dy = 0f))
        assertNull(swipe(3, BOARD_SIZE - 1, dx = 60f, dy = 0f))
    }

    @Test
    fun aLongDragStillMovesOnlyOneCell() {
        assertEquals(Move(Pos(3, 3), Pos(3, 4)), swipe(3, 3, dx = 450f, dy = 10f))
    }

    private fun preview(
        fromRow: Int,
        fromCol: Int,
        dx: Float,
        dy: Float,
    ): DragPreview? =
        previewDrag(
            layout,
            down = Point(centreX(fromCol), centreY(fromRow)),
            current = Point(centreX(fromCol) + dx, centreY(fromRow) + dy),
        )

    @Test
    fun previewShowsTheNeighbourAndHowFarTheDragHasGone() {
        val half = preview(3, 3, dx = 50f, dy = 0f)

        assertEquals(Pos(3, 3), half?.from)
        assertEquals(Pos(3, 4), half?.to)
        assertEquals(0.5f, half?.progress)
    }

    @Test
    fun previewProgressIsCappedAtOneCell() {
        assertEquals(1f, preview(3, 3, dx = 0f, dy = 250f)?.progress)
        assertEquals(Pos(4, 3), preview(3, 3, dx = 0f, dy = 250f)?.to)
    }

    @Test
    fun previewFollowsEachDirection() {
        assertEquals(Pos(3, 2), preview(3, 3, dx = -40f, dy = 0f)?.to)
        assertEquals(Pos(2, 3), preview(3, 3, dx = 0f, dy = -40f)?.to)
    }

    @Test
    fun tinyMovementShowsNoNeighbourYet() {
        val preview = preview(3, 3, dx = 5f, dy = 3f)

        assertEquals(Pos(3, 3), preview?.from)
        assertNull(preview?.to)
        assertEquals(0f, preview?.progress)
    }

    @Test
    fun draggingBackToTheStartCancelsThePreview() {
        assertNull(preview(3, 3, dx = 4f, dy = 0f)?.to)
    }

    @Test
    fun previewIgnoresADiagonalTieAndTheBoardEdge() {
        assertNull(preview(3, 3, dx = 40f, dy = 40f)?.to)
        assertNull(preview(0, 3, dx = 0f, dy = -60f)?.to)
        assertNull(preview(3, BOARD_SIZE - 1, dx = 60f, dy = 0f)?.to)
    }

    @Test
    fun previewIsNullWhenTheTouchStartsOffTheBoard() {
        assertNull(previewDrag(layout, down = Point(10f, 10f), current = Point(200f, 10f)))
    }

    @Test
    fun cellSizeMustBePositive() {
        assertFailsWith<IllegalArgumentException> { BoardLayout(0f, 0f, 0f) }
        assertFailsWith<IllegalArgumentException> { BoardLayout(0f, 0f, -10f) }
    }

    @Test
    fun anyResultIsAnInBoundsAdjacentPair() {
        val rng = Rng(77L)
        val span = BOARD_SIZE * 100f

        fun randomPoint() =
            Point(
                x = 50f + (rng.nextInt(2000) - 500) / 1000f * span,
                y = 200f + (rng.nextInt(2000) - 500) / 1000f * span,
            )

        val moves = (1..2000).mapNotNull { resolveSwipe(layout, down = randomPoint(), current = randomPoint()) }

        assertTrue(moves.size > 100, "too few swipes resolved: ${moves.size}")
        moves.forEach {
            assertTrue(it.from.isInBounds() && it.to.isInBounds(), "$it")
            assertTrue(it.from.isAdjacentTo(it.to), "$it")
        }
    }
}
