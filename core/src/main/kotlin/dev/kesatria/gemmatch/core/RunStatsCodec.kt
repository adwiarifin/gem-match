package dev.kesatria.gemmatch.core

private const val MOVES = "moves"
private const val CASCADE = "cascade"
private const val MOVE_SCORE = "moveScore"
private const val GEMS = "gems"
private const val MATCH = "match"
private const val STARTED = "started"
private const val ACTIVE = "active"
private const val CREATED = "c"
private const val FIRED = "t"

/** The stats as key=value pairs. Zeros are left out, because a missing key reads as 0. */
internal fun RunStats.toPairs(): List<Pair<String, Long>> =
    (
        listOf(MOVES to moves, CASCADE to cascade, MOVE_SCORE to moveScore, GEMS to gems, MATCH to match) +
            Special.entries.map { CREATED + SPECIAL_CHARS.getValue(it) to (created[it] ?: 0) } +
            Special.entries.map { FIRED + SPECIAL_CHARS.getValue(it) to (fired[it] ?: 0) }
    ).filter { it.second != 0 }
        .map { (key, value) -> key to value.toLong() }

internal fun RunTime.toPairs(): List<Pair<String, Long>> {
    val clocks = listOf(STARTED to startedMs, ACTIVE to activeMs)
    return clocks.filter { it.second != 0L }
}

private fun kindKeys(prefix: String): List<String> = Special.entries.map { prefix + SPECIAL_CHARS.getValue(it) }

/** The stats in [pairs], or null if a count does not fit an `Int`. Unknown keys are ignored. */
internal fun runStatsFrom(pairs: Map<String, Long>): RunStats? {
    val keys = listOf(MOVES, CASCADE, MOVE_SCORE, GEMS, MATCH) + kindKeys(CREATED) + kindKeys(FIRED)
    if (keys.any { (pairs[it] ?: 0L) > Int.MAX_VALUE }) return null

    fun count(key: String): Int = (pairs[key] ?: 0L).toInt()

    fun kinds(prefix: String): Map<Special, Int> =
        Special.entries.associateWith { count(prefix + SPECIAL_CHARS.getValue(it)) }.filterValues { it != 0 }
    return RunStats(
        moves = count(MOVES),
        cascade = count(CASCADE),
        moveScore = count(MOVE_SCORE),
        gems = count(GEMS),
        match = count(MATCH),
        created = kinds(CREATED),
        fired = kinds(FIRED),
    )
}

internal fun runTimeFrom(pairs: Map<String, Long>): RunTime = RunTime(pairs[STARTED] ?: 0L, pairs[ACTIVE] ?: 0L)
