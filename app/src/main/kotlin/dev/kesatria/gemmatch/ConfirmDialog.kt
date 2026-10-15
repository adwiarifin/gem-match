package dev.kesatria.gemmatch

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.BoardLayout

enum class DialogChoice { CANCEL, CONFIRM }

/**
 * "Start a new game?" drawn on the canvas, above everything else, so it matches the game's own
 * style. While it is open the controller sends it every touch and the board gets none.
 */
class ConfirmDialog {
    private val buttons = ButtonPainter()
    private val dim = Paint().apply { color = Color.argb(DIM_ALPHA, 0, 0, 0) }
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(36, 36, 72) }
    private val title =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    private val body =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(190, 190, 220)
            textAlign = Paint.Align.CENTER
        }
    private val panel = RectF()
    private val cancel = RectF()
    private val confirm = RectF()

    fun draw(
        canvas: Canvas,
        layout: BoardLayout,
    ) {
        place(layout)
        val cell = layout.cellSize
        canvas.drawPaint(dim)
        canvas.drawRoundRect(panel, cell * PANEL_CORNER, cell * PANEL_CORNER, panelPaint)

        title.textSize = cell * TITLE_SIZE
        body.textSize = cell * BODY_SIZE
        canvas.drawText("Start a new game?", panel.centerX(), panel.top + cell * TITLE_Y, title)
        canvas.drawText("Your current game will be lost.", panel.centerX(), panel.top + cell * BODY_Y, body)
        buttons.draw(canvas, cancel, "Cancel", CANCEL_COLOR, cell * BUTTON_TEXT)
        buttons.draw(canvas, confirm, "New game", CONFIRM_COLOR, cell * BUTTON_TEXT)
    }

    fun choiceAt(
        layout: BoardLayout,
        x: Float,
        y: Float,
    ): DialogChoice? {
        place(layout)
        return when {
            cancel.contains(x, y) -> DialogChoice.CANCEL
            confirm.contains(x, y) -> DialogChoice.CONFIRM
            else -> null
        }
    }

    private fun place(layout: BoardLayout) {
        val cell = layout.cellSize
        val cx = layout.originX + cell * BOARD_SIZE / 2
        val cy = layout.originY + cell * BOARD_SIZE / 2
        panel.set(cx - cell * PANEL_WIDTH / 2, cy - cell * PANEL_HEIGHT / 2, cx + cell * PANEL_WIDTH / 2, cy + cell * PANEL_HEIGHT / 2)
        val top = panel.bottom - cell * (BUTTON_HEIGHT + BUTTON_INSET)
        val width = cell * BUTTON_WIDTH
        val gap = cell * BUTTON_GAP
        cancel.set(cx - gap / 2 - width, top, cx - gap / 2, top + cell * BUTTON_HEIGHT)
        confirm.set(cx + gap / 2, top, cx + gap / 2 + width, top + cell * BUTTON_HEIGHT)
    }

    private companion object {
        const val DIM_ALPHA = 170
        const val PANEL_WIDTH = 6.6f
        const val PANEL_HEIGHT = 3.8f
        const val PANEL_CORNER = 0.4f
        const val TITLE_SIZE = 0.55f
        const val BODY_SIZE = 0.36f
        const val TITLE_Y = 0.95f
        const val BODY_Y = 1.6f
        const val BUTTON_WIDTH = 2.8f
        const val BUTTON_HEIGHT = 1.0f
        const val BUTTON_GAP = 0.3f
        const val BUTTON_INSET = 0.4f
        const val BUTTON_TEXT = 0.38f
        val CANCEL_COLOR = Color.rgb(60, 60, 110)
        val CONFIRM_COLOR = Color.rgb(30, 136, 229)
    }
}
