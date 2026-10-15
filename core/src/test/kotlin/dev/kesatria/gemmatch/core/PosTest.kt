package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PosTest {
    @Test
    fun orthogonalNeighboursAreAdjacent() {
        val p = Pos(3, 3)

        assertTrue(p.isAdjacentTo(Pos(2, 3)))
        assertTrue(p.isAdjacentTo(Pos(4, 3)))
        assertTrue(p.isAdjacentTo(Pos(3, 2)))
        assertTrue(p.isAdjacentTo(Pos(3, 4)))
    }

    @Test
    fun diagonalSamePositionAndDistanceTwoAreNotAdjacent() {
        val p = Pos(3, 3)

        assertFalse(p.isAdjacentTo(Pos(2, 2)))
        assertFalse(p.isAdjacentTo(Pos(3, 3)))
        assertFalse(p.isAdjacentTo(Pos(3, 5)))
    }

    @Test
    fun cornersAreInBounds() {
        assertTrue(Pos(0, 0).isInBounds())
        assertTrue(Pos(BOARD_SIZE - 1, BOARD_SIZE - 1).isInBounds())
    }

    @Test
    fun positionsOutsideTheBoardAreNotInBounds() {
        assertFalse(Pos(-1, 0).isInBounds())
        assertFalse(Pos(0, -1).isInBounds())
        assertFalse(Pos(BOARD_SIZE, 0).isInBounds())
        assertFalse(Pos(0, BOARD_SIZE).isInBounds())
    }
}
