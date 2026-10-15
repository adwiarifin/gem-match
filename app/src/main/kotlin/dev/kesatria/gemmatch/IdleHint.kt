package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.Board
import dev.kesatria.gemmatch.core.Move
import dev.kesatria.gemmatch.core.findValidMoves
import kotlin.math.PI
import kotlin.math.cos

/**
 * After [HINT_AFTER_SECONDS] of a settled board with no touch, one valid move pulses. The timer counts
 * game-loop time only, so it never runs in the background. The controller calls [reset] whenever
 * anything happens (a touch, an animation, a dialog), which clears the hint and restarts the timer.
 */
class IdleHint(
    private val scene: Scene,
) {
    private var idleSeconds = 0f
    private var hint: Move? = null

    fun reset() {
        hint?.let { move ->
            scene[move.from]?.scale = 1f
            scene[move.to]?.scale = 1f
        }
        hint = null
        idleSeconds = 0f
    }

    /** Counts [seconds] of idle time and, once past the threshold, pulses a valid move on [board]. */
    fun update(
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
