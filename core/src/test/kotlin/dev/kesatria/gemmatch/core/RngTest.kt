package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RngTest {
    @Test
    fun matchesTheReferenceSplitMix64Sequence() {
        val rng = Rng(0L)

        assertEquals(-2152535657050944081L, rng.nextLong())
        assertEquals(7960286522194355700L, rng.nextLong())
        assertEquals(487617019471545679L, rng.nextLong())
    }

    @Test
    fun sameSeedGivesSameSequence() {
        val a = Rng(12345L)
        val b = Rng(12345L)

        repeat(100) { assertEquals(a.nextLong(), b.nextLong()) }
    }

    @Test
    fun differentSeedsGiveDifferentSequences() {
        assertTrue(Rng(1L).nextLong() != Rng(2L).nextLong())
    }

    @Test
    fun savedStateResumesTheSameFuture() {
        val rng = Rng(99L)
        repeat(10) { rng.nextLong() }

        val resumed = Rng(rng.state)

        repeat(50) { assertEquals(rng.nextLong(), resumed.nextLong()) }
    }

    @Test
    fun copyIsIndependentOfTheOriginal() {
        val rng = Rng(7L)
        val copy = rng.copy()

        rng.nextLong()

        assertEquals(Rng(7L).nextLong(), copy.nextLong())
    }

    @Test
    fun nextIntStaysInsideTheBound() {
        val rng = Rng(5L)

        repeat(1000) { assertTrue(rng.nextInt(GEM_TYPES) in 0 until GEM_TYPES) }
    }

    @Test
    fun nextIntReachesEveryValue() {
        val rng = Rng(5L)
        val seen = mutableSetOf<Int>()

        repeat(1000) { seen += rng.nextInt(GEM_TYPES) }

        assertEquals((0 until GEM_TYPES).toSet(), seen)
    }

    @Test
    fun nextIntRejectsANonPositiveBound() {
        assertFailsWith<IllegalArgumentException> { Rng(1L).nextInt(0) }
        assertFailsWith<IllegalArgumentException> { Rng(1L).nextInt(-3) }
    }

    @Test
    fun nextGemReturnsEveryGemType() {
        val rng = Rng(3L)
        val seen = mutableSetOf<Gem>()

        repeat(1000) { seen += rng.nextGem() }

        assertEquals(Gem.entries.toSet(), seen)
    }
}
