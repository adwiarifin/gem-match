package dev.kesatria.gemmatch

import kotlin.test.Test
import kotlin.test.assertEquals

class RunClockTest {
    @Test
    fun aNewClockStartsAtTheTimeItIsGiven() {
        assertEquals(0L, RunClock().activeMs)
        assertEquals(4_000L, RunClock(4_000L).activeMs)
    }

    @Test
    fun frameTimeAddsUpInMilliseconds() {
        val clock = RunClock()

        clock.advance(1.5f)
        clock.advance(0.25f)

        assertEquals(1_750L, clock.activeMs)
    }

    @Test
    fun aSecondOfSixtyHertzStepsIsASecond() {
        val clock = RunClock()

        repeat(60) { clock.advance(1f / 60f) }

        assertEquals(1_000L, clock.activeMs)
    }

    @Test
    fun aStalledOrNegativeFrameAddsNothing() {
        val clock = RunClock(500L)

        clock.advance(0f)
        clock.advance(-3f)

        assertEquals(500L, clock.activeMs)
    }

    @Test
    fun resettingStartsFromZero() {
        val clock = RunClock(9_000L)
        clock.advance(2f)

        clock.reset()

        assertEquals(0L, clock.activeMs)
        clock.advance(1f)
        assertEquals(1_000L, clock.activeMs)
    }
}
