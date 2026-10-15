package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.Pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class SceneTest {
    private val board = Game.new(1L).board
    private val scene = Scene().also { it.load(board) }

    @Test
    fun loadPutsOneSpriteOnEveryCellOfTheBoard() {
        assertSceneShows(board, scene)
    }

    @Test
    fun loadingAgainThrowsAwayTheHeldGemAndAnyDragDamage() {
        val before = assertNotNull(scene[Pos(0, 0)])
        scene.raised = before
        before.scale = 2f

        scene.load(board)

        assertNotSame(before, scene[Pos(0, 0)])
        assertSceneShows(board, scene)
    }

    @Test
    fun loadingAlsoClearsAnyNoticeLeftOverFromAnAbandonedAnimation() {
        scene.message = "No moves left - reshuffling"
        scene.messageAlpha = 1f

        scene.load(board)

        assertNull(scene.message)
    }

    @Test
    fun removeTakesTheSpriteOutOfItsSlot() {
        val sprite = scene.remove(Pos(4, 4))

        assertNotNull(sprite)
        assertNull(scene[Pos(4, 4)])
    }

    @Test
    fun swapSlotsExchangesTwoSprites() {
        val first = scene[Pos(1, 1)]
        val second = scene[Pos(1, 2)]

        scene.swapSlots(Pos(1, 1), Pos(1, 2))

        assertSame(second, scene[Pos(1, 1)])
        assertSame(first, scene[Pos(1, 2)])
    }

    @Test
    fun forEachVisitsEverySpriteAndSkipsEmptySlots() {
        scene.remove(Pos(0, 0))
        var seen = 0

        scene.forEach { seen++ }

        assertEquals(BOARD_SIZE * BOARD_SIZE - 1, seen)
    }
}
