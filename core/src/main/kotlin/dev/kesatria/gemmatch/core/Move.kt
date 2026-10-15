package dev.kesatria.gemmatch.core

data class Move(
    val from: Pos,
    val to: Pos,
)

/** True if swapping [a] and [b] puts one of them into a run. Only adjacent, occupied cells can swap. */
fun isValidSwap(
    board: Board,
    a: Pos,
    b: Pos,
): Boolean {
    if (!a.isAdjacentTo(b) || board[a] == null || board[b] == null) return false
    return findRuns(board.swap(a, b)).any { a in it.cells || b in it.cells }
}

/** Every valid swap once, listed as a cell and its right or lower neighbour. */
fun findValidMoves(board: Board): List<Move> = candidateMoves().filter { isValidSwap(board, it.from, it.to) }.toList()

fun hasValidMove(board: Board): Boolean = candidateMoves().any { isValidSwap(board, it.from, it.to) }

private fun candidateMoves(): Sequence<Move> =
    sequence {
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val from = Pos(row, col)
                yield(Move(from, Pos(row, col + 1)))
                yield(Move(from, Pos(row + 1, col)))
            }
        }
    }
