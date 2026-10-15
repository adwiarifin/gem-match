package dev.kesatria.gemmatch.core

private const val VERSION = "GM1"
private const val SEPARATOR = ";"
private const val PART_COUNT = 4
private const val BOARD_PART = 3

internal data class SavedState(
    val board: Board,
    val score: Int,
    val rngState: Long,
)

/** Text format `GM1;<score>;<rng state>;<64 gem digits, row by row>`. Hand-written to avoid a dependency. */
internal fun encodeState(state: SavedState): String {
    val digits =
        (0 until BOARD_SIZE)
            .flatMap { row -> (0 until BOARD_SIZE).map { col -> state.board[Pos(row, col)] } }
            .joinToString("") { (it?.ordinal ?: -1).toString() }
    return listOf(VERSION, state.score, state.rngState, digits).joinToString(SEPARATOR)
}

/** Returns null for anything that is not a complete, valid, playable saved game. */
internal fun decodeState(text: String): SavedState? {
    val parts = text.trim().split(SEPARATOR)
    if (parts.size != PART_COUNT || parts[0] != VERSION) return null

    val score = parts[1].toIntOrNull()
    val rngState = parts[2].toLongOrNull()
    val board = decodeBoard(parts[BOARD_PART])
    val valid = score != null && score >= 0 && rngState != null && board != null && isPlayable(board)
    return if (valid) SavedState(board, score, rngState) else null
}

private fun decodeBoard(digits: String): Board? {
    val gems = digits.map { char -> if (char in '0'..'9') Gem.entries.getOrNull(char - '0') else null }
    if (digits.length != BOARD_SIZE * BOARD_SIZE || gems.any { it == null }) return null
    return gems.foldIndexed(Board.empty()) { index, board, gem ->
        board.with(Pos(index / BOARD_SIZE, index % BOARD_SIZE), gem)
    }
}
