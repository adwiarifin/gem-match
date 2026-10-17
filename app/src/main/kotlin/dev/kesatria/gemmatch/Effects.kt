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
}

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
                Special.LINE_ROW -> drawBeam(canvas, layout, effect, horizontal = true)
                Special.LINE_COL -> drawBeam(canvas, layout, effect, horizontal = false)
                Special.CROSS -> {
                    drawBeam(canvas, layout, effect, horizontal = true)
                    drawBeam(canvas, layout, effect, horizontal = false)
                }
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
        val reach = effect.cells.maxOf { distance(it, effect.origin) }.coerceAtLeast(1)
        for (cell in effect.cells) {
            val arrives = distance(cell, effect.origin).toFloat() / reach * ARRIVAL_SPREAD
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
        val (cx, cy) = centre(layout, effect.origin)
        stroke.strokeWidth = layout.cellSize * RING_WIDTH
        stroke.alpha = (OPAQUE * (1f - effect.progress)).toInt()
        canvas.drawCircle(cx, cy, layout.cellSize * (RING_START + RING_GROWTH * effect.progress), stroke)
    }

    private fun drawBeam(
        canvas: Canvas,
        layout: BoardLayout,
        effect: Effect,
        horizontal: Boolean,
    ) {
        val (cx, cy) = centre(layout, effect.origin)
        val reach = layout.cellSize * BOARD_SIZE * effect.progress
        val half = layout.cellSize * BEAM_HALF
        beam.alpha = (BEAM_ALPHA * (1f - effect.progress * BEAM_FADE)).toInt()
        if (horizontal) {
            rect.set(cx - reach, cy - half, cx + reach, cy + half)
        } else {
            rect.set(cx - half, cy - reach, cx + half, cy + reach)
        }
        canvas.drawRoundRect(rect, half, half, beam)
    }

    private fun drawStreaks(
        canvas: Canvas,
        layout: BoardLayout,
        effect: Effect,
    ) {
        val (cx, cy) = centre(layout, effect.origin)
        val travelled = (effect.progress / STREAK_TRAVEL).coerceAtMost(1f)
        stroke.strokeWidth = layout.cellSize * STREAK_WIDTH
        stroke.alpha = (OPAQUE * (1f - effect.progress * STREAK_FADE)).toInt()
        for (cell in effect.cells) {
            val (tx, ty) = centre(layout, cell)
            canvas.drawLine(cx, cy, cx + (tx - cx) * travelled, cy + (ty - cy) * travelled, stroke)
        }
    }

    private fun centre(
        layout: BoardLayout,
        pos: Pos,
    ) = Pair(layout.originX + (pos.col + HALF) * layout.cellSize, layout.originY + (pos.row + HALF) * layout.cellSize)

    private fun distance(
        a: Pos,
        b: Pos,
    ) = max(abs(a.row - b.row), abs(a.col - b.col))

    private companion object {
        const val HALF = 0.5f
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
