package dev.kesatria.gemmatch.core

data class Move(
    val from: Pos,
    val to: Pos,
)

/**
 * True if the swap of [a] and [b] is allowed: adjacent, both cells occupied, and either one of them is a
 * special piece or the swap puts a gem into a run.
 */
fun isValidSwap(
    board: Board,
    a: Pos,
    b: Pos,
): Boolean {
    val first = board[a]
    val second = board[b]
    if (!a.isAdjacentTo(b) || first == null || second == null) return false
    return first.isSpecial || second.isSpecial || makesMatch(board, a, b)
}

/** True if swapping [a] and [b] puts one of them into a run. Specials never join one. */
private fun makesMatch(
    board: Board,
    a: Pos,
    b: Pos,
): Boolean = findRuns(board.swap(a, b)).any { a in it.cells || b in it.cells }

/** Every valid swap once, listed as a cell and its right or lower neighbour. */
fun findValidMoves(board: Board): List<Move> = candidateMoves().filter { isValidSwap(board, it.from, it.to) }.toList()

/** The valid swaps that form a run. A swap that is legal only because it touches a special is left out. */
fun findMatchMoves(board: Board): List<Move> =
    candidateMoves().filter { bothOccupied(board, it) && makesMatch(board, it.from, it.to) }.toList()

fun hasValidMove(board: Board): Boolean = candidateMoves().any { isValidSwap(board, it.from, it.to) }

private fun bothOccupied(
    board: Board,
    move: Move,
): Boolean = board[move.from] != null && board[move.to] != null

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
