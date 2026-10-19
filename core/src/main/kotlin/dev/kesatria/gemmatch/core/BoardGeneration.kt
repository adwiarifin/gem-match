package dev.kesatria.gemmatch.core

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
