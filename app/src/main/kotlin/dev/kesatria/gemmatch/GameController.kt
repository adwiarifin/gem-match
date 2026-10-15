package dev.kesatria.gemmatch

import android.graphics.Canvas
import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.BoardLayout
import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.Move
import dev.kesatria.gemmatch.core.Placed
import dev.kesatria.gemmatch.core.Point
import dev.kesatria.gemmatch.core.Pos
import dev.kesatria.gemmatch.core.findValidMoves
import dev.kesatria.gemmatch.core.previewDrag
import dev.kesatria.gemmatch.core.SwapOutcome
import dev.kesatria.gemmatch.core.resolveSwipe
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    private val store: GameStore,
    initialBest: Int,
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
    private var liftedPos: Pos? = null
    private var previewTo: Pos? = null
    private var shownScore = game.score
    private var savedBest = maxOf(initialBest, game.score)

    @Volatile private var bestScore = savedBest

    /** The newest resolved game, including a move whose animation is still playing. What gets saved. */
    @Volatile private var latest = game

    @Volatile private var dialogOpen = false

    @Volatile private var idleResetRequested = true
    private var idleSeconds = 0f
    private var hint: Move? = null

    fun onTouch(event: TouchEvent) {
        touches.add(event)
    }

    /** Starts the idle timer from zero. Called when the app comes back, so time away never counts. */
    fun resetIdle() {
        idleResetRequested = true
    }

    /** Writes the current game and best score to disk and waits. Called from the UI thread when the app is paused. */
    fun save() = store.flush(latest, bestScore)

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
            idleResetRequested = true
            if (layout != null) handle(event, layout)
        }
        advanceTimeline(seconds)
        updateIdleHint(seconds)
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
        when (event.action) {
            ACTION_DOWN -> beginTouch(event, layout)
            ACTION_MOVE -> followFinger(Point(event.x, event.y), layout)
            else -> endTouch(event, layout)
        }
    }

    /** Touching a gem picks it up: it grows and sits on top. Nothing is decided until release. */
    private fun beginTouch(
        event: TouchEvent,
        layout: BoardLayout,
    ) {
        buttonPressed = hud.isOnNewGameButton(layout, event.x, event.y)
        val start = if (!buttonPressed && timeline == null) Point(event.x, event.y) else null
        gestureStart = start
        liftedPos = start?.let { layout.cellAt(it.x, it.y) }
        liftedPos?.let { pos ->
            scene[pos]?.let {
                it.scale = LIFT_SCALE
                scene.raised = it
            }
        }
    }

    /** The held gem follows the finger along its axis and the neighbour slides the other way: a swap that is not final yet. */
    private fun followFinger(
        point: Point,
        layout: BoardLayout,
    ) {
        val start = gestureStart ?: return
        val held = liftedPos ?: return
        restPreview(held)
        val preview = previewDrag(layout, start, point) ?: return
        val to = preview.to ?: return
        val dRow = (to.row - held.row) * preview.progress
        val dCol = (to.col - held.col) * preview.progress
        scene[held]?.let {
            it.row = held.row + dRow
            it.col = held.col + dCol
        }
        scene[to]?.let {
            // The neighbour sidesteps a little as it passes, so both gems stay visible at the half-way point.
            val sidestep = (SIDESTEP * sin(PI * preview.progress)).toFloat()
            it.row = to.row - dRow + if (to.row == held.row) sidestep else 0f
            it.col = to.col - dCol + if (to.col == held.col) sidestep else 0f
        }
        previewTo = to
    }

    private fun restPreview(held: Pos) {
        scene[held]?.let {
            it.row = held.row.toFloat()
            it.col = held.col.toFloat()
        }
        previewTo?.let { to ->
            scene[to]?.let {
                it.row = to.row.toFloat()
                it.col = to.col.toFloat()
            }
        }
        previewTo = null
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

        val start = gestureStart
        val held = liftedPos
        if (start != null && held != null) {
            val point = Point(event.x, event.y)
            if (released) followFinger(point, layout)
            val move = if (released) resolveSwipe(layout, start, point) else null
            if (move != null) startMove(move) else timeline = Timeline(listOf(SettleAnimation(held, previewTo)))
        }
        gestureStart = null
        liftedPos = null
        previewTo = null
    }

    private fun startMove(move: Move) {
        when (val outcome = game.trySwap(move.from, move.to)) {
            SwapOutcome.Rejected -> {
                timeline = Timeline(listOf(SwapAnimation(move.from, move.to), SwapAnimation(move.from, move.to)))
            }

            is SwapOutcome.Accepted -> {
                timeline = Timeline.forSteps(outcome.steps, ::addPoints)
                pending = outcome.game
                latest = outcome.game
                store.saveGame(outcome.game)
            }
        }
    }

    /**
     * After [HINT_AFTER_SECONDS] of a settled board with no touch, one valid move pulses. The timer counts
     * game-loop time only, so it never runs in the background. Any touch, animation or dialog clears it.
     */
    private fun updateIdleHint(seconds: Float) {
        val idle = timeline == null && !dialogOpen && gestureStart == null && !buttonPressed
        if (!idle || idleResetRequested) {
            clearHint()
            idleSeconds = 0f
            idleResetRequested = false
            return
        }
        idleSeconds += seconds
        if (idleSeconds < HINT_AFTER_SECONDS) return
        val move = hint ?: findValidMoves(game.board).randomOrNull()?.also { hint = it } ?: return
        val phase = (idleSeconds - HINT_AFTER_SECONDS) / HINT_PULSE_SECONDS
        val scale = 1f + HINT_PULSE * (0.5f - 0.5f * cos(2 * PI * phase).toFloat())
        scene[move.from]?.scale = scale
        scene[move.to]?.scale = scale
    }

    private fun clearHint() {
        hint?.let { move ->
            scene[move.from]?.scale = 1f
            scene[move.to]?.scale = 1f
        }
        hint = null
    }

    private fun addPoints(points: Int) {
        shownScore += points
        bestScore = maxOf(bestScore, shownScore)
    }

    /** The old gems shrink away, then the new board rains in from the top. Score resets at once; best stays. */
    private fun startNewGame() {
        scene.load((pending ?: game).board)
        game = Game.new(newSeed())
        pending = game
        latest = game
        store.saveGame(game)
        saveBestIfImproved()
        shownScore = 0
        val allCells = (0 until BOARD_SIZE).flatMap { row -> (0 until BOARD_SIZE).map { col -> Pos(row, col) } }.toSet()
        val arrivals = allCells.sortedWith(compareBy({ it.col }, { it.row })).mapNotNull { pos -> game.board[pos]?.let { Placed(it, pos) } }
        timeline = Timeline(listOf(ClearAnimation(allCells, 0) {}, SpawnAnimation(arrivals)))
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
            scene.raised = null
            pending = null
            timeline = null
            saveBestIfImproved()
        }
    }

    private fun saveBestIfImproved() {
        if (bestScore > savedBest) {
            savedBest = bestScore
            store.saveBest(bestScore)
        }
    }

    companion object {
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val ACTION_MOVE = 2
        private const val ACTION_BACK = -1
        private const val LIFT_SCALE = 1.2f
        private const val SIDESTEP = 0.22f
        private const val HINT_AFTER_SECONDS = 8f
        private const val HINT_PULSE_SECONDS = 1f
        private const val HINT_PULSE = 0.2f
    }
}
