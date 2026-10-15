package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.Board
import dev.kesatria.gemmatch.core.BoardLayout
import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.Gem
import dev.kesatria.gemmatch.core.Point
import dev.kesatria.gemmatch.core.Pos
import dev.kesatria.gemmatch.core.findValidMoves
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IdleHintTest {
    private val board = Game.new(31L).board
    private val scene = Scene().also { it.load(board) }
    private val hint =
        IdleHint(scene).also {
            it.touched()
            it.tick(0f, board, settled = true)
        }

    private fun pulsing(): Set<Pos> {
        val found = mutableSetOf<Pos>()
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                if (scene[Pos(row, col)]!!.scale != 1f) found += Pos(row, col)
            }
        }
        return found
    }

    @Test
    fun nothingPulsesBeforeEightSeconds() {
        hint.tick(7.9f, board, settled = true)

        assertTrue(pulsing().isEmpty())
    }

    @Test
    fun afterEightSecondsExactlyOneValidSwapPulses() {
        hint.tick(8.5f, board, settled = true)

        val pair = pulsing()
        assertEquals(2, pair.size)
        val validPairs = findValidMoves(board).map { setOf(it.from, it.to) }
        assertTrue(pair in validPairs, "the pulsing gems $pair must be a valid move")
    }

    @Test
    fun thePulseGrowsToTwentyPercentAtItsPeakAndIsBackToNormalAfterOneSecond() {
        hint.tick(8.5f, board, settled = true)
        val pair = pulsing()
        pair.forEach { assertNear(1.2f, scene[it]!!.scale, "scale at the peak of the pulse") }

        hint.tick(0.5f, board, settled = true)

        pair.forEach { assertNear(1f, scene[it]!!.scale, "scale after one full pulse") }
    }

    @Test
    fun theSamePairKeepsPulsingUntilReset() {
        hint.tick(8.5f, board, settled = true)
        val first = pulsing()

        repeat(30) { hint.tick(0.1f, board, settled = true) }

        assertEquals(first, pulsing())
    }

    @Test
    fun resetStopsThePulseAndTheEightSecondsStartAgain() {
        hint.tick(8.5f, board, settled = true)
        assertTrue(pulsing().isNotEmpty())

        hint.touched()
        hint.tick(0f, board, settled = true)
        assertTrue(pulsing().isEmpty(), "reset puts the gems back at normal size")

        hint.tick(7.9f, board, settled = true)
        assertTrue(pulsing().isEmpty(), "the timer restarted from zero")
        hint.tick(0.2f, board, settled = true)
        assertEquals(2, pulsing().size)
    }

    @Test
    fun aBoardWithNoValidMoveShowsNoHintAndDoesNotCrash() {
        var dead = Board.empty()
        val diagonal = listOf(Gem.RED, Gem.GREEN, Gem.BLUE)
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) dead = dead.with(Pos(row, col), diagonal[(row + col) % 3])
        }
        assertTrue(findValidMoves(dead).isEmpty(), "the fixture must really be dead")
        val deadScene = Scene().also { it.load(dead) }

        IdleHint(deadScene).also { it.touched() }.run {
            tick(0f, dead, settled = true)
            tick(20f, dead, settled = true)
        }

        deadScene.forEach { assertEquals(1f, it.scale) }
    }

    @Test
    fun aTouchThatCameAndWentWithinOneFrameStillRestartsTheTimer() {
        hint.tick(8.5f, board, settled = true)
        assertTrue(pulsing().isNotEmpty())

        hint.touched()
        hint.tick(1f / 60, board, settled = true)

        assertTrue(pulsing().isEmpty(), "the hint is gone even though the finger was already up")
        hint.tick(7.9f, board, settled = true)
        assertTrue(pulsing().isEmpty(), "and the eight seconds start again from the touch")
        hint.tick(0.2f, board, settled = true)
        assertEquals(2, pulsing().size)
    }

    @Test
    fun timeSpentOnAnUnsettledBoardNeverCountsTowardTheHint() {
        hint.tick(7.9f, board, settled = true)

        hint.tick(1f, board, settled = false)
        hint.tick(7.9f, board, settled = true)

        assertTrue(pulsing().isEmpty())
    }

    @Test
    fun anUnsettledBoardShowsNoHintEvenAfterALongWait() {
        hint.tick(8.5f, board, settled = true)

        hint.tick(30f, board, settled = false)

        assertTrue(pulsing().isEmpty())
    }

    @Test
    fun touchingAGemWhileItPulsesAsTheHintStillLiftsIt() {
        val layout = BoardLayout(originX = 0f, originY = 0f, cellSize = 100f)
        val drag = DragGesture(scene)
        hint.tick(8.5f, board, settled = true)
        val held = pulsing().first()

        // What the controller does in one frame: the touch begins a drag, then the idle hint ticks.
        hint.touched()
        drag.begin(Point(held.col * 100f + 50f, held.row * 100f + 50f), layout)
        hint.tick(1f / 60, board, settled = !drag.isActive)

        assertEquals(1.2f, scene[held]!!.scale, "the held gem keeps its lift")
        assertTrue(pulsing() == setOf(held), "the other hint gem goes back to normal")
    }
}
