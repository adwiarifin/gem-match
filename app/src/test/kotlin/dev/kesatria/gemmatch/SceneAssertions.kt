package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.Board
import dev.kesatria.gemmatch.core.Pos
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val TOLERANCE = 1e-4f

/** Fails unless every cell of [scene] shows exactly the gem of [board], at rest and at normal size. */
fun assertSceneShows(
    board: Board,
    scene: Scene,
    context: String = "",
) {
    assertNull(scene.raised, "$context: nothing should be held")
    assertNull(scene.message, "$context: no notice should be showing")
    for (row in 0 until BOARD_SIZE) {
        for (col in 0 until BOARD_SIZE) {
            val where = "$context: cell ($row, $col)"
            val sprite = assertNotNull(scene[Pos(row, col)], "$where has no sprite")
            assertEquals(board[Pos(row, col)], sprite.gem, "$where shows the wrong gem")
            assertNear(row.toFloat(), sprite.row, "$where row")
            assertNear(col.toFloat(), sprite.col, "$where column")
            assertNear(1f, sprite.scale, "$where scale")
            assertNear(1f, sprite.alpha, "$where alpha")
            assertNear(0f, sprite.dx, "$where sideways offset")
        }
    }
}

fun assertNear(
    expected: Float,
    actual: Float,
    message: String,
) = assertTrue(abs(expected - actual) < TOLERANCE, "$message: expected $expected but was $actual")

private val FRAME_SECONDS = floatArrayOf(1f / 60, 1f / 90, 1f / 30, 0.25f)

/** Advances [timeline] with uneven frame times until it finishes; a timeline that never ends fails, not hangs. */
fun playToEnd(
    timeline: Timeline,
    scene: Scene,
) {
    var frames = 0
    while (!timeline.isFinished) {
        check(frames < MAX_FRAMES) { "timeline still running after $MAX_FRAMES frames" }
        timeline.advance(FRAME_SECONDS[frames % FRAME_SECONDS.size], scene)
        frames++
    }
}

private const val MAX_FRAMES = 5_000
