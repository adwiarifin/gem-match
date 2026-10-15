package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BoardLayout
import dev.kesatria.gemmatch.core.Move
import dev.kesatria.gemmatch.core.Point
import dev.kesatria.gemmatch.core.Pos
import dev.kesatria.gemmatch.core.previewDrag
import dev.kesatria.gemmatch.core.resolveSwipe
import kotlin.math.PI
import kotlin.math.sin

/**
 * The finger-down-to-finger-up life of one touch on the board. Touching a gem picks it up: it grows and
 * sits on top. While dragging, the held gem follows the finger along its axis and the neighbour slides the
 * other way, a swap that is not final yet. Nothing is decided until release.
 */
class DragGesture(
    private val scene: Scene,
) {
    private var start: Point? = null
    private var held: Pos? = null
    private var previewTo: Pos? = null

    /** True from a touch on the board until it ends, even if the finger landed outside the grid. */
    val isActive: Boolean get() = start != null

    /** Starts a gesture at [point], or none when [point] is null (a button press, or an animation is playing). */
    fun begin(
        point: Point?,
        layout: BoardLayout,
    ) {
        start = point
        held = point?.let { layout.cellAt(it.x, it.y) }
        held?.let { pos ->
            scene[pos]?.let {
                it.scale = LIFT_SCALE
                scene.raised = it
            }
        }
    }

    fun follow(
        point: Point,
        layout: BoardLayout,
    ) {
        val from = start
        val pos = held
        if (from != null && pos != null) {
            rest(pos)
            val preview = previewDrag(layout, from, point)
            val to = preview?.to
            if (preview != null && to != null) {
                shift(pos, to, preview.progress)
                previewTo = to
            }
        }
    }

    /** Lets go at [point]: the move to try, or null when the swipe is short of the commit point. */
    fun release(
        point: Point,
        layout: BoardLayout,
    ): Move? {
        val from = start ?: return null
        follow(point, layout)
        return resolveSwipe(layout, from, point)
    }

    /** The gems spring back from wherever the drag left them. */
    fun settle(): Animation? = held?.let { SettleAnimation(it, previewTo) }

    fun clear() {
        start = null
        held = null
        previewTo = null
    }

    private fun shift(
        pos: Pos,
        to: Pos,
        progress: Float,
    ) {
        val dRow = (to.row - pos.row) * progress
        val dCol = (to.col - pos.col) * progress
        scene[pos]?.let {
            it.row = pos.row + dRow
            it.col = pos.col + dCol
        }
        scene[to]?.let {
            // The neighbour sidesteps a little as it passes, so both gems stay visible at the half-way point.
            val sidestep = (SIDESTEP * sin(PI * progress)).toFloat()
            it.row = to.row - dRow + if (to.row == pos.row) sidestep else 0f
            it.col = to.col - dCol + if (to.col == pos.col) sidestep else 0f
        }
    }

    private fun rest(pos: Pos) {
        scene[pos]?.let {
            it.row = pos.row.toFloat()
            it.col = pos.col.toFloat()
        }
        previewTo?.let { to ->
            scene[to]?.let {
                it.row = to.row.toFloat()
                it.col = to.col.toFloat()
            }
        }
        previewTo = null
    }

    private companion object {
        const val LIFT_SCALE = 1.2f
        const val SIDESTEP = 0.22f
    }
}
