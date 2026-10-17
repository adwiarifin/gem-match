package dev.kesatria.gemmatch.core

import kotlin.math.abs

data class Pos(
    val row: Int,
    val col: Int,
) {
    fun isAdjacentTo(other: Pos): Boolean = abs(row - other.row) + abs(col - other.col) == 1

    fun isInBounds(): Boolean = row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE
}

/** Board order: row by row, left to right. Used wherever the rules need a deterministic order. */
val BOARD_ORDER: Comparator<Pos> = compareBy({ it.row }, { it.col })
