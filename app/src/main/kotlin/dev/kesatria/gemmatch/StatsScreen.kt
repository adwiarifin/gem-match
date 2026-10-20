package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.BoardLayout
import dev.kesatria.gemmatch.core.FinishedRun
import dev.kesatria.gemmatch.core.Lifetime

/** One line of the stats screen's list. */
sealed interface StatsItem {
    data class Heading(
        val text: String,
    ) : StatsItem

    data class Line(
        val label: String,
        val value: String,
    ) : StatsItem

    data class Message(
        val text: String,
    ) : StatsItem
}

private const val MOVES_AND_TIME = " moves · "

private fun average(
    total: Long,
    count: Long,
): Long = if (count == 0L) 0L else total / count

/**
 * What the stats screen lists: the lifetime records, totals and averages, then the last runs, newest first. Lifetime
 * counts every run ever, including ones the list has dropped, so the two can disagree on purpose.
 */
fun statsItems(
    lifetime: Lifetime,
    runs: List<FinishedRun>,
): List<StatsItem> {
    val best = StatsItem.Line("Best score", formatCount(lifetime.bestScore.toLong()))
    if (lifetime.runs == 0L && runs.isEmpty()) {
        return listOf(StatsItem.Heading("Records"), best, StatsItem.Message("No finished runs yet"))
    }
    val made = lifetime.created.values.sum()
    val fired = lifetime.fired.values.sum()
    val block =
        listOf(
            StatsItem.Heading("Records"),
            best,
            StatsItem.Line("Longest cascade", formatCount(lifetime.bestCascade.toLong())),
            StatsItem.Line("Biggest move score", formatCount(lifetime.bestMoveScore.toLong())),
            StatsItem.Line("Biggest match", formatCount(lifetime.bestMatch.toLong())),
            StatsItem.Line("Longest run (total time)", formatDuration(lifetime.bestWallMs)),
            StatsItem.Line("Longest run (active time)", formatDuration(lifetime.bestActiveMs)),
            StatsItem.Heading("Totals"),
            StatsItem.Line("Runs played", formatCount(lifetime.runs)),
            StatsItem.Line("Moves", formatCount(lifetime.moves)),
            StatsItem.Line("Gems cleared", formatCount(lifetime.gems)),
            StatsItem.Line("Special gems made", formatCount(made)),
            StatsItem.Line("Special gems fired", formatCount(fired)),
            StatsItem.Line("Active time", formatDuration(lifetime.activeMs)),
            StatsItem.Line("Total time", formatDuration(lifetime.wallMs)),
            StatsItem.Heading("Averages"),
            StatsItem.Line("Score per run", formatCount(average(lifetime.score, lifetime.runs))),
            StatsItem.Line("Score per move", formatCount(average(lifetime.score, lifetime.moves))),
            StatsItem.Line("Moves per run", formatCount(average(lifetime.moves, lifetime.runs))),
        )
    return if (runs.isEmpty()) {
        block + StatsItem.Message("No runs in the list")
    } else {
        block + StatsItem.Heading("Last runs") + runs.map { it.toLine() }
    }
}

private fun FinishedRun.toLine(): StatsItem.Line {
    val summary = formatCount(stats.moves.toLong()) + MOVES_AND_TIME + formatDuration(activeMs)
    return StatsItem.Line(formatCount(score.toLong()), summary)
}

enum class StatsAction { CLOSE }

/** Where the stats screen sits: a tall panel around the board with a list and a Close button pinned under it. */
class StatsLayout(
    layout: BoardLayout,
) {
    private val cell = layout.cellSize
    private val cx = layout.originX + cell * BOARD_SIZE / 2
    private val top = layout.originY - cell * PANEL_MARGIN
    private val bottom = layout.originY + cell * (BOARD_SIZE + PANEL_MARGIN)

    val panel = Box(cx - cell * PANEL_WIDTH / 2, top, cx + cell * PANEL_WIDTH / 2, bottom)
    val list =
        Box(
            panel.left + cell * LIST_INSET,
            top + cell * HEADER_HEIGHT,
            panel.right - cell * LIST_INSET,
            bottom - cell * FOOTER_HEIGHT,
        )
    val close =
        Box(
            cx - cell * BUTTON_WIDTH / 2,
            bottom - cell * (BUTTON_HEIGHT + BUTTON_INSET),
            cx + cell * BUTTON_WIDTH / 2,
            bottom - cell * BUTTON_INSET,
        )
    val rowHeight = cell * ROW_HEIGHT
    val titleY = top + cell * TITLE_Y

    fun isOnClose(
        x: Float,
        y: Float,
    ): Boolean = close.contains(x, y)

    companion object {
        const val PANEL_WIDTH = 7.6f
        const val PANEL_MARGIN = 1.8f
        const val HEADER_HEIGHT = 1.5f
        const val FOOTER_HEIGHT = 1.9f
        const val LIST_INSET = 0.5f
        const val ROW_HEIGHT = 0.62f
        const val BUTTON_WIDTH = 3.2f
        const val BUTTON_HEIGHT = 1.0f
        const val BUTTON_INSET = 0.45f
        const val TITLE_Y = 0.95f
    }
}

/** The stats screen's state: closed or open on a list of items, scrolled by dragging. It draws nothing itself. */
class StatsScreen {
    @Volatile
    var isOpen = false
        private set

    var items = emptyList<StatsItem>()
        private set

    val scroll = ScrollList()

    fun open(history: RunHistory) {
        items = statsItems(history.lifetime(), history.runs())
        scroll.reset()
        isOpen = true
    }

    fun close() {
        isOpen = false
    }

    /** Dragging scrolls the list and never closes the screen; lifting a finger that did not drag on Close does. */
    fun onTouch(
        action: Int,
        x: Float,
        y: Float,
        layout: BoardLayout,
    ): StatsAction? {
        scroll.touch(action, y, layout.cellSize * DRAG_SLOP)
        val tapped = action == GameController.ACTION_UP && !scroll.dragged
        return if (tapped && StatsLayout(layout).isOnClose(x, y)) StatsAction.CLOSE else null
    }

    private companion object {
        const val DRAG_SLOP = 0.15f
    }
}
