package dev.kesatria.gemmatch.core

import kotlin.math.abs
import kotlin.math.floor

/** Share of a cell the finger must travel before a swipe is recognised. Tunable by feel. */
const val SWIPE_THRESHOLD = 0.3f

/** A touch position in pixels. */
data class Point(
    val x: Float,
    val y: Float,
)

/** Where the board sits on screen: top-left corner and the side of one square cell, in pixels. */
data class BoardLayout(
    val originX: Float,
    val originY: Float,
    val cellSize: Float,
) {
    init {
        require(cellSize > 0f) { "cellSize must be positive, was $cellSize" }
    }

    /** The cell under a point, or null outside the board. The top-left edge is inside, the far edge is not. */
    fun cellAt(
        x: Float,
        y: Float,
    ): Pos? {
        val pos = Pos(floor((y - originY) / cellSize).toInt(), floor((x - originX) / cellSize).toInt())
        return if (pos.isInBounds()) pos else null
    }
}

/**
 * Turns a drag into a swap request: the cell touched first plus one of four directions, picked by the
 * longer axis once the finger has moved [threshold] of a cell. Returns null until the drag is clear,
 * for a touch off the board, for a perfect diagonal, and for a swipe off the board edge.
 */
fun resolveSwipe(
    layout: BoardLayout,
    down: Point,
    current: Point,
    threshold: Float = SWIPE_THRESHOLD,
): Move? {
    val from = layout.cellAt(down.x, down.y)
    val dx = current.x - down.x
    val dy = current.y - down.y
    val travelled = maxOf(abs(dx), abs(dy))
    val clear = abs(dx) != abs(dy) && travelled >= threshold * layout.cellSize

    val to =
        when {
            from == null || !clear -> null
            abs(dx) > abs(dy) -> Pos(from.row, from.col + if (dx > 0) 1 else -1)
            else -> Pos(from.row + if (dy > 0) 1 else -1, from.col)
        }
    return if (from != null && to != null && to.isInBounds()) Move(from, to) else null
}
