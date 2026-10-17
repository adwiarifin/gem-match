package dev.kesatria.gemmatch.core

/** Immutable 8x8 grid. A null cell is empty (during a cascade, between clearing and falling). */
class Board private constructor(
    private val cells: List<Piece?>,
) {
    /** The piece at [pos], or null if the cell is empty or outside the board. */
    fun piece(pos: Pos): Piece? = if (pos.isInBounds()) cells[indexOf(pos)] else null

    /** The gem at [pos]. Null for an empty cell, outside the board, or the colour gem. Temporary shim until T20. */
    operator fun get(pos: Pos): Gem? = piece(pos)?.gem

    fun withPiece(
        pos: Pos,
        piece: Piece?,
    ): Board {
        require(pos.isInBounds()) { "$pos is outside the board" }
        val next = cells.toMutableList()
        next[indexOf(pos)] = piece
        return Board(next)
    }

    /** Temporary shim until T20. */
    fun with(
        pos: Pos,
        gem: Gem?,
    ): Board = withPiece(pos, gem?.let { Piece(it) })

    fun swap(
        a: Pos,
        b: Pos,
    ): Board = withPiece(a, piece(b)).withPiece(b, piece(a))

    override fun equals(other: Any?): Boolean = other is Board && cells == other.cells

    override fun hashCode(): Int = cells.hashCode()

    companion object {
        fun empty(): Board = Board(List(BOARD_SIZE * BOARD_SIZE) { null })

        private fun indexOf(pos: Pos): Int = pos.row * BOARD_SIZE + pos.col
    }
}
