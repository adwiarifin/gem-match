package dev.kesatria.gemmatch.core

private const val VERSION_1 = "GM1"
private const val VERSION_2 = "GM2"
private const val VERSION_3 = "GM3"
private const val SEPARATOR = ";"
private const val PART_COUNT = 4
private const val PART_COUNT_GM3 = 5
private const val RUN_PART = 4
private const val END_KEY = "end"
private const val BOARD_PART = 3
private const val NO_PART = '-'

/** A real save is under 170 characters; anything much longer is not one and is not worth parsing. */
private const val MAX_SAVE_LENGTH = 256

/** A `GM3` save carries the run's stats as well; a worst case is about 560 characters. */
private const val MAX_GM3_LENGTH = 1024

private const val CELL_COUNT = BOARD_SIZE * BOARD_SIZE

internal val SPECIAL_CHARS =
    mapOf(
        Special.BLAST to 'B',
        Special.LINE_ROW to 'R',
        Special.LINE_COL to 'C',
        Special.CROSS to 'X',
        Special.X_SHORT to 'S',
        Special.X_FULL to 'F',
        Special.COLOUR to 'W',
    )

private val GEM_BY_DIGIT = Gem.entries.associateBy { '0' + it.ordinal }

private val SPECIAL_BY_CHAR = SPECIAL_CHARS.entries.associate { (special, char) -> char to special }

internal data class SavedState(
    val board: Board,
    val score: Int,
    val rngState: Long,
    val stats: RunStats = RunStats.EMPTY,
    val time: RunTime = RunTime(0L, 0L),
)

/**
 * Text format `GM3;<score>;<rng state>;<64 cells, row by row>;<pairs>`. The pairs are the run's stats, `started` and
 * `active`, as `key=value` separated by commas, zeros left out, and always ending in `end=1` so that a cut-off save is
 * never mistaken for a short one. A cell is two characters: the gem digit
 * (`-` for the colour gem), then the special kind (`-` none, `B` blast, `R` line along the row,
 * `C` line along the column, `X` cross, `S` short X, `F` full X, `W` colour). Hand-written to avoid a dependency.
 */
internal fun encodeState(state: SavedState): String {
    val cells =
        (0 until BOARD_SIZE)
            .flatMap { row -> (0 until BOARD_SIZE).map { col -> state.board[Pos(row, col)] } }
            .joinToString("") { encodeCell(it) }
    val pairs = state.stats.toPairs() + state.time.toPairs() + (END_KEY to 1L)
    return listOf(VERSION_3, state.score, state.rngState, cells, encodePairs(pairs)).joinToString(SEPARATOR)
}

private fun encodeCell(piece: Piece?): String {
    val gem = piece?.gem?.ordinal?.toString() ?: NO_PART.toString()
    val special = piece?.special?.let { SPECIAL_CHARS.getValue(it) } ?: NO_PART
    return "$gem$special"
}

/**
 * Reads `GM3`, and the older `GM2` and `GM1` (one gem digit per cell, no specials), which carry no stats: their run
 * starts at [nowMs]. Null for anything else.
 */
internal fun decodeState(
    text: String,
    nowMs: Long = 0L,
): SavedState? =
    text
        .takeIf { it.length <= if (it.startsWith(VERSION_3 + SEPARATOR)) MAX_GM3_LENGTH else MAX_SAVE_LENGTH }
        ?.trim()
        ?.split(SEPARATOR)
        ?.takeIf { it.size == if (it[0] == VERSION_3) PART_COUNT_GM3 else PART_COUNT }
        ?.let { parts -> decodeParts(parts, nowMs) }

/** The stats and clocks in a `GM3` run part, or null if it is malformed or does not end in `end=1`. */
private fun decodeRunPart(text: String): Pair<RunStats, RunTime>? {
    val pairs = decodePairs(text)?.takeIf { it[END_KEY] == 1L }
    val stats = pairs?.let { runStatsFrom(it) }
    return if (pairs != null && stats != null) stats to runTimeFrom(pairs) else null
}

private fun decodeParts(
    parts: List<String>,
    nowMs: Long,
): SavedState? {
    val score = parts[1].toIntOrNull()
    val rngState = parts[2].toLongOrNull()
    val board =
        when (parts[0]) {
            VERSION_1 -> decodeGm1Board(parts[BOARD_PART])
            VERSION_2, VERSION_3 -> decodeGm2Board(parts[BOARD_PART])
            else -> null
        }
    val run = if (parts[0] == VERSION_3) decodeRunPart(parts[RUN_PART]) else RunStats.EMPTY to RunTime(nowMs, 0L)
    val settled = board != null && findRuns(board).isEmpty()
    val valid = score != null && score >= 0 && rngState != null && settled && run != null
    return if (valid) SavedState(board, score, rngState, run.first, run.second) else null
}

private fun decodeGm1Board(digits: String): Board? {
    val gems = digits.takeIf { it.length == CELL_COUNT }?.map { GEM_BY_DIGIT[it] }
    return gems?.takeIf { null !in it }?.let { boardOf(it.filterNotNull().map { gem -> Piece(gem) }) }
}

private fun decodeGm2Board(text: String): Board? {
    val pieces = text.takeIf { it.length == CELL_COUNT * 2 }?.chunked(2)?.map { decodeCell(it) }
    return pieces?.takeIf { cells -> cells.none { it == null } }?.let { boardOf(it.filterNotNull()) }
}

/** Null unless the pair is a gem digit with `-` or a letter other than `W`, or the colour gem `-W`. */
private fun decodeCell(cell: String): Piece? {
    val (gemChar, specialChar) = cell[0] to cell[1]
    val gemKnown = gemChar == NO_PART || gemChar in GEM_BY_DIGIT
    val specialKnown = specialChar == NO_PART || specialChar in SPECIAL_BY_CHAR
    val gem = GEM_BY_DIGIT[gemChar]
    val special = SPECIAL_BY_CHAR[specialChar]
    val coherent = (gem == null) == (special == Special.COLOUR)
    return if (gemKnown && specialKnown && coherent) Piece(gem, special) else null
}

private fun boardOf(pieces: List<Piece>): Board =
    pieces.foldIndexed(Board.empty()) { index, board, piece ->
        board.with(Pos(index / BOARD_SIZE, index % BOARD_SIZE), piece)
    }
