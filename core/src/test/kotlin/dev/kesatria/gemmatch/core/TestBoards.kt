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
                board = board.with(Pos(row, col), gem)
            }
        }
    }
    return board
}

fun cells(vararg positions: Pair<Int, Int>): Set<Pos> = positions.map { Pos(it.first, it.second) }.toSet()
