package dev.kesatria.gemmatch.core

/** Immutable 8x8 grid. A null cell is empty (during a cascade, between clearing and falling). */
class Board private constructor(
    private val cells: List<Piece?>,
) {
    /** The piece at [pos], or null if the cell is empty or outside the board. */
    operator fun get(pos: Pos): Piece? = if (pos.isInBounds()) cells[indexOf(pos)] else null

    fun with(
        pos: Pos,
        piece: Piece?,
    ): Board {
        require(pos.isInBounds()) { "$pos is outside the board" }
        val next = cells.toMutableList()
        next[indexOf(pos)] = piece
        return Board(next)
    }

    fun swap(
        a: Pos,
        b: Pos,
    ): Board = with(a, this[b]).with(b, this[a])

    override fun equals(other: Any?): Boolean = other is Board && cells == other.cells

    override fun hashCode(): Int = cells.hashCode()

    companion object {
        fun empty(): Board = Board(List(BOARD_SIZE * BOARD_SIZE) { null })

        private fun indexOf(pos: Pos): Int = pos.row * BOARD_SIZE + pos.col
    }
}

/** Every cell of the board, row by row. */
internal val ALL_CELLS: Set<Pos> =
    (0 until BOARD_SIZE).flatMapTo(mutableSetOf()) { row -> (0 until BOARD_SIZE).map { col -> Pos(row, col) } }
