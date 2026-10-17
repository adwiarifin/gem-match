package dev.kesatria.gemmatch.core

/** A maximal straight line of [MIN_MATCH] or more identical gems. */
data class Run(
    val gem: Gem,
    val cells: List<Pos>,
    val isHorizontal: Boolean,
) {
    val length: Int get() = cells.size
}

/**
 * Runs that share cells (L, T and + shapes), cleared together. A run of exactly three can also take one
 * [hanging] gem of the same colour that touches one of its cells at a right angle: a short L when it touches an
 * end, a short T when it touches the middle.
 */
data class MatchGroup(
    val runs: List<Run>,
    val hanging: Pos? = null,
) {
    val cells: Set<Pos> = runs.flatMapTo(mutableSetOf()) { it.cells } + listOfNotNull(hanging)
    val isCompound: Boolean get() = runs.size > 1
}

fun findRuns(board: Board): List<Run> =
    (0 until BOARD_SIZE).flatMap { runsIn(board, it, isHorizontal = true) } +
        (0 until BOARD_SIZE).flatMap { runsIn(board, it, isHorizontal = false) }

fun findMatchGroups(board: Board): List<MatchGroup> {
    val runs = findRuns(board)
    val inRun = runs.flatMapTo(mutableSetOf()) { it.cells }
    val taken = mutableSetOf<Pos>()
    return groupRuns(runs).map { group ->
        val run = group.runs.singleOrNull()?.takeIf { it.length == HANGING_RUN }
        val hanging = run?.let { hangingGem(board, it, inRun, taken) }
        if (hanging == null) group else MatchGroup(group.runs, hanging).also { taken += hanging }
    }
}

/** The one gem that hangs off [run]: same colour, in no run itself, taken by no other group. First in board order. */
private fun hangingGem(
    board: Board,
    run: Run,
    inRun: Set<Pos>,
    taken: Set<Pos>,
): Pos? =
    run.cells
        .flatMap { cell ->
            if (run.isHorizontal) {
                listOf(Pos(cell.row - 1, cell.col), Pos(cell.row + 1, cell.col))
            } else {
                listOf(
                    Pos(cell.row, cell.col - 1),
                    Pos(
                        cell.row,
                        cell.col + 1,
                    ),
                )
            }
        }.filter { it.isInBounds() && board[it]?.gem == run.gem && it !in inRun && it !in taken }
        .minWithOrNull(compareBy({ it.row }, { it.col }))

private const val HANGING_RUN = 3

private fun groupRuns(runs: List<Run>): List<MatchGroup> {
    val remaining = runs.toMutableList()
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
        val here = board[pos]?.gem
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
