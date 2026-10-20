package dev.kesatria.gemmatch.core

private const val LIFETIME_VERSION = "LT1"
private const val SEPARATOR = ";"
private const val LIFETIME_PART_COUNT = 2
private const val MAX_LIFETIME_LENGTH = 2048
private const val RUNS = "runs"
private const val SCORE = "score"
private const val MOVES = "moves"
private const val GEMS = "gems"
private const val WALL = "wall"
private const val ACTIVE = "active"
private const val BEST_SCORE = "bestScore"
private const val BEST_CASCADE = "bestCascade"
private const val BEST_MOVE = "bestMove"
private const val BEST_MATCH = "bestMatch"
private const val BEST_WALL = "bestWall"
private const val BEST_ACTIVE = "bestActive"
private const val LAST = "last"
private const val CREATED = "c"
private const val FIRED = "t"

/**
 * Everything ever played, kept apart from the run list so that dropping an old run from the list loses nothing.
 * Sums are `Long` and stop at the limit instead of wrapping. [last] is the start time of the newest run recorded,
 * so a run that is recorded twice can be told apart from a new one.
 */
data class Lifetime(
    val runs: Long,
    val score: Long,
    val moves: Long,
    val gems: Long,
    val created: Map<Special, Long>,
    val fired: Map<Special, Long>,
    val wallMs: Long,
    val activeMs: Long,
    val bestScore: Int,
    val bestCascade: Int,
    val bestMoveScore: Int,
    val bestMatch: Int,
    val bestWallMs: Long,
    val bestActiveMs: Long,
    val last: Long,
) {
    operator fun plus(run: FinishedRun): Lifetime =
        Lifetime(
            runs = add(runs, 1L),
            score = add(score, run.score.toLong()),
            moves = add(moves, run.stats.moves.toLong()),
            gems = add(gems, run.stats.gems.toLong()),
            created = created.addKinds(run.stats.created),
            fired = fired.addKinds(run.stats.fired),
            wallMs = add(wallMs, run.wallMs),
            activeMs = add(activeMs, run.activeMs),
            bestScore = maxOf(bestScore, run.score),
            bestCascade = maxOf(bestCascade, run.stats.cascade),
            bestMoveScore = maxOf(bestMoveScore, run.stats.moveScore),
            bestMatch = maxOf(bestMatch, run.stats.match),
            bestWallMs = maxOf(bestWallMs, run.wallMs),
            bestActiveMs = maxOf(bestActiveMs, run.activeMs),
            last = run.startedMs,
        )

    companion object {
        val EMPTY = Lifetime(0, 0, 0, 0, emptyMap(), emptyMap(), 0, 0, 0, 0, 0, 0, 0, 0, 0)
    }
}

private fun add(
    a: Long,
    b: Long,
): Long = if (a > Long.MAX_VALUE - b) Long.MAX_VALUE else a + b

private fun Map<Special, Long>.addKinds(more: Map<Special, Int>): Map<Special, Long> =
    (keys + more.keys).associateWith { add(this[it] ?: 0L, (more[it] ?: 0).toLong()) }.filterValues { it != 0L }

private fun Map<Special, Long>.toPairs(prefix: String): List<Pair<String, Long>> =
    Special.entries.map { prefix + SPECIAL_CHARS.getValue(it) to (this[it] ?: 0L) }

/** `LT1;<key=value,...>`. Zeros are left out. */
fun encodeLifetime(lifetime: Lifetime): String {
    val pairs =
        listOf(
            RUNS to lifetime.runs,
            SCORE to lifetime.score,
            MOVES to lifetime.moves,
            GEMS to lifetime.gems,
            WALL to lifetime.wallMs,
            ACTIVE to lifetime.activeMs,
            BEST_SCORE to lifetime.bestScore.toLong(),
            BEST_CASCADE to lifetime.bestCascade.toLong(),
            BEST_MOVE to lifetime.bestMoveScore.toLong(),
            BEST_MATCH to lifetime.bestMatch.toLong(),
            BEST_WALL to lifetime.bestWallMs,
            BEST_ACTIVE to lifetime.bestActiveMs,
            LAST to lifetime.last,
        ) + lifetime.created.toPairs(CREATED) + lifetime.fired.toPairs(FIRED)
    return LIFETIME_VERSION + SEPARATOR + encodePairs(pairs.filter { it.second != 0L })
}

/** Reads a line from [encodeLifetime], or null if malformed. Missing keys read 0, unknown keys are ignored. */
fun decodeLifetime(line: String): Lifetime? =
    line
        .takeIf { it.length <= MAX_LIFETIME_LENGTH }
        ?.split(SEPARATOR)
        ?.takeIf { it.size == LIFETIME_PART_COUNT && it[0] == LIFETIME_VERSION }
        ?.let { decodePairs(it[1]) }
        ?.let { lifetimeFrom(it) }

private fun kindTotals(
    pairs: Map<String, Long>,
    prefix: String,
): Map<Special, Long> {
    val totals = Special.entries.associateWith { pairs.value(prefix + SPECIAL_CHARS.getValue(it)) }
    return totals.filterValues { it != 0L }
}

private fun lifetimeFrom(pairs: Map<String, Long>): Lifetime? {
    val bests = listOf(BEST_SCORE, BEST_CASCADE, BEST_MOVE, BEST_MATCH).map { pairs.value(it) }
    return if (bests.all { it <= Int.MAX_VALUE }) {
        Lifetime(
            runs = pairs.value(RUNS),
            score = pairs.value(SCORE),
            moves = pairs.value(MOVES),
            gems = pairs.value(GEMS),
            created = kindTotals(pairs, CREATED),
            fired = kindTotals(pairs, FIRED),
            wallMs = pairs.value(WALL),
            activeMs = pairs.value(ACTIVE),
            bestScore = bests[0].toInt(),
            bestCascade = bests[1].toInt(),
            bestMoveScore = bests[2].toInt(),
            bestMatch = bests[3].toInt(),
            bestWallMs = pairs.value(BEST_WALL),
            bestActiveMs = pairs.value(BEST_ACTIVE),
            last = pairs.value(LAST),
        )
    } else {
        null
    }
}
