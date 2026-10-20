package dev.kesatria.gemmatch

import kotlin.math.roundToLong

/**
 * The time a run has been played with the app in front: the sum of the game loop's frame times. The loop stops when
 * the surface goes away, so a pause, a phone call or a locked screen adds nothing, the same rule the idle hint uses.
 * The loop thread advances it and the UI thread reads it when the app pauses.
 */
class RunClock(
    activeMs: Long = 0L,
) {
    @Volatile private var seconds = activeMs / MS_PER_SECOND

    val activeMs: Long get() = (seconds * MS_PER_SECOND).roundToLong()

    fun advance(frameSeconds: Float) {
        if (frameSeconds > 0f) seconds += frameSeconds
    }

    fun reset() {
        seconds = 0.0
    }

    private companion object {
        const val MS_PER_SECOND = 1000.0
    }
}
