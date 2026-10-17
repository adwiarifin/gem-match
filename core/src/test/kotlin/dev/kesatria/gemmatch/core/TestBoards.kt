package dev.kesatria.gemmatch.core

private val GEM_BY_CHAR =
    mapOf(
        'R' to Gem.RED,
        'O' to Gem.ORANGE,
        'Y' to Gem.YELLOW,
        'G' to Gem.GREEN,
        'B' to Gem.BLUE,
        'P' to Gem.PURPLE,
        'W' to Gem.WHITE,
    )

/**
 * Builds a board from text rows: R O Y G B P W are gems, '.' is empty.
 * Missing rows and columns are left empty, so tests only spell out the part they care about.
 */
fun boardOf(vararg rows: String): Board {
    var board = Board.empty()
    rows.forEachIndexed { row, text ->
        text.forEachIndexed { col, char ->
            if (char != '.') {
                val gem = requireNotNull(GEM_BY_CHAR[char]) { "unknown gem '$char'" }
                board = board.with(Pos(row, col), Piece(gem))
            }
        }
    }
    return board
}

fun cells(vararg positions: Pair<Int, Int>): Set<Pos> = positions.map { Pos(it.first, it.second) }.toSet()

/** A full board with no runs, filled left to right and top to bottom from [seed]. */
fun randomStableBoard(seed: Long): Board {
    val rng = Rng(seed)
    var board = Board.empty()
    for (row in 0 until BOARD_SIZE) {
        for (col in 0 until BOARD_SIZE) {
            var gem = rng.nextGem()
            while (completesRun(board, Pos(row, col), gem)) gem = rng.nextGem()
            board = board.with(Pos(row, col), Piece(gem))
        }
    }
    return board
}

private fun completesRun(
    board: Board,
    pos: Pos,
    gem: Gem,
): Boolean {
    val left = board[Pos(pos.row, pos.col - 1)]?.gem == gem && board[Pos(pos.row, pos.col - 2)]?.gem == gem
    val above = board[Pos(pos.row - 1, pos.col)]?.gem == gem && board[Pos(pos.row - 2, pos.col)]?.gem == gem
    return left || above
}
