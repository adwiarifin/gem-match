package dev.kesatria.gemmatch.core

private const val RUN_VERSION = "RN1"
private const val SEPARATOR = ";"
private const val RUN_PART_COUNT = 2
private const val MAX_RUN_LENGTH = 1024
private const val SCORE = "score"
private const val WALL = "wall"
private const val ACTIVE = "active"
private const val STARTED = "started"
private const val ENDED = "ended"
private const val BEST = "best"

/** How many finished runs are kept in the list. Lifetime totals still count every run. */
const val RUNS_KEPT = 100

/**
 * One finished run. [wallMs] runs from [startedMs] to [endedMs] and is never less than [activeMs], so a clock set
 * backwards cannot make a run shorter than the time it was played. [newBest] means the score beat the best before it.
 */
data class FinishedRun(
    val score: Int,
    val stats: RunStats,
    val startedMs: Long,
    val endedMs: Long,
    val wallMs: Long,
    val activeMs: Long,
    val newBest: Boolean,
)

/** The finished run for this game, ended at [endedMs] (epoch ms), against the best score before it. */
fun Game.finish(
    endedMs: Long,
    previousBest: Int,
): FinishedRun =
    FinishedRun(
        score = score,
        stats = stats,
        startedMs = time.startedMs,
        endedMs = endedMs,
        wallMs = maxOf(endedMs - time.startedMs, time.activeMs),
        activeMs = time.activeMs,
        newBest = score > previousBest,
    )

/** [run] first, then [runs], cut to the [limit] newest. */
fun keepNewest(
    runs: List<FinishedRun>,
    run: FinishedRun,
    limit: Int = RUNS_KEPT,
): List<FinishedRun> = (listOf(run) + runs).take(limit)

/** `RN1;<key=value,...>`: the score, the stats, the clocks and `best` (1 for a new best). Zeros are left out. */
fun encodeRun(run: FinishedRun): String {
    val clocks = listOf(WALL to run.wallMs, ACTIVE to run.activeMs, STARTED to run.startedMs, ENDED to run.endedMs)
    val pairs =
        (listOf(SCORE to run.score.toLong()) + run.stats.toPairs() + clocks + (BEST to if (run.newBest) 1L else 0L))
            .filter { it.second != 0L }
    return RUN_VERSION + SEPARATOR + encodePairs(pairs)
}

/** Reads a line written by [encodeRun], or null if it is malformed. Missing keys read 0, unknown keys are ignored. */
fun decodeRun(line: String): FinishedRun? =
    line
        .takeIf { it.length <= MAX_RUN_LENGTH }
        ?.split(SEPARATOR)
        ?.takeIf { it.size == RUN_PART_COUNT && it[0] == RUN_VERSION }
        ?.let { decodePairs(it[1]) }
        ?.let { runFrom(it) }

private fun runFrom(pairs: Map<String, Long>): FinishedRun? {
    val stats = runStatsFrom(pairs)
    val score = pairs.value(SCORE).takeIf { it <= Int.MAX_VALUE }?.toInt()
    val bestFlagIsValid = pairs.value(BEST) <= 1L
    return if (stats != null && score != null && bestFlagIsValid) {
        FinishedRun(
            score = score,
            stats = stats,
            startedMs = pairs.value(STARTED),
            endedMs = pairs.value(ENDED),
            wallMs = pairs.value(WALL),
            activeMs = pairs.value(ACTIVE),
            newBest = pairs.value(BEST) == 1L,
        )
    } else {
        null
    }
}
