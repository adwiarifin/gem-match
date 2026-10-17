package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.Pos
import dev.kesatria.gemmatch.core.Special
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EffectTest {
    private val everyCell = (0 until BOARD_SIZE).flatMap { r -> (0 until BOARD_SIZE).map { c -> Pos(r, c) } }.toSet()

    private fun fullX(origin: Pos) = Effect(origin, Special.X_FULL, everyCell)

    private fun column(col: Int) = (0 until BOARD_SIZE).map { Pos(it, col) }.toSet()

    private fun row(row: Int) = (0 until BOARD_SIZE).map { Pos(row, it) }.toSet()

    @Test
    fun aSingleLineBeamsAlongItsOwnRowOrColumn() {
        assertEquals(listOf(4), Effect(Pos(4, 4), Special.LINE_ROW, row(4)).beamRows)
        assertEquals(listOf(4), Effect(Pos(4, 4), Special.LINE_COL, column(4)).beamColumns)
        assertEquals(listOf(0), Effect(Pos(0, 3), Special.LINE_ROW, row(0)).beamRows)
    }

    @Test
    fun twoRowLinesBeamAlongThreeRowsAndTwoColumnLinesAlongThreeColumns() {
        val threeRows = (3..5).flatMap { row(it) }.toSet()
        val threeColumns = (3..5).flatMap { column(it) }.toSet()

        assertEquals(setOf(3, 4, 5), Effect(Pos(4, 4), Special.LINE_ROW, threeRows).beamRows.toSet())
        assertEquals(setOf(3, 4, 5), Effect(Pos(4, 4), Special.LINE_COL, threeColumns).beamColumns.toSet())
    }

    @Test
    fun aDoubledLineAtTheEdgeBeamsAlongTwoRowsBecauseTheThirdIsOffTheBoard() {
        val atTheEdge = (0..1).flatMap { row(it) }.toSet()

        assertEquals(setOf(0, 1), Effect(Pos(0, 3), Special.LINE_ROW, atTheEdge).beamRows.toSet())
    }

    @Test
    fun aCrossBeamsAlongItsOwnRowAndColumnOnly() {
        val cross = row(3) + column(3)
        val effect = Effect(Pos(3, 3), Special.CROSS, cross)

        assertEquals(listOf(3), effect.beamRows)
        assertEquals(listOf(3), effect.beamColumns)
    }

    @Test
    fun anEffectWithNoCellsStillHasAReachOfOne() {
        assertEquals(1, Effect(Pos(2, 2), Special.BLAST, emptySet()).reach)
    }

    @Test
    fun cellsAreKeptAsBoardIndexes() {
        val effect = Effect(Pos(1, 1), Special.BLAST, setOf(Pos(1, 1), Pos(2, 3)))

        assertEquals(setOf(1 * BOARD_SIZE + 1, 2 * BOARD_SIZE + 3), effect.cellIndexes.toSet())
    }

    @Test
    fun nothingFlashesBeforeTheEffectStarts() {
        val effect = fullX(Pos(3, 3))
        effect.progress = 0f

        assertTrue(effect.cellIndexes.indices.all { effect.flashAt(it) == 0f })
    }

    @Test
    fun theOriginFlashesBeforeTheFarthestCellDoes() {
        val effect = fullX(Pos(0, 0))
        effect.progress = 0.1f

        val slotOf = { pos: Pos -> effect.cellIndexes.indexOf(pos.row * BOARD_SIZE + pos.col) }
        assertTrue(effect.flashAt(slotOf(Pos(0, 0))) > 0f, "the origin is already lit")
        assertEquals(0f, effect.flashAt(slotOf(Pos(7, 7))), "the far corner is not reached yet")
    }

    @Test
    fun everyTakenCellFlashesAtSomePointAndIsDarkAgainAtTheEnd() {
        val effect = fullX(Pos(0, 0))
        val brightest = FloatArray(effect.cellIndexes.size)

        for (step in 0..100) {
            effect.progress = step / 100f
            effect.cellIndexes.indices.forEach { brightest[it] = maxOf(brightest[it], effect.flashAt(it)) }
        }

        assertTrue(brightest.all { it > 0.5f }, "some cell never lit up")
        effect.progress = 1f
        assertTrue(effect.cellIndexes.indices.all { effect.flashAt(it) < 0.01f })
    }
}

class ShimmerTest {
    @Test
    fun theGlowSwingsBetweenZeroAndOneOnceEveryShimmerPeriod() {
        assertNear(0.5f, shimmerGlow(0f), "at the start")
        assertNear(1f, shimmerGlow(0.6f), "a quarter of 2.4 s in")
        assertNear(0f, shimmerGlow(1.8f), "three quarters of 2.4 s in")
        assertNear(shimmerGlow(0.7f), shimmerGlow(0.7f + SHIMMER_SECONDS), "one period later")
    }
}
