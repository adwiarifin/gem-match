package dev.kesatria.gemmatch.core

/**
 * Independent re-implementation of "apply these steps to a board", used to prove that the steps
 * emitted by the resolver fully describe what happened. Checks each Clear against the matches on
 * the board it is applied to, so scoring is cross-checked too.
 */
fun replay(
    start: Board,
    steps: List<Step>,
): Board {
    val pending = mutableSetOf<Pos>()
    return steps.fold(start) { board, step ->
        when (step) {
            is Step.Swap -> {
                board.swap(step.a, step.b)
            }

            is Step.Activate -> {
                check(step.cells == expectedEffect(board, step)) { "Activate cells differ from the effect" }
                pending += step.cells
                board
            }

            is Step.Clear -> {
                val groups = findMatchGroups(board)
                val matched = groups.flatMap { it.cells }.toSet()
                check(step.cells == matched + pending) { "Clear cells differ from matches and effects" }
                val effectOnly = (pending - matched).size
                val expected = scoreGroups(groups, step.depth) + effectOnly * POINTS_PER_EFFECT_GEM * step.depth
                check(step.points == expected) { "Clear points differ from scoring rules" }
                pending.clear()
                step.cells.fold(board) { b, pos -> b.with(pos, null) }
            }

            is Step.Create -> {
                board.with(step.pos, step.piece)
            }

            is Step.Fall -> {
                val vacated = step.moves.fold(board) { b, move -> b.with(move.from, null) }
                step.moves.fold(vacated) { b, move -> b.with(move.to, move.piece) }
            }

            is Step.Spawn -> {
                step.gems.fold(board) { b, placed -> b.with(placed.pos, placed.piece) }
            }
        }
    }
}

fun isFull(board: Board): Boolean {
    val range = 0 until BOARD_SIZE
    return range.all { row -> range.all { col -> board[Pos(row, col)] != null } }
}

/** What an Activate should take, worked out from the board it fires on rather than from the resolver. */
private fun expectedEffect(
    board: Board,
    step: Step.Activate,
): Set<Pos> =
    when {
        // Two of the same line or blast swapped together clear one bigger area, centred where the dragged one landed.
        step.special != Special.COLOUR && step.cells == pairCells(step.special, step.origin) -> step.cells
        step.special != Special.COLOUR -> effectCells(step.origin, step.special)
        // Two colour gems clear the board.
        ALL_CELLS.count { board[it]?.special == Special.COLOUR } >= 2 && step.cells == ALL_CELLS -> ALL_CELLS
        // Otherwise the colour takes every piece of one gem colour, and itself.
        else -> colourTake(board, step)
    }

private fun colourTake(
    board: Board,
    step: Step.Activate,
): Set<Pos> {
    val colour = step.cells.firstOrNull { it != step.origin }?.let { board[it]?.gem }
    return ALL_CELLS.filterTo(mutableSetOf(step.origin)) { colour != null && board[it]?.gem == colour }
}
