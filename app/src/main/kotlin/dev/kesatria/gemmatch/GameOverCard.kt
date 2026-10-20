package dev.kesatria.gemmatch

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import dev.kesatria.gemmatch.core.BoardLayout
import dev.kesatria.gemmatch.core.FinishedRun

/**
 * What the player sees when no move is left: the dead board dims, and after a short beat a card fades in with the
 * score, the best, every stat of the run in a list that scrolls, and two buttons pinned under it. While it shows,
 * the controller sends it every touch. Text is built once in [show], never per frame.
 */
class GameOverCard {
    private val reveal = CardReveal()
    private val scroll = ScrollList()
    private val buttons = ButtonPainter()
    private var run: FinishedRun? = null
    private var rows = emptyList<StatRow>()
    private var scoreText = ""
    private var bestText = ""
    private var newBest = false

    private var touchStartY = 0f
    private var touchLastY = 0f
    private var dragged = false
    private val rect = RectF()
    private val dim = Paint().apply { color = Color.argb(DIM_ALPHA, 0, 0, 0) }
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Palette.PANEL }
    private val title =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    private val score = Paint(title)
    private val best = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Palette.BODY_TEXT }
    private val value =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
            isFakeBoldText = true
        }
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Palette.LABEL_TEXT }

    val isVisible: Boolean get() = reveal.isVisible

    private fun show(
        finished: FinishedRun,
        bestScore: Int,
    ) {
        run = finished
        rows = statRows(finished)
        scoreText = formatCount(finished.score.toLong())
        bestText = "Best ${formatCount(bestScore.toLong())}"
        newBest = finished.newBest
        reveal.hide()
        scroll.reset()
    }

    private fun hide() {
        run = null
        reveal.hide()
    }

    /**
     * Follows the session: shows [finished] once, hides the card when there is none, and keeps time. Called every step.
     */
    fun sync(
        finished: FinishedRun?,
        bestScore: Int,
        seconds: Float,
    ) {
        if (finished == null) {
            hide()
        } else {
            if (run !== finished) show(finished, bestScore)
            reveal.advance(seconds)
        }
    }

    /**
     * One touch event while the card shows. Dragging scrolls the list and never picks a button; lifting a finger that
     * did not drag picks the button under it.
     */
    fun onTouch(
        action: Int,
        x: Float,
        y: Float,
        layout: BoardLayout,
    ): CardChoice? {
        when (action) {
            GameController.ACTION_DOWN -> {
                touchStartY = y
                touchLastY = y
                dragged = false
            }

            GameController.ACTION_MOVE -> {
                if (kotlin.math.abs(y - touchStartY) > layout.cellSize * DRAG_SLOP) dragged = true
                scroll.scrollBy(touchLastY - y)
                touchLastY = y
            }
        }
        return if (action == GameController.ACTION_UP && !dragged) CardLayout(layout).choiceAt(x, y) else null
    }

    fun draw(
        canvas: Canvas,
        layout: BoardLayout,
    ) {
        val geometry = CardLayout(layout)
        val cell = layout.cellSize
        val alpha = (reveal.alpha * OPAQUE).toInt()
        val layer = canvas.saveLayerAlpha(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), alpha)
        canvas.drawPaint(dim)
        rect.set(geometry.panel.left, geometry.panel.top, geometry.panel.right, geometry.panel.bottom)
        canvas.drawRoundRect(rect, cell * CORNER, cell * CORNER, panelPaint)

        title.textSize = cell * TITLE_SIZE
        score.textSize = cell * SCORE_SIZE
        best.textSize = cell * BEST_SIZE
        best.color = if (newBest) NEW_BEST_COLOUR else Palette.LABEL_TEXT
        best.isFakeBoldText = newBest
        canvas.drawText("Game over", geometry.panel.centerX, geometry.titleY, title)
        canvas.drawText(scoreText, geometry.panel.centerX, geometry.scoreY, score)
        canvas.drawText(if (newBest) "New best!" else bestText, geometry.panel.centerX, geometry.bestY, best)

        drawList(canvas, geometry, cell)
        rect.set(geometry.playAgain.left, geometry.playAgain.top, geometry.playAgain.right, geometry.playAgain.bottom)
        buttons.draw(canvas, rect, "Play again", PLAY_COLOUR, cell * BUTTON_TEXT)
        rect.set(geometry.stats.left, geometry.stats.top, geometry.stats.right, geometry.stats.bottom)
        buttons.draw(canvas, rect, "Stats", STATS_COLOUR, cell * BUTTON_TEXT)
        canvas.restoreToCount(layer)
    }

    private fun drawList(
        canvas: Canvas,
        geometry: CardLayout,
        cell: Float,
    ) {
        val list = geometry.list
        scroll.fit(rows.size * geometry.rowHeight, list.height)
        label.textSize = cell * ROW_TEXT
        value.textSize = cell * ROW_TEXT
        canvas.save()
        canvas.clipRect(list.left, list.top, list.right, list.bottom)
        for (index in rows.indices) {
            val top = list.top + index * geometry.rowHeight - scroll.offset
            if (top + geometry.rowHeight < list.top || top > list.bottom) continue
            val baseline = top + geometry.rowHeight * BASELINE
            canvas.drawText(rows[index].label, list.left, baseline, label)
            canvas.drawText(rows[index].value, list.right, baseline, value)
        }
        canvas.restore()
        if (scroll.canScroll) drawScrollBar(canvas, geometry, rows.size * geometry.rowHeight)
    }

    private fun drawScrollBar(
        canvas: Canvas,
        geometry: CardLayout,
        contentHeight: Float,
    ) {
        val list = geometry.list
        val thumb = list.height * list.height / contentHeight
        val travel = list.height - thumb
        val top = list.top + travel * scroll.offset / (contentHeight - list.height)
        val x = list.right + geometry.rowHeight * BAR_GAP
        rect.set(x, top, x + geometry.rowHeight * BAR_WIDTH, top + thumb)
        canvas.drawRoundRect(rect, rect.width() / 2, rect.width() / 2, bar)
    }

    private companion object {
        const val DIM_ALPHA = 170
        const val OPAQUE = 255
        const val CORNER = 0.4f
        const val TITLE_SIZE = 0.55f
        const val SCORE_SIZE = 0.95f
        const val BEST_SIZE = 0.4f
        const val ROW_TEXT = 0.34f
        const val BUTTON_TEXT = 0.38f
        const val BASELINE = 0.72f
        const val DRAG_SLOP = 0.15f
        const val BAR_GAP = 0.25f
        const val BAR_WIDTH = 0.12f
        val NEW_BEST_COLOUR = Color.rgb(253, 216, 53)
        val PLAY_COLOUR = Color.rgb(30, 136, 229)
        val STATS_COLOUR = Color.rgb(60, 60, 110)
    }
}
