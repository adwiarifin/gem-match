package dev.kesatria.gemmatch

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.BoardLayout

/** Draws a rounded button with centred text. Shared by the HUD and the confirm dialog. */
class ButtonPainter {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }

    fun draw(
        canvas: Canvas,
        rect: RectF,
        label: String,
        color: Int,
        textSize: Float,
    ) {
        fill.color = color
        val radius = rect.height() / 2
        canvas.drawRoundRect(rect, radius, radius, fill)
        text.textSize = textSize
        val baseline = rect.centerY() - (text.fontMetrics.ascent + text.fontMetrics.descent) / 2
        canvas.drawText(label, rect.centerX(), baseline, text)
    }
}

/** Score, best score and the New Game button, positioned relative to the board. */
class Hud {
    private val buttons = ButtonPainter()
    private val label =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Palette.LABEL_TEXT }
    private val value =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            isFakeBoldText = true
        }
    private val button = RectF()

    fun draw(
        canvas: Canvas,
        layout: BoardLayout,
        score: Int,
        best: Int,
    ) {
        val cell = layout.cellSize
        val left = layout.originX
        val right = layout.originX + cell * BOARD_SIZE
        val labelY = layout.originY - cell * LABEL_ABOVE
        val valueY = layout.originY - cell * VALUE_ABOVE

        label.textSize = cell * LABEL_SIZE
        value.textSize = cell * VALUE_SIZE
        label.textAlign = Paint.Align.LEFT
        value.textAlign = Paint.Align.LEFT
        canvas.drawText("SCORE", left, labelY, label)
        canvas.drawText(score.toString(), left, valueY, value)
        label.textAlign = Paint.Align.RIGHT
        value.textAlign = Paint.Align.RIGHT
        canvas.drawText("BEST", right, labelY, label)
        canvas.drawText(best.toString(), right, valueY, value)

        buttons.draw(canvas, buttonRect(layout), "New game", BUTTON_COLOR, cell * BUTTON_TEXT)
    }

    fun isOnNewGameButton(
        layout: BoardLayout,
        x: Float,
        y: Float,
    ): Boolean = buttonRect(layout).contains(x, y)

    private fun buttonRect(layout: BoardLayout): RectF {
        val cell = layout.cellSize
        val centreX = layout.originX + cell * BOARD_SIZE / 2
        val top = layout.originY + cell * BOARD_SIZE + cell * BUTTON_GAP
        val halfWidth = cell * BUTTON_WIDTH / 2
        button.set(centreX - halfWidth, top, centreX + halfWidth, top + cell * BUTTON_HEIGHT)
        return button
    }

    private companion object {
        const val LABEL_ABOVE = 0.95f
        const val VALUE_ABOVE = 0.25f
        const val LABEL_SIZE = 0.3f
        const val VALUE_SIZE = 0.7f
        const val BUTTON_GAP = 0.6f
        const val BUTTON_WIDTH = 3.4f
        const val BUTTON_HEIGHT = 1.0f
        const val BUTTON_TEXT = 0.4f
        val BUTTON_COLOR = Color.rgb(60, 60, 110)
    }
}
