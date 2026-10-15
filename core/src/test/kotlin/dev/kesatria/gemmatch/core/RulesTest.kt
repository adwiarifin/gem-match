package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals

class RulesTest {
    @Test
    fun boardIsEightByEight() {
        assertEquals(8, BOARD_SIZE)
    }

    @Test
    fun thereAreSevenGemTypes() {
        assertEquals(7, GEM_TYPES)
    }

    @Test
    fun minimumMatchIsThree() {
        assertEquals(3, MIN_MATCH)
    }
}
