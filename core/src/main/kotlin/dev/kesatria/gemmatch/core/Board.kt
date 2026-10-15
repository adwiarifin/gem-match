package dev.kesatria.gemmatch.core

/** Immutable 8x8 grid. A null cell is empty (during a cascade, between clearing and falling). */
class Board private constructor(
    private val cells: List<Gem?>,
) {
    /** The gem at [pos], or null if the cell is empty or outside the board. */
    operator fun get(pos: Pos): Gem? = if (pos.isInBounds()) cells[indexOf(pos)] else null

    fun with(
        pos: Pos,
        gem: Gem?,
    ): Board {
        require(pos.isInBounds()) { "$pos is outside the board" }
        val next = cells.toMutableList()
        next[indexOf(pos)] = gem
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
