package dev.kesatria.gemmatch.core

/** A maximal straight line of [MIN_MATCH] or more identical gems. */
data class Run(
    val gem: Gem,
    val cells: List<Pos>,
    val isHorizontal: Boolean,
) {
    val length: Int get() = cells.size
}

/** Runs that share cells (L, T and + shapes), cleared together. */
data class MatchGroup(
    val runs: List<Run>,
) {
    val cells: Set<Pos> = runs.flatMapTo(mutableSetOf()) { it.cells }
    val isCompound: Boolean get() = runs.size > 1
}

fun findRuns(board: Board): List<Run> =
    (0 until BOARD_SIZE).flatMap { runsIn(board, it, isHorizontal = true) } +
        (0 until BOARD_SIZE).flatMap { runsIn(board, it, isHorizontal = false) }

fun findMatchGroups(board: Board): List<MatchGroup> {
    val remaining = findRuns(board).toMutableList()
    val groups = mutableListOf<MatchGroup>()
    while (remaining.isNotEmpty()) {
        val members = mutableListOf(remaining.removeAt(0))
        var cells = members[0].cells.toSet()
        do {
            val touching = remaining.filter { run -> run.cells.any { it in cells } }
            remaining.removeAll(touching)
            members += touching
            cells = cells + touching.flatMap { it.cells }
        } while (touching.isNotEmpty())
        groups += MatchGroup(members)
    }
    return groups
}

private fun runsIn(
    board: Board,
    line: Int,
    isHorizontal: Boolean,
): List<Run> {
    val runs = mutableListOf<Run>()
    var gem: Gem? = null
    var current = mutableListOf<Pos>()

    fun closeRun() {
        val runGem = gem
        if (runGem != null && current.size >= MIN_MATCH) {
            runs += Run(runGem, current.toList(), isHorizontal)
        }
    }

    for (step in 0 until BOARD_SIZE) {
        val pos = if (isHorizontal) Pos(line, step) else Pos(step, line)
        val here = board[pos]
        if (here != null && here == gem) {
            current += pos
        } else {
            closeRun()
            gem = here
            current = if (here != null) mutableListOf(pos) else mutableListOf()
        }
    }
    closeRun()
    return runs
}
