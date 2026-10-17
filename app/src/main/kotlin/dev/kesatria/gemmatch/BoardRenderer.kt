package dev.kesatria.gemmatch

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.BoardLayout

/** Draws a [Scene] onto a canvas. Owns its paints so nothing is allocated per frame. */
class BoardRenderer {
    private val background = Paint().apply { color = Palette.BACKGROUND }
    private val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Palette.PANEL }
    private val effectRenderer = EffectRenderer()
    private val panelRect = RectF()
    private val noticeRect = RectF()
    private val noticeFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val noticeText =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

    fun draw(
        canvas: Canvas,
        layout: BoardLayout,
        scene: Scene,
    ) {
        canvas.drawPaint(background)

        val side = layout.cellSize * BOARD_SIZE
        panelRect.set(layout.originX, layout.originY, layout.originX + side, layout.originY + side)
        canvas.drawRoundRect(panelRect, layout.cellSize * PANEL_CORNER, layout.cellSize * PANEL_CORNER, panel)

        canvas.save()
        canvas.clipRect(panelRect)
        val glow = shimmerGlow(scene.clock)
        scene.forEach { if (it !== scene.raised) drawSprite(canvas, layout, it, glow) }
        scene.raised?.let { drawSprite(canvas, layout, it, glow) }
        effectRenderer.draw(canvas, layout, scene.effects)
        canvas.restore()

        scene.message?.let { drawNotice(canvas, layout, it, scene.messageAlpha) }
    }

    private fun drawSprite(
        canvas: Canvas,
        layout: BoardLayout,
        sprite: Sprite,
        glow: Float,
    ) {
        val cx = layout.originX + (sprite.col + sprite.dx + HALF) * layout.cellSize
        val cy = layout.originY + (sprite.row + HALF) * layout.cellSize
        canvas.save()
        canvas.translate(cx, cy)
        val radius = layout.cellSize * GEM_RADIUS * sprite.scale
        canvas.scale(radius, radius)
        GemShapes.draw(canvas, sprite)
        SpecialOverlays.draw(canvas, sprite, glow)
        canvas.restore()
    }

    private fun drawNotice(
        canvas: Canvas,
        layout: BoardLayout,
        text: String,
        alpha: Float,
    ) {
        val cell = layout.cellSize
        noticeText.textSize = cell * NOTICE_TEXT
        val width = noticeText.measureText(text) + cell * NOTICE_PADDING
        val cx = layout.originX + cell * BOARD_SIZE / 2
        val cy = layout.originY + cell * BOARD_SIZE / 2
        val half = cell * NOTICE_HEIGHT / 2
        noticeRect.set(cx - width / 2, cy - half, cx + width / 2, cy + half)
        noticeFill.color = Palette.NOTICE_FILL
        noticeFill.alpha = (alpha * NOTICE_ALPHA).toInt()
        noticeText.alpha = (alpha * OPAQUE).toInt()
        canvas.drawRoundRect(noticeRect, half, half, noticeFill)
        val baseline = cy - (noticeText.fontMetrics.ascent + noticeText.fontMetrics.descent) / 2
        canvas.drawText(text, cx, baseline, noticeText)
    }

    private companion object {
        const val HALF = 0.5f
        const val GEM_RADIUS = 0.38f
        const val PANEL_CORNER = 0.2f
        const val NOTICE_TEXT = 0.5f
        const val NOTICE_PADDING = 1.2f
        const val NOTICE_HEIGHT = 1.1f
        const val NOTICE_ALPHA = 220
        const val OPAQUE = 255
    }
}
