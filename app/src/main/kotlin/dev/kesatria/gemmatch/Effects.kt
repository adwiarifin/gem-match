package dev.kesatria.gemmatch

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.BoardLayout
import dev.kesatria.gemmatch.core.Pos
import dev.kesatria.gemmatch.core.Special
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/** One special firing: where from, what kind, which cells it takes, and how far the animation has got (0 to 1). */
class Effect(
    val origin: Pos,
    val special: Special,
    val cells: Set<Pos>,
) {
    var progress = 0f

    /** The rows a row line beams along: its own row, or three together for two row lines. */
    val beamRows: List<Int> =
        if (special == Special.LINE_ROW) indexesWhere({ it.col == origin.col }) { it.row } else listOf(origin.row)

    /** The columns a column line beams along: its own column, or three together for two column lines. */
    val beamColumns: List<Int> =
        if (special == Special.LINE_COL) indexesWhere({ it.row == origin.row }) { it.col } else listOf(origin.col)

    private fun indexesWhere(
        on: (Pos) -> Boolean,
        index: (Pos) -> Int,
    ): List<Int> = cells.filter(on).map(index).distinct()

    /** How far the farthest taken cell is from the origin, in cells (at least 1). */
    val reach: Int = (cells.maxOfOrNull { cheb(it, origin) } ?: 0).coerceAtLeast(1)
}

private fun cheb(
    a: Pos,
    b: Pos,
) = max(abs(a.row - b.row), abs(a.col - b.col))

