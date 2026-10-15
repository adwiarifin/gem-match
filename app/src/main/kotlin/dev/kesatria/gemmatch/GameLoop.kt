package dev.kesatria.gemmatch

import android.graphics.Canvas
import android.view.SurfaceHolder

/**
 * Fixed-timestep loop: game logic advances in constant [STEP_SECONDS] slices however long a frame
 * takes, then one frame is drawn. Runs on its own thread from [start] until [stopAndJoin].
 */
class GameLoop(
    private val holder: SurfaceHolder,
    private val update: (Float) -> Unit,
    private val draw: (Canvas) -> Unit,
) : Thread("game-loop") {
    @Volatile private var running = true

    override fun run() {
        var previous = System.nanoTime()
        var accumulator = 0L
        while (running) {
            val frameStart = System.nanoTime()
            accumulator += minOf(frameStart - previous, MAX_FRAME_NANOS)
            previous = frameStart

            while (accumulator >= STEP_NANOS) {
                update(STEP_SECONDS)
                accumulator -= STEP_NANOS
            }

            val canvas = holder.lockCanvas()
            if (canvas != null) {
                try {
                    draw(canvas)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            sleepUntilNextFrame(frameStart)
        }
    }

    private fun sleepUntilNextFrame(frameStart: Long) {
        val spare = STEP_NANOS - (System.nanoTime() - frameStart)
        if (spare > 0) sleep(spare / NANOS_PER_MILLI, (spare % NANOS_PER_MILLI).toInt())
    }

    fun stopAndJoin() {
        running = false
        join()
    }

    private companion object {
        const val STEPS_PER_SECOND = 60
        const val NANOS_PER_SECOND = 1_000_000_000L
        const val NANOS_PER_MILLI = 1_000_000L
        const val STEP_NANOS = NANOS_PER_SECOND / STEPS_PER_SECOND
        const val STEP_SECONDS = 1f / STEPS_PER_SECOND

        /** After a stall (debugger, long pause) catch up at most this much, so logic never spirals. */
        const val MAX_FRAME_NANOS = 250_000_000L
    }
}
