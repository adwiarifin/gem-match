package dev.kesatria.gemmatch.core

/**
 * The clocks of a run, in milliseconds. [startedMs] is the epoch time the run began and [activeMs] the time the app
 * was in the foreground. `core` never reads a clock; the app passes both in.
 */
data class RunTime(
    val startedMs: Long,
    val activeMs: Long,
)
