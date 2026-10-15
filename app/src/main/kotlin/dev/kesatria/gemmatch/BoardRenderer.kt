package dev.kesatria.gemmatch

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.BoardLayout
import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.Gem
import dev.kesatria.gemmatch.core.Pos

/** Draws a [Game] onto a canvas. Owns its paints so nothing is allocated per frame. */
class BoardRenderer {
    private val background = Paint().apply { color = Color.rgb(24, 24, 48) }
    private val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(36, 36, 72) }
    private val gemPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panelRect = RectF()

    fun draw(
        canvas: Canvas,
        layout: BoardLayout,
        game: Game,
    ) {
        canvas.drawPaint(background)

        val side = layout.cellSize * BOARD_SIZE
        panelRect.set(layout.originX, layout.originY, layout.originX + side, layout.originY + side)
        canvas.drawRoundRect(panelRect, layout.cellSize * PANEL_CORNER, layout.cellSize * PANEL_CORNER, panel)

        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val gem = game.board[Pos(row, col)] ?: continue
                gemPaint.color = colorOf(gem)
                val cx = layout.originX + (col + HALF) * layout.cellSize
                val cy = layout.originY + (row + HALF) * layout.cellSize
                canvas.drawCircle(cx, cy, layout.cellSize * GEM_RADIUS, gemPaint)
            }
        }
    }

    private fun colorOf(gem: Gem): Int =
        when (gem) {
            Gem.RED -> Color.rgb(229, 57, 53)
            Gem.ORANGE -> Color.rgb(251, 140, 0)
            Gem.YELLOW -> Color.rgb(253, 216, 53)
            Gem.GREEN -> Color.rgb(67, 160, 71)
            Gem.BLUE -> Color.rgb(30, 136, 229)
            Gem.PURPLE -> Color.rgb(142, 36, 170)
            Gem.WHITE -> Color.rgb(236, 239, 241)
        }

    private companion object {
        const val HALF = 0.5f
        const val GEM_RADIUS = 0.38f
        const val PANEL_CORNER = 0.2f
    }
}
