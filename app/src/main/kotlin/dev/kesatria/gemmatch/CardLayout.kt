package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.BoardLayout

/** A rectangle in pixels, with no Android types so that the card's geometry can be tested on the JVM. */
data class Box(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2
    val centerY: Float get() = (top + bottom) / 2

    fun contains(
        x: Float,
        y: Float,
    ): Boolean = x >= left && x < right && y >= top && y < bottom
}

enum class CardChoice { PLAY_AGAIN, STATS }

/**
 * When the game-over card appears: a short beat on the dead board, then a fade. Time comes from the game loop's step,
 * like every other animation.
 */
class CardReveal {
    private var elapsed = 0f

    val alpha: Float get() = ((elapsed - BEAT_SECONDS) / FADE_SECONDS).coerceIn(0f, 1f)

    val isVisible: Boolean get() = alpha > 0f

    fun advance(seconds: Float) {
        elapsed += seconds
    }

    fun hide() {
        elapsed = 0f
    }

    companion object {
        /** How long the dead board shows on its own before the card fades in. */
        const val BEAT_SECONDS = 0.6f
        const val FADE_SECONDS = 0.3f
    }
}

/** Where the game-over card sits, relative to the board: a header, a list, and two buttons pinned below it. */
class CardLayout(
    layout: BoardLayout,
) {
    private val cell = layout.cellSize
    private val cx = layout.originX + cell * BOARD_SIZE / 2
    private val cy = layout.originY + cell * BOARD_SIZE / 2

    private val width = cell * PANEL_WIDTH
    private val height = cell * PANEL_HEIGHT
    private val buttonWidth = cell * BUTTON_WIDTH
    private val buttonHeight = cell * BUTTON_HEIGHT
    val panel = at(cx - width / 2, cy - height / 2, width, height)
    val list =
        at(
            panel.left + cell * LIST_INSET,
            panel.top + cell * HEADER_HEIGHT,
            panel.width - cell * LIST_INSET * 2,
            panel.height - cell * (HEADER_HEIGHT + FOOTER_HEIGHT),
        )
    val playAgain = at(cx - cell * BUTTON_GAP / 2 - buttonWidth, buttonTop(), buttonWidth, buttonHeight)
    val stats = at(cx + cell * BUTTON_GAP / 2, buttonTop(), buttonWidth, buttonHeight)
    val rowHeight = cell * ROW_HEIGHT

    /** Baselines of the header lines. */
    val titleY = panel.top + cell * TITLE_Y
    val scoreY = panel.top + cell * SCORE_Y
    val bestY = panel.top + cell * BEST_Y

    fun choiceAt(
        x: Float,
        y: Float,
    ): CardChoice? =
        when {
            playAgain.contains(x, y) -> CardChoice.PLAY_AGAIN
            stats.contains(x, y) -> CardChoice.STATS
            else -> null
        }

    private fun buttonTop() = cy + height / 2 - buttonHeight - cell * BUTTON_INSET

    private fun at(
        left: Float,
        top: Float,
        width: Float,
        height: Float,
    ) = Box(left, top, left + width, top + height)

    companion object {
        const val PANEL_WIDTH = 7.2f
        const val PANEL_HEIGHT = 9.6f
        const val HEADER_HEIGHT = 3.3f
        const val FOOTER_HEIGHT = 2.0f
        const val LIST_INSET = 0.5f
        const val ROW_HEIGHT = 0.62f
        const val BUTTON_WIDTH = 3.2f
        const val BUTTON_HEIGHT = 1.0f
        const val BUTTON_GAP = 0.3f
        const val BUTTON_INSET = 0.45f
        const val TITLE_Y = 0.95f
        const val SCORE_Y = 1.95f
        const val BEST_Y = 2.65f
    }
}
