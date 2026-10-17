package dev.kesatria.gemmatch.core

private const val MAX_SHUFFLE_ATTEMPTS = 200

internal fun isPlayable(board: Board): Boolean = findRuns(board).isEmpty() && hasValidMove(board)

/** A full board with no runs and at least one valid move. */
fun generateBoard(rng: Rng): Board = generateSequence { fillWithoutRuns(rng) }.first { hasValidMove(it) }

/** Fills left to right and top to bottom, redrawing any gem that would complete a run. */
private fun fillWithoutRuns(rng: Rng): Board {
    val positions = (0 until BOARD_SIZE).flatMap { row -> (0 until BOARD_SIZE).map { col -> Pos(row, col) } }
    return positions.fold(Board.empty()) { board, pos ->
        val gem = generateSequence { rng.nextGem() }.first { !completesRun(board, pos, it) }
        board.with(pos, Piece(gem))
    }
}

/**
 * Returns [board] untouched if it is stable and has a valid move. Otherwise rearranges the same gems
 * until it does, and reports that as a [Step.Reshuffle]. If no arrangement of those gems can work,
 * falls back to a freshly generated board so the game never stalls.
 */
fun ensureMoveExists(
    board: Board,
    rng: Rng,
): Pair<Board, Step.Reshuffle?> {
    if (isPlayable(board)) return board to null
    val rearranged =
        (0 until MAX_SHUFFLE_ATTEMPTS)
            .asSequence()
            .mapNotNull { rearranged(board, rng) }
            .firstOrNull { isPlayable(it) } ?: generateBoard(rng)
    return rearranged to Step.Reshuffle(rearranged)
}

/**
 * Places the same gems in a random order, never completing a run. Returns null if it paints itself
 * into a corner (for example when the gems cannot avoid runs at all).
 */
private fun rearranged(
    board: Board,
    rng: Rng,
): Board? {
    val positions = (0 until BOARD_SIZE).flatMap { row -> (0 until BOARD_SIZE).map { col -> Pos(row, col) } }
    val pool = positions.mapNotNull { board[it] }.toMutableList()
    var result = Board.empty()
    for (pos in positions) {
        val fits = pool.indices.filter { !completesRun(result, pos, pool[it].gem) }
        if (fits.isEmpty()) return null
        result = result.with(pos, pool.removeAt(fits[rng.nextInt(fits.size)]))
    }
    return result
}

private fun completesRun(
    board: Board,
    pos: Pos,
    gem: Gem?,
): Boolean {
    if (gem == null) return false
    val left = board[Pos(pos.row, pos.col - 1)]?.gem == gem && board[Pos(pos.row, pos.col - 2)]?.gem == gem
    val above = board[Pos(pos.row - 1, pos.col)]?.gem == gem && board[Pos(pos.row - 2, pos.col)]?.gem == gem
    return left || above
}
