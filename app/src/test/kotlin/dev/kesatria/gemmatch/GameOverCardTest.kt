package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BoardLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameOverCardTest {
    private val layout = BoardLayout(originX = 40f, originY = 300f, cellSize = 100f)

    @Test
    fun theCardIsHiddenUntilTheBeatHasPassedThenFadesIn() {
        val reveal = CardReveal()
        assertFalse(reveal.isVisible)

        reveal.advance(0.59f)
        assertFalse(reveal.isVisible, "still in the beat")
        assertEquals(0f, reveal.alpha)

        reveal.advance(0.01f + 0.15f)
        assertTrue(reveal.isVisible)
        assertTrue(reveal.alpha > 0f && reveal.alpha < 1f, "half way through the fade, was ${reveal.alpha}")

        reveal.advance(1f)
        assertEquals(1f, reveal.alpha)
    }

    @Test
    fun hidingStartsTheBeatOverForTheNextGame() {
        val reveal = CardReveal()
        reveal.advance(5f)
        assertTrue(reveal.isVisible)

        reveal.hide()

        assertFalse(reveal.isVisible)
        reveal.advance(0.3f)
        assertFalse(reveal.isVisible)
    }

    @Test
    fun theButtonsSitPinnedBelowTheListInsideThePanel() {
        val card = CardLayout(layout)

        assertTrue(card.playAgain.top >= card.list.bottom, "the list does not run under the buttons")
        assertTrue(card.stats.top >= card.list.bottom)
        assertTrue(card.playAgain.bottom <= card.panel.bottom)
        assertTrue(card.list.top >= card.panel.top && card.list.bottom <= card.panel.bottom)
        assertTrue(card.playAgain.right <= card.stats.left, "the two buttons do not overlap")
        assertTrue(card.playAgain.left >= card.panel.left && card.stats.right <= card.panel.right)
    }

    @Test
    fun thePanelIsCentredOnTheBoardAndFitsTheScreenWidthOfTheBoard() {
        val card = CardLayout(layout)
        val boardCentreX = layout.originX + layout.cellSize * 4

        assertEquals(boardCentreX, (card.panel.left + card.panel.right) / 2, 0.01f)
        assertTrue(card.panel.width <= layout.cellSize * 8, "no wider than the board")
    }

    @Test
    fun aTouchOnAButtonPicksThatButtonAndAnywhereElsePicksNothing() {
        val card = CardLayout(layout)

        assertEquals(CardChoice.PLAY_AGAIN, card.choiceAt(card.playAgain.centerX, card.playAgain.centerY))
        assertEquals(CardChoice.STATS, card.choiceAt(card.stats.centerX, card.stats.centerY))
        assertNull(card.choiceAt(card.list.centerX, card.list.centerY))
        assertNull(card.choiceAt(card.panel.left - 5f, card.panel.top - 5f))
    }

    @Test
    fun theListCanHoldSeveralRowsAndRowHeightScalesWithTheBoard() {
        val small = CardLayout(BoardLayout(0f, 0f, 50f))
        val big = CardLayout(BoardLayout(0f, 0f, 100f))

        assertEquals(small.rowHeight * 2, big.rowHeight, 0.01f)
        assertTrue(big.list.height >= big.rowHeight * 4, "at least four rows are visible at once")
    }
}