/** Draws the effects of a [Scene] over its gems. Owns its paints so nothing is allocated per frame. */
class EffectRenderer {
    private val rect = RectF()
    private val flash = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeCap = Paint.Cap.ROUND
        }
    private val beam = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    fun draw(
        canvas: Canvas,
        layout: BoardLayout,
        effects: List<Effect>,
    ) {
        for (effect in effects) {
            drawTaken(canvas, layout, effect)
            when (effect.special) {
                Special.BLAST -> drawRing(canvas, layout, effect)
                Special.LINE_ROW -> drawBeams(canvas, layout, effect, horizontal = true)
                Special.LINE_COL -> drawBeams(canvas, layout, effect, horizontal = false)
                Special.CROSS -> {
                    drawBeams(canvas, layout, effect, horizontal = true)
                    drawBeams(canvas, layout, effect, horizontal = false)
                }
                Special.X_SHORT, Special.X_FULL -> drawDiagonals(canvas, layout, effect)
                Special.COLOUR -> drawStreaks(canvas, layout, effect)
            }
        }
    }

    /** A white flash on every gem the effect takes, reaching the far ones a little later than the near ones. */
    private fun drawTaken(
        canvas: Canvas,
        layout: BoardLayout,
        effect: Effect,
    ) {
        for (cell in effect.cells) {
            val arrives = cheb(cell, effect.origin).toFloat() / effect.reach * ARRIVAL_SPREAD
            val local = ((effect.progress - arrives) / (1f - ARRIVAL_SPREAD)).coerceIn(0f, 1f)
            if (local <= 0f) continue
            flash.alpha = (FLASH_ALPHA * sin(Math.PI * local)).toInt()
            val size = layout.cellSize
            val left = layout.originX + cell.col * size
            val top = layout.originY + cell.row * size
            rect.set(left + size * INSET, top + size * INSET, left + size * (1 - INSET), top + size * (1 - INSET))
            canvas.drawRoundRect(rect, size * FLASH_CORNER, size * FLASH_CORNER, flash)
        }
    }

    private fun drawRing(
        canvas: Canvas,
        layout: BoardLayout,
        effect: Effect,
    ) {
        val cx = centreX(layout, effect.origin)
        val cy = centreY(layout, effect.origin)
        stroke.strokeWidth = layout.cellSize * RING_WIDTH
        stroke.alpha = (OPAQUE * (1f - effect.progress)).toInt()
        canvas.drawCircle(cx, cy, layout.cellSize * effect.reach * (RING_START + RING_GROWTH * effect.progress), stroke)
    }

    private fun drawBeams(
        canvas: Canvas,
        layout: BoardLayout,
        effect: Effect,
        horizontal: Boolean,
    ) {
        val reach = layout.cellSize * BOARD_SIZE * effect.progress
        val half = layout.cellSize * BEAM_HALF
        beam.alpha = (BEAM_ALPHA * (1f - effect.progress * BEAM_FADE)).toInt()
        val lines = if (horizontal) effect.beamRows else effect.beamColumns
        for (line in lines) {
            if (horizontal) {
                val cy = layout.originY + (line + HALF) * layout.cellSize
                val cx = centreX(layout, effect.origin)
                rect.set(cx - reach, cy - half, cx + reach, cy + half)
            } else {
                val cx = layout.originX + (line + HALF) * layout.cellSize
                val cy = centreY(layout, effect.origin)
                rect.set(cx - half, cy - reach, cx + half, cy + reach)
            }
            canvas.drawRoundRect(rect, half, half, beam)
        }
    }

    /** Two beams turned 45 degrees, each growing out of the origin to the farthest cell the effect takes. */
    private fun drawDiagonals(
        canvas: Canvas,
        layout: BoardLayout,
        effect: Effect,
    ) {
        val cx = centreX(layout, effect.origin)
        val cy = centreY(layout, effect.origin)
        val reach = layout.cellSize * effect.reach * DIAGONAL_STRETCH * effect.progress
        val half = layout.cellSize * BEAM_HALF
        beam.alpha = (BEAM_ALPHA * (1f - effect.progress * BEAM_FADE)).toInt()
        for (degrees in DIAGONAL_TURNS) {
            canvas.save()
            canvas.rotate(degrees, cx, cy)
            rect.set(cx - reach, cy - half, cx + reach, cy + half)
            canvas.drawRoundRect(rect, half, half, beam)
            canvas.restore()
        }
    }

    private fun drawStreaks(
        canvas: Canvas,
        layout: BoardLayout,
        effect: Effect,
    ) {
        val cx = centreX(layout, effect.origin)
        val cy = centreY(layout, effect.origin)
        val travelled = (effect.progress / STREAK_TRAVEL).coerceAtMost(1f)
        stroke.strokeWidth = layout.cellSize * STREAK_WIDTH
        stroke.alpha = (OPAQUE * (1f - effect.progress * STREAK_FADE)).toInt()
        for (cell in effect.cells) {
            val tx = centreX(layout, cell)
            val ty = centreY(layout, cell)
            canvas.drawLine(cx, cy, cx + (tx - cx) * travelled, cy + (ty - cy) * travelled, stroke)
        }
    }

    private fun centreX(
        layout: BoardLayout,
        pos: Pos,
    ) = layout.originX + (pos.col + HALF) * layout.cellSize

    private fun centreY(
        layout: BoardLayout,
        pos: Pos,
    ) = layout.originY + (pos.row + HALF) * layout.cellSize

    private companion object {
        const val HALF = 0.5f
        const val DIAGONAL_STRETCH = 1.4142135f
        val DIAGONAL_TURNS = floatArrayOf(45f, -45f)
        const val OPAQUE = 255
        const val ARRIVAL_SPREAD = 0.5f
        const val FLASH_ALPHA = 170
        const val INSET = 0.08f
        const val FLASH_CORNER = 0.2f
        const val RING_WIDTH = 0.16f
        const val RING_START = 0.3f
        const val RING_GROWTH = 1.5f
        const val BEAM_HALF = 0.16f
        const val BEAM_ALPHA = 230
        const val BEAM_FADE = 0.6f
        const val STREAK_WIDTH = 0.07f
        const val STREAK_TRAVEL = 0.6f
        const val STREAK_FADE = 0.7f
    }
}
