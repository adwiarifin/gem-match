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
 * Game state, input and animation in one place. [onTouch] is the only method called from the UI
 * thread; everything else runs on the game-loop thread.
 */
class GameController(
    private var game: Game,
    private val renderer: BoardRenderer,
) {
    private val scene = Scene().also { it.load(game.board) }
    private val touches = ConcurrentLinkedQueue<TouchEvent>()
    private var timeline: Timeline? = null
    private var pending: Game? = null
    private var gestureStart: Point? = null

    fun onTouch(event: TouchEvent) {
        touches.add(event)
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
    ) = renderer.draw(canvas, layout, scene)

    private fun handle(
        event: TouchEvent,
        layout: BoardLayout,
    ) {
        val point = Point(event.x, event.y)
        when (event.action) {
            ACTION_DOWN -> gestureStart = if (timeline == null) point else null
            ACTION_MOVE -> gestureStart?.let { start -> resolveSwipe(layout, start, point)?.let(::startMove) }
            else -> gestureStart = null
        }
    }

    private fun startMove(move: Move) {
        gestureStart = null
        when (val outcome = game.trySwap(move.from, move.to)) {
            SwapOutcome.Rejected -> {
                timeline = Timeline(listOf(SwapAnimation(move.from, move.to), SwapAnimation(move.from, move.to)))
            }

            is SwapOutcome.Accepted -> {
                timeline = Timeline.forSteps(outcome.steps)
                pending = outcome.game
            }
        }
    }

    private fun advanceTimeline(seconds: Float) {
        val running = timeline ?: return
        running.advance(seconds, scene)
        if (running.isFinished) {
            pending?.let {
                game = it
                scene.load(it.board)
            }
            pending = null
            timeline = null
        }
    }

    companion object {
        const val ACTION_DOWN = 0
        const val ACTION_MOVE = 2
    }
}
