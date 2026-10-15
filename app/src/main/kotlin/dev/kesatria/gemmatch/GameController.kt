package dev.kesatria.gemmatch

import android.graphics.Canvas
import dev.kesatria.gemmatch.core.BoardLayout
import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.Move
import dev.kesatria.gemmatch.core.Point
import dev.kesatria.gemmatch.core.SwapOutcome
import dev.kesatria.gemmatch.core.resolveSwipe
import java.util.concurrent.ConcurrentLinkedQueue

/** A touch copied off the UI thread: MotionEvent objects are recycled, so only plain values cross over. */
class TouchEvent(
    val action: Int,
    val x: Float,
    val y: Float,
)

/**
 * Game state, input and animation in one place. [onTouch] and [onBack] are the only methods called
 * from the UI thread; everything else runs on the game-loop thread.
 */
class GameController(
    private var game: Game,
    private val renderer: BoardRenderer,
    private val newSeed: () -> Long = System::nanoTime,
) {
    private val scene = Scene().also { it.load(game.board) }
    private val hud = Hud()
    private val dialog = ConfirmDialog()
    private val touches = ConcurrentLinkedQueue<TouchEvent>()
    private var timeline: Timeline? = null
    private var pending: Game? = null
    private var gestureStart: Point? = null
    private var buttonPressed = false
    private var shownScore = game.score
    private var bestScore = game.score

    @Volatile private var dialogOpen = false

    fun onTouch(event: TouchEvent) {
        touches.add(event)
    }

    /** True if the back press was used to close the dialog; false means the app should handle it. */
    fun onBack(): Boolean {
        if (!dialogOpen) return false
        touches.add(TouchEvent(ACTION_BACK, 0f, 0f))
        return true
    }

    fun update(
        seconds: Float,
        layout: BoardLayout?,
    ) {
        while (true) {
            val event = touches.poll() ?: break
            if (layout != null) handle(event, layout)
        }
        advanceTimeline(seconds)
    }

    fun render(
        canvas: Canvas,
        layout: BoardLayout,
    ) {
        renderer.draw(canvas, layout, scene)
        hud.draw(canvas, layout, shownScore, bestScore)
        if (dialogOpen) dialog.draw(canvas, layout)
    }

    private fun handle(
        event: TouchEvent,
        layout: BoardLayout,
    ) {
        when {
            event.action == ACTION_BACK -> dialogOpen = false
            dialogOpen -> handleDialogTouch(event, layout)
            else -> handleBoardTouch(event, layout)
        }
    }

    private fun handleDialogTouch(
        event: TouchEvent,
        layout: BoardLayout,
    ) {
        if (event.action != ACTION_UP) return
        when (dialog.choiceAt(layout, event.x, event.y)) {
            DialogChoice.CONFIRM -> {
                startNewGame()
                dialogOpen = false
            }

            DialogChoice.CANCEL -> dialogOpen = false

            null -> Unit
        }
    }

    private fun handleBoardTouch(
        event: TouchEvent,
        layout: BoardLayout,
    ) {
        val point = Point(event.x, event.y)
        when (event.action) {
            ACTION_DOWN -> {
                buttonPressed = hud.isOnNewGameButton(layout, event.x, event.y)
                gestureStart = if (!buttonPressed && timeline == null) point else null
            }

            ACTION_MOVE -> gestureStart?.let { start -> resolveSwipe(layout, start, point)?.let(::startMove) }

            else -> {
                if (event.action == ACTION_UP && buttonPressed && hud.isOnNewGameButton(layout, event.x, event.y)) {
                    dialogOpen = true
                }
                buttonPressed = false
                gestureStart = null
            }
        }
    }

    private fun startMove(move: Move) {
        gestureStart = null
        when (val outcome = game.trySwap(move.from, move.to)) {
            SwapOutcome.Rejected -> {
                timeline = Timeline(listOf(SwapAnimation(move.from, move.to), SwapAnimation(move.from, move.to)))
            }

            is SwapOutcome.Accepted -> {
                timeline = Timeline.forSteps(outcome.steps, ::addPoints)
                pending = outcome.game
            }
        }
    }

    private fun addPoints(points: Int) {
        shownScore += points
        bestScore = maxOf(bestScore, shownScore)
    }

    private fun startNewGame() {
        game = Game.new(newSeed())
        scene.load(game.board)
        timeline = null
        pending = null
        shownScore = 0
    }

    private fun advanceTimeline(seconds: Float) {
        val running = timeline ?: return
        running.advance(seconds, scene)
        if (running.isFinished) {
            pending?.let {
                game = it
                scene.load(it.board)
                shownScore = it.score
                bestScore = maxOf(bestScore, shownScore)
            }
            pending = null
            timeline = null
        }
    }

    companion object {
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val ACTION_MOVE = 2
        private const val ACTION_BACK = -1
    }
}
