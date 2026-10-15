package dev.kesatria.gemmatch.core

/**
 * Independent re-implementation of "apply these steps to a board", used to prove that the steps
 * emitted by the resolver fully describe what happened. Checks each Clear against the matches on
 * the board it is applied to, so scoring is cross-checked too.
 */
fun replay(
    start: Board,
    steps: List<Step>,
): Board =
    steps.fold(start) { board, step ->
        when (step) {
            is Step.Swap -> {
                board.swap(step.a, step.b)
            }

            is Step.Clear -> {
                val groups = findMatchGroups(board)
                check(step.cells == groups.flatMap { it.cells }.toSet()) { "Clear cells differ from matches" }
                check(step.points == scoreGroups(groups, step.depth)) { "Clear points differ from scoring rules" }
                step.cells.fold(board) { b, pos -> b.with(pos, null) }
            }

            is Step.Fall -> {
                val vacated = step.moves.fold(board) { b, move -> b.with(move.from, null) }
                step.moves.fold(vacated) { b, move -> b.with(move.to, move.gem) }
            }

            is Step.Spawn -> {
                step.gems.fold(board) { b, placed -> b.with(placed.pos, placed.gem) }
            }

            is Step.Reshuffle -> {
                step.board
            }
        }
    }

fun isFull(board: Board): Boolean {
    val range = 0 until BOARD_SIZE
    return range.all { row -> range.all { col -> board[Pos(row, col)] != null } }
}
