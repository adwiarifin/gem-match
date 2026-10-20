package dev.kesatria.gemmatch

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import dev.kesatria.gemmatch.core.BoardLayout

/** Draws an open [StatsScreen]: a panel over the dimmed game, a scrolling list, and a Close button pinned below it. */
class StatsPainter {
    private val buttons = ButtonPainter()
    private val rect = RectF()
    private val dim = Paint().apply { color = Color.argb(DIM_ALPHA, 0, 0, 0) }
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Palette.PANEL }
    private val title =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    private val heading =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Palette.LABEL_TEXT
            isFakeBoldText = true
        }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Palette.BODY_TEXT }
    private val value =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
            isFakeBoldText = true
        }
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Palette.LABEL_TEXT }

    fun draw(
        canvas: Canvas,
        layout: BoardLayout,
        screen: StatsScreen,
    ) {
        val geometry = StatsLayout(layout)
        val cell = layout.cellSize
        canvas.drawPaint(dim)
        rect.set(geometry.panel.left, geometry.panel.top, geometry.panel.right, geometry.panel.bottom)
        canvas.drawRoundRect(rect, cell * CORNER, cell * CORNER, panelPaint)
        title.textSize = cell * TITLE_SIZE
        canvas.drawText("Stats", geometry.panel.centerX, geometry.titleY, title)

        drawList(canvas, geometry, screen, cell)
        rect.set(geometry.close.left, geometry.close.top, geometry.close.right, geometry.close.bottom)
        buttons.draw(canvas, rect, "Close", CLOSE_COLOUR, cell * BUTTON_TEXT)
    }

    private fun drawList(
        canvas: Canvas,
        geometry: StatsLayout,
        screen: StatsScreen,
        cell: Float,
    ) {
        val list = geometry.list
        val items = screen.items
        screen.scroll.fit(items.size * geometry.rowHeight, list.height)
        label.textSize = cell * ROW_TEXT
        value.textSize = cell * ROW_TEXT
        heading.textSize = cell * HEADING_TEXT
        canvas.save()
        canvas.clipRect(list.left, list.top, list.right, list.bottom)
        for (index in items.indices) {
            val top = list.top + index * geometry.rowHeight - screen.scroll.offset
            if (top + geometry.rowHeight < list.top || top > list.bottom) continue
            val baseline = top + geometry.rowHeight * BASELINE
            when (val item = items[index]) {
                is StatsItem.Heading -> canvas.drawText(item.text, list.left, baseline, heading)
                is StatsItem.Message -> canvas.drawText(item.text, list.left, baseline, label)
                is StatsItem.Line -> {
                    canvas.drawText(item.label, list.left, baseline, label)
                    canvas.drawText(item.value, list.right, baseline, value)
                }
            }
        }
        canvas.restore()
        val content = items.size * geometry.rowHeight
        if (screen.scroll.canScroll) drawScrollBar(canvas, geometry, screen.scroll.offset, content)
    }

    private fun drawScrollBar(
        canvas: Canvas,
        geometry: StatsLayout,
        offset: Float,
        contentHeight: Float,
    ) {
        val list = geometry.list
        val thumb = list.height * list.height / contentHeight
        val top = list.top + (list.height - thumb) * offset / (contentHeight - list.height)
        val x = list.right + geometry.rowHeight * BAR_GAP
        rect.set(x, top, x + geometry.rowHeight * BAR_WIDTH, top + thumb)
        canvas.drawRoundRect(rect, rect.width() / 2, rect.width() / 2, bar)
    }

    private companion object {
        const val DIM_ALPHA = 200
        const val CORNER = 0.4f
        const val TITLE_SIZE = 0.55f
        const val ROW_TEXT = 0.34f
        const val HEADING_TEXT = 0.3f
        const val BUTTON_TEXT = 0.38f
        const val BASELINE = 0.72f
        const val BAR_GAP = 0.25f
        const val BAR_WIDTH = 0.12f
        val CLOSE_COLOUR = Color.rgb(30, 136, 229)
    }
}
