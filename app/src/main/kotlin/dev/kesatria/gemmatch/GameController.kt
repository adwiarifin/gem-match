package dev.kesatria.gemmatch

import android.graphics.Canvas
import dev.kesatria.gemmatch.core.BoardLayout
import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.Point
import java.util.concurrent.ConcurrentLinkedQueue

/** A touch copied off the UI thread: MotionEvent objects are recycled, so only plain values cross over. */
class TouchEvent(
    val action: Int,
    val x: Float,
    val y: Float,
)

/**
 * Wires input, animation and the game session together. [onTouch] and [onBack] are the only methods called
 * from the UI thread (plus [save] and [resetIdle] on pause and resume); everything else runs on the game-loop thread.
 */
class GameController(
    game: Game,
    private val renderer: BoardRenderer,
    store: GameStore,
    initialBest: Int,
    newSeed: () -> Long = System::nanoTime,
) {
    private val session = GameSession(game, store, initialBest, newSeed)
    private val scene = Scene().also { it.load(session.board) }
    private val hud = Hud()
    private val dialog = ConfirmDialog()
    private val drag = DragGesture(scene)
    private val idleHint = IdleHint(scene)
    private val touches = ConcurrentLinkedQueue<TouchEvent>()
    private var timeline: Timeline? = null
    private var buttonPressed = false

    @Volatile private var dialogOpen = false

    fun onTouch(event: TouchEvent) {
        touches.add(event)
    }

    /** Starts the idle timer from zero. Called when the app comes back, so time away never counts. */
    fun resetIdle() {
        idleHint.touched()
    }

    /** Writes the current game and best score to disk and waits. Called from the UI thread when the app is paused. */
    fun save() = session.save()

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
            idleHint.touched()
            if (layout != null) handle(event, layout)
        }
        scene.advanceClock(seconds)
        timeline?.let { running ->
            running.advance(seconds, scene)
            if (running.isFinished) {
                session.finish(scene)
                scene.raised = null
                timeline = null
            }
        }
        val settled = timeline == null && !dialogOpen && !drag.isActive && !buttonPressed
        idleHint.tick(seconds, session.board, settled)
    }

    fun render(
        canvas: Canvas,
        layout: BoardLayout,
    ) {
        renderer.draw(canvas, layout, scene)
        hud.draw(canvas, layout, session.shownScore, session.bestScore)
        if (dialogOpen) dialog.draw(canvas, layout)
    }

    private fun handle(
        event: TouchEvent,
        layout: BoardLayout,
    ) {
        when {
            event.action == ACTION_BACK -> dialogOpen = false
            dialogOpen -> handleDialogTouch(event, layout)
            event.action == ACTION_DOWN -> beginTouch(event, layout)
            event.action == ACTION_MOVE -> drag.follow(Point(event.x, event.y), layout)
            else -> endTouch(event, layout)
        }
    }

    private fun handleDialogTouch(
        event: TouchEvent,
        layout: BoardLayout,
    ) {
        if (event.action != ACTION_UP) return
        when (dialog.choiceAt(layout, event.x, event.y)) {
            DialogChoice.CONFIRM -> {
                timeline = session.newGame(scene)
                dialogOpen = false
            }

            DialogChoice.CANCEL -> dialogOpen = false

            null -> Unit
        }
    }

    /**
     * Touching a gem picks it up. A touch on the New Game button picks up nothing. A touch while an animation plays
     * skips to its end and picks up nothing; the next touch plays. A spring-back or a rejected swap is not skipped.
     */
    private fun beginTouch(
        event: TouchEvent,
        layout: BoardLayout,
    ) {
        buttonPressed = hud.isOnNewGameButton(layout, event.x, event.y)
        if (shouldSkipOnTouch(buttonPressed, session.isMovePlaying)) timeline?.skip(scene)
        val canPlay = !buttonPressed && timeline == null
        drag.begin(if (canPlay) Point(event.x, event.y) else null, layout)
    }

    /**
     * Release decides. Past the commit point the swap is attempted, carrying on from where the gems
     * are. Short of it, the gems spring back. A cancelled touch always springs back.
     */
    private fun endTouch(
        event: TouchEvent,
        layout: BoardLayout,
    ) {
        val released = event.action == ACTION_UP
        if (released && buttonPressed && hud.isOnNewGameButton(layout, event.x, event.y)) dialogOpen = true
        buttonPressed = false

        if (drag.isActive) {
            val move = if (released) drag.release(Point(event.x, event.y), layout) else null
            timeline = if (move != null) session.play(move) else drag.settle()?.let { Timeline(listOf(it)) }
            drag.clear()
        }
    }

    companion object {
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val ACTION_MOVE = 2
        private const val ACTION_BACK = -1
    }
}

/**
 * Whether a touch that lands while an animation plays should skip to its end: only for an accepted move or a new
 * game, and never for the New Game button. A rejected swap and a spring-back are left to play out.
 */
internal fun shouldSkipOnTouch(
    onNewGameButton: Boolean,
    movePlaying: Boolean,
): Boolean = movePlaying && !onNewGameButton
