package dev.kesatria.gemmatch

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.BoardLayout
import dev.kesatria.gemmatch.core.Game

class GameSurfaceView(
    context: Context,
    store: GameStore,
) : SurfaceView(context),
    SurfaceHolder.Callback {
    private val controller =
        GameController(store.loadGame() ?: Game.new(System.nanoTime()), BoardRenderer(), store, store.loadBest())
    private var loop: GameLoop? = null

    @Volatile private var layout: BoardLayout? = null

    init {
        holder.addCallback(this)
    }

    fun onBack(): Boolean = controller.onBack()

    /** Saves the game to disk, waiting until it is written. */
    fun save() = controller.save()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        controller.onTouch(TouchEvent(event.actionMasked, event.x, event.y))
        return true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        controller.resetIdle()
        loop = GameLoop(holder, ::advance, ::render).also { it.start() }
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        val margin = MARGIN_DP * resources.displayMetrics.density
        val cell = (minOf(width, height) - 2 * margin) / BOARD_SIZE
        layout = BoardLayout((width - cell * BOARD_SIZE) / 2, (height - cell * BOARD_SIZE) / 2, cell)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        loop?.stopAndJoin()
        loop = null
    }

    private fun advance(seconds: Float) = controller.update(seconds, layout)

    private fun render(canvas: Canvas) {
        val current = layout ?: return
        controller.render(canvas, current)
    }

    private companion object {
        const val MARGIN_DP = 16f
    }
}
