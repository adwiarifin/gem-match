package dev.kesatria.gemmatch

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private var loop: RenderThread? = null

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        loop = RenderThread(holder).also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        loop?.stopAndJoin()
        loop = null
    }

    private class RenderThread(private val holder: SurfaceHolder) : Thread("render") {
        @Volatile private var running = true
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 64f
            textAlign = Paint.Align.CENTER
        }

        override fun run() {
            while (running) {
                val canvas = holder.lockCanvas() ?: continue
                try {
                    canvas.drawColor(Color.rgb(24, 24, 48))
                    canvas.drawText("Gem Match", canvas.width / 2f, canvas.height / 2f, paint)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
                sleep(FRAME_MS)
            }
        }

        fun stopAndJoin() {
            running = false
            join()
        }
    }

    private companion object {
        const val FRAME_MS = 16L
    }
}
