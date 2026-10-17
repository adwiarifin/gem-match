package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PieceTest {
    @Test
    fun colourGemHasNoGem() {
        val piece = Piece(null, Special.COLOUR)

        assertNull(piece.gem)
        assertEquals(Special.COLOUR, piece.special)
    }

    @Test
    fun colourGemWithAGemIsRejected() {
        assertFailsWith<IllegalArgumentException> { Piece(Gem.RED, Special.COLOUR) }
    }

    @Test
    fun everyOtherSpecialNeedsAGem() {
        for (special in Special.entries.filter { it != Special.COLOUR }) {
            assertFailsWith<IllegalArgumentException> { Piece(null, special) }
            assertEquals(special, Piece(Gem.BLUE, special).special)
        }
    }

    @Test
    fun plainPieceNeedsAGem() {
        assertFailsWith<IllegalArgumentException> { Piece(null, null) }
        assertNull(Piece(Gem.GREEN).special)
    }

    @Test
    fun onlyPlainPiecesAreNotSpecial() {
        assertTrue(!Piece(Gem.RED).isSpecial)
        assertTrue(Piece(Gem.RED, Special.BLAST).isSpecial)
        assertTrue(Piece(null, Special.COLOUR).isSpecial)
    }
}
