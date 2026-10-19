package dev.kesatria.gemmatch.core

/**
 * What a run has done so far, built from the steps of each move. Absent kinds in [created] and [fired] count as 0.
 * [cascade], [moveScore] and [match] are the best seen; the rest are totals.
 */
data class RunStats(
    val moves: Int,
    val cascade: Int,
    val moveScore: Int,
    val gems: Int,
    val match: Int,
    val created: Map<Special, Int>,
    val fired: Map<Special, Int>,
) {
    operator fun plus(other: RunStats): RunStats =
        RunStats(
            moves = saturatingAdd(moves, other.moves),
            cascade = maxOf(cascade, other.cascade),
            moveScore = maxOf(moveScore, other.moveScore),
            gems = saturatingAdd(gems, other.gems),
            match = maxOf(match, other.match),
            created = created.sumWith(other.created),
            fired = fired.sumWith(other.fired),
        )

    companion object {
        val EMPTY = RunStats(0, 0, 0, 0, 0, emptyMap(), emptyMap())

        /** The stats of one accepted move. */
        internal fun ofMove(resolution: Resolution): RunStats {
            val clears = resolution.steps.filterIsInstance<Step.Clear>()
            return RunStats(
                moves = 1,
                cascade = clears.maxOfOrNull { it.depth } ?: 0,
                moveScore = resolution.score,
                gems = clears.fold(0) { total, clear -> saturatingAdd(total, clear.cells.size) },
                match = resolution.largestMatch,
                created =
                    resolution.steps
                        .filterIsInstance<Step.Create>()
                        .mapNotNull { it.piece.special }
                        .countByKind(),
                fired =
                    resolution.steps
                        .filterIsInstance<Step.Activate>()
                        .map { it.special }
                        .countByKind(),
            )
        }
    }
}

private fun List<Special>.countByKind(): Map<Special, Int> = groupingBy { it }.eachCount()

private fun Map<Special, Int>.sumWith(other: Map<Special, Int>): Map<Special, Int> =
    (keys + other.keys).associateWith { saturatingAdd(this[it] ?: 0, other[it] ?: 0) }
