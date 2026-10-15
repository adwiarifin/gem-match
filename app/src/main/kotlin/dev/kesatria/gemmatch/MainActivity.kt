package dev.kesatria.gemmatch

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.window.OnBackInvokedDispatcher

class MainActivity : Activity() {
    private lateinit var gameView: GameSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameView = GameSurfaceView(this, GameStore(filesDir))
        setContentView(gameView)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) { goBack() }
        }
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            goBack()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onPause() {
        gameView.save()
        super.onPause()
    }

    /** Back closes the New Game dialog if it is open, otherwise leaves the app. */
    private fun goBack() {
        if (!gameView.onBack()) finish()
    }
}
