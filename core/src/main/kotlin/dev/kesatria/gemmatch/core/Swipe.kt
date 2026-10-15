package dev.kesatria.gemmatch.core

import kotlin.math.abs
import kotlin.math.floor

/** Share of a cell the finger must travel for a release to count as a move. Tunable by feel. */
const val SWIPE_THRESHOLD = 0.35f

/** Movement smaller than this share of a cell is treated as holding still, so the preview does not twitch. */
const val DRAG_DEAD_ZONE = 0.08f

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
 * Where a drag stands right now: the cell touched first, the neighbour it is heading for (null while
 * undecided, on a perfect diagonal, or toward the board edge) and how far it has gone, 0 to 1 cell.
 */
data class DragPreview(
    val from: Pos,
    val to: Pos?,
    val progress: Float,
)

/** Reads a drag in progress, so the UI can show the gem following the finger. Null if the touch began off the board. */
fun previewDrag(
    layout: BoardLayout,
    down: Point,
    current: Point,
): DragPreview? {
    val from = layout.cellAt(down.x, down.y) ?: return null
    val dx = (current.x - down.x) / layout.cellSize
    val dy = (current.y - down.y) / layout.cellSize
    val travelled = maxOf(abs(dx), abs(dy))

    val to =
        when {
            travelled < DRAG_DEAD_ZONE || abs(dx) == abs(dy) -> null
            abs(dx) > abs(dy) -> Pos(from.row, from.col + if (dx > 0) 1 else -1)
            else -> Pos(from.row + if (dy > 0) 1 else -1, from.col)
        }
    val heading = to?.takeIf { it.isInBounds() }
    return DragPreview(from, heading, if (heading != null) minOf(1f, travelled) else 0f)
}

/**
 * Turns a finished drag into a swap request: the cell touched first plus the neighbour the drag
 * pointed at, once the finger travelled at least [threshold] of a cell. Null for a short drag, a
 * touch off the board, a perfect diagonal, and a swipe off the board edge.
 */
fun resolveSwipe(
    layout: BoardLayout,
    down: Point,
    current: Point,
    threshold: Float = SWIPE_THRESHOLD,
): Move? {
    val preview = previewDrag(layout, down, current) ?: return null
    val to = preview.to
    return if (to != null && preview.progress >= threshold) Move(preview.from, to) else null
}
