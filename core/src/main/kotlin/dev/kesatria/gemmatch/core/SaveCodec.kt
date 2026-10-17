package dev.kesatria.gemmatch.core

private const val VERSION_1 = "GM1"
private const val VERSION_2 = "GM2"
private const val SEPARATOR = ";"
private const val PART_COUNT = 4
private const val BOARD_PART = 3
private const val NO_PART = '-'

/** A real save is under 170 characters; anything much longer is not one and is not worth parsing. */
private const val MAX_SAVE_LENGTH = 256

private const val CELL_COUNT = BOARD_SIZE * BOARD_SIZE

private val SPECIAL_CHARS =
    mapOf(
        Special.BLAST to 'B',
        Special.LINE_ROW to 'R',
        Special.LINE_COL to 'C',
        Special.CROSS to 'X',
        Special.X_SHORT to 'S',
        Special.X_FULL to 'F',
        Special.COLOUR to 'W',
    )

internal data class SavedState(
    val board: Board,
    val score: Int,
    val rngState: Long,
)

/**
 * Text format `GM2;<score>;<rng state>;<64 cells, row by row>`. A cell is two characters: the gem digit
 * (`-` for the colour gem), then the special kind (`-` none, `B` blast, `R` line along the row,
 * `C` line along the column, `X` cross, `S` short X, `F` full X, `W` colour). Hand-written to avoid a dependency.
 */
internal fun encodeState(state: SavedState): String {
    val cells =
        (0 until BOARD_SIZE)
            .flatMap { row -> (0 until BOARD_SIZE).map { col -> state.board[Pos(row, col)] } }
            .joinToString("") { encodeCell(it) }
    return listOf(VERSION_2, state.score, state.rngState, cells).joinToString(SEPARATOR)
}

private fun encodeCell(piece: Piece?): String {
    val gem = piece?.gem?.ordinal?.toString() ?: NO_PART.toString()
    val special = piece?.special?.let { SPECIAL_CHARS.getValue(it) } ?: NO_PART
    return "$gem$special"
}

/** Reads `GM2`, and the older `GM1` (one gem digit per cell, no specials). Null for anything else. */
internal fun decodeState(text: String): SavedState? =
    text
        .takeIf { it.length <= MAX_SAVE_LENGTH }
        ?.trim()
        ?.split(SEPARATOR)
        ?.takeIf { it.size == PART_COUNT }
        ?.let { parts -> decodeParts(parts) }

private fun decodeParts(parts: List<String>): SavedState? {
    val score = parts[1].toIntOrNull()
    val rngState = parts[2].toLongOrNull()
    val board =
        when (parts[0]) {
            VERSION_1 -> decodeGm1Board(parts[BOARD_PART])
            VERSION_2 -> decodeGm2Board(parts[BOARD_PART])
            else -> null
        }
    val valid = score != null && score >= 0 && rngState != null && board != null && isPlayable(board)
    return if (valid) SavedState(board, score, rngState) else null
}

private fun decodeGm1Board(digits: String): Board? {
    val gems =
        digits
            .takeIf { it.length == CELL_COUNT }
            ?.map { char -> if (char in '0'..'9') Gem.entries.getOrNull(char - '0') else null }
            ?.filterNotNull()
    return gems?.takeIf { it.size == digits.length }?.let { boardOf(it.map { gem -> Piece(gem) }) }
}

private fun decodeGm2Board(text: String): Board? {
    val pieces = text.takeIf { it.length == CELL_COUNT * 2 }?.chunked(2)?.map { decodeCell(it) }
    return pieces?.takeIf { cells -> cells.none { it == null } }?.let { boardOf(it.filterNotNull()) }
}

/** Null unless the pair is a gem digit with `-` or a letter other than `W`, or the colour gem `-W`. */
private fun decodeCell(cell: String): Piece? {
    val gem = if (cell[0] == NO_PART) null else Gem.entries.getOrNull(cell[0] - '0')?.takeIf { cell[0] in '0'..'9' }
    val special = if (cell[1] == NO_PART) null else SPECIAL_CHARS.entries.firstOrNull { it.value == cell[1] }?.key
    val known = (cell[0] == NO_PART || gem != null) && (cell[1] == NO_PART || special != null)
    val coherent = (gem == null) == (special == Special.COLOUR)
    return if (known && coherent) Piece(gem, special) else null
}

private fun boardOf(pieces: List<Piece>): Board =
    pieces.foldIndexed(Board.empty()) { index, board, piece ->
        board.with(Pos(index / BOARD_SIZE, index % BOARD_SIZE), piece)
    }
