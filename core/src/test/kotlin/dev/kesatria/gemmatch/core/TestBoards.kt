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

/** The gem for one of the characters R O Y G B P W, as `boardOf` reads them. */
fun gemOf(char: Char): Gem = requireNotNull(GEM_BY_CHAR[char]) { "unknown gem '$char'" }

/** The red, orange, yellow diagonal has no runs and no moves; only the cells passed in are overwritten. */
fun patternedBoard(overrides: List<Pair<Pos, Piece>>): Board {
    val cells = (0 until BOARD_SIZE).flatMap { row -> (0 until BOARD_SIZE).map { col -> Pos(row, col) } }
    val base = cells.fold(Board.empty()) { board, pos -> board.with(pos, Piece(gemOf("ROY"[(pos.row + pos.col) % 3]))) }
    return overrides.fold(base) { board, (pos, piece) -> board.with(pos, piece) }
}

fun patterned(vararg overrides: Pair<Pos, Piece>): Board = patternedBoard(overrides.toList())

private val SAVE_LETTERS =
    mapOf(
        Special.BLAST to 'B',
        Special.LINE_ROW to 'R',
        Special.LINE_COL to 'C',
        Special.CROSS to 'X',
        Special.X_SHORT to 'S',
        Special.X_FULL to 'F',
        Special.COLOUR to 'W',
    )

/** A game on [board], which may hold special pieces, loaded through the real `GM2` reader. */
fun gameOf(
    board: Board,
    score: Int = 0,
    rng: Long = 1L,
): Game {
    val cells =
        (0 until BOARD_SIZE).joinToString("") { row ->
            (0 until BOARD_SIZE).joinToString("") { col ->
                val piece = checkNotNull(board[Pos(row, col)])
                "${piece.gem?.ordinal ?: '-'}${piece.special?.let { SAVE_LETTERS.getValue(it) } ?: '-'}"
            }
        }
    return checkNotNull(Game.decode("GM2;$score;$rng;$cells")) { "the board is not a playable game" }
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
