package dev.kesatria.gemmatch

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SkipPolicyTest {
    @Test
    fun aTapSkipsOnlyWhenAMoveIsPlayingAndTheTapIsNotOnTheNewGameButton() {
        assertTrue(shouldSkipOnTouch(onNewGameButton = false, movePlaying = true))
        assertFalse(shouldSkipOnTouch(onNewGameButton = false, movePlaying = false), "a rejected swap or a spring-back")
        assertFalse(shouldSkipOnTouch(onNewGameButton = true, movePlaying = true), "the New Game button is not a skip")
        assertFalse(shouldSkipOnTouch(onNewGameButton = true, movePlaying = false))
    }
}
