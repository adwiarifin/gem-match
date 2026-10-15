package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals

class GemTest {
    @Test
    fun gemCountMatchesTheRuleConstant() {
        assertEquals(GEM_TYPES, Gem.entries.size)
    }
}
