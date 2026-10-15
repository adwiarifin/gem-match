package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.Board
import dev.kesatria.gemmatch.core.Move
import dev.kesatria.gemmatch.core.findValidMoves
import kotlin.math.PI
import kotlin.math.cos

/**
 * After [HINT_AFTER_SECONDS] of a settled board with no touch, one valid move pulses. The timer counts
 * game-loop time only, so it never runs in the background. Anything that happens (a touch, the app coming
 * back) calls [touched], and the next [tick] clears the hint and restarts the timer.
 */
class IdleHint(
    private val scene: Scene,
) {
    private var idleSeconds = 0f
    private var hint: Move? = null

    /** Set from the UI thread when the app resumes, so it is volatile. Starts set: the first tick starts the timer. */
    @Volatile private var restartRequested = true

    /**
     * Something happened that restarts the timer. A request outlives the touch itself: a tap pressed and
     * released within one frame is seen only as "touched", never as a finger held down.
     */
    fun touched() {
        restartRequested = true
    }

    /**
     * Advances the idle timer by [seconds]. Only a [settled] board with no pending [touched] counts; otherwise the
     * hint is cleared and the timer restarts. Past the threshold, one valid move on [board] pulses.
     */
    fun tick(
        seconds: Float,
        board: Board,
        settled: Boolean,
    ) {
        if (!settled || restartRequested) {
            restart()
            restartRequested = false
        } else {
            count(seconds, board)
        }
    }

    private fun restart() {
        hint?.let { move ->
            scene[move.from]?.scale = 1f
            scene[move.to]?.scale = 1f
        }
        hint = null
        idleSeconds = 0f
    }

    private fun count(
        seconds: Float,
        board: Board,
    ) {
        idleSeconds += seconds
        if (idleSeconds < HINT_AFTER_SECONDS) return
        val move = hint ?: findValidMoves(board).randomOrNull()?.also { hint = it }
        if (move != null) {
            val phase = (idleSeconds - HINT_AFTER_SECONDS) / HINT_PULSE_SECONDS
            val scale = 1f + HINT_PULSE * (HALF - HALF * cos(2 * PI * phase).toFloat())
            scene[move.from]?.scale = scale
            scene[move.to]?.scale = scale
        }
    }

    private companion object {
        const val HINT_AFTER_SECONDS = 8f
        const val HINT_PULSE_SECONDS = 1f
        const val HINT_PULSE = 0.2f
        const val HALF = 0.5f
    }
}
