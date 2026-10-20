package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LifetimeTest {
    private fun run(
        score: Int,
        started: Long,
        stats: RunStats = RunStats.EMPTY,
        wall: Long = 1_000,
        active: Long = 500,
    ) = FinishedRun(score, stats, started, started + wall, wall, active, newBest = false)

    @Test
    fun noRunsIsAllZero() {
        assertEquals(0L, Lifetime.EMPTY.runs)
        assertEquals(0, Lifetime.EMPTY.bestScore)
        assertEquals(0L, Lifetime.EMPTY.last)
    }

    @Test
    fun oneRunSetsTheTotalsTheBestsAndTheLastMarker() {
        val stats = RunStats(40, 5, 300, 210, 6, mapOf(Special.BLAST to 2), mapOf(Special.COLOUR to 1))

        val lifetime = Lifetime.EMPTY + run(1_200, started = 77L, stats = stats, wall = 9_000, active = 4_000)

        assertEquals(1L, lifetime.runs)
        assertEquals(1_200L, lifetime.score)
        assertEquals(40L, lifetime.moves)
        assertEquals(210L, lifetime.gems)
        assertEquals(mapOf(Special.BLAST to 2L), lifetime.created)
        assertEquals(mapOf(Special.COLOUR to 1L), lifetime.fired)
        assertEquals(9_000L, lifetime.wallMs)
        assertEquals(4_000L, lifetime.activeMs)
        assertEquals(1_200, lifetime.bestScore)
        assertEquals(5, lifetime.bestCascade)
        assertEquals(300, lifetime.bestMoveScore)
        assertEquals(6, lifetime.bestMatch)
        assertEquals(9_000L, lifetime.bestWallMs)
        assertEquals(4_000L, lifetime.bestActiveMs)
        assertEquals(77L, lifetime.last)
    }

    @Test
    fun totalsAddUpAndBestsKeepTheHighestEvenWhenALaterRunIsWorse() {
        val big = run(5_000, started = 1L, stats = RunStats(100, 9, 800, 500, 8, emptyMap(), emptyMap()), wall = 50_000)
        val small = run(100, started = 2L, stats = RunStats(10, 2, 60, 30, 3, emptyMap(), emptyMap()), wall = 2_000)

        val lifetime = Lifetime.EMPTY + big + small

        assertEquals(2L, lifetime.runs)
        assertEquals(5_100L, lifetime.score)
        assertEquals(110L, lifetime.moves)
        assertEquals(5_000, lifetime.bestScore)
        assertEquals(9, lifetime.bestCascade)
        assertEquals(50_000L, lifetime.bestWallMs)
        assertEquals(2L, lifetime.last, "last is the newest run recorded")
    }

    @Test
    fun lifetimeStillCountsARunThatTheRunListHasDropped() {
        var lifetime = Lifetime.EMPTY
        var kept = emptyList<FinishedRun>()
        val first = run(9_999, started = 1L)
        for (i in 1..101) {
            val next = if (i == 1) first else run(i, started = i.toLong())
            lifetime += next
            kept = keepNewest(kept, next, limit = 100)
        }

        assertEquals(101L, lifetime.runs)
        assertEquals(9_999, lifetime.bestScore)
        assertTrue(first !in kept, "the first run is gone from the list")
        assertEquals(100, kept.size)
    }

    @Test
    fun sumsStopAtTheLimitInsteadOfWrapping() {
        val nearlyFull = Lifetime.EMPTY.copy(runs = Long.MAX_VALUE, score = Long.MAX_VALUE - 1, wallMs = Long.MAX_VALUE)

        val lifetime = nearlyFull + run(1_000, started = 1L, wall = 5_000)

        assertEquals(Long.MAX_VALUE, lifetime.runs)
        assertEquals(Long.MAX_VALUE, lifetime.score)
        assertEquals(Long.MAX_VALUE, lifetime.wallMs)
    }

    @Test
    fun aLifetimeLineStartsWithTheVersionAndReadsBackAsWritten() {
        val stats =
            RunStats(40, 5, 300, 210, 6, Special.entries.associateWith { 2 }, Special.entries.associateWith { 3 })
        val lifetime =
            Lifetime.EMPTY + run(1_200, started = 77L, stats = stats) + run(3_000, started = 99L, stats = stats)

        val line = encodeLifetime(lifetime)

        assertTrue(line.startsWith("LT1;"), line)
        assertEquals(lifetime, decodeLifetime(line))
    }

    @Test
    fun anEmptyLifetimeRoundTrips() {
        assertEquals(Lifetime.EMPTY, decodeLifetime(encodeLifetime(Lifetime.EMPTY)))
    }

    @Test
    fun aMissingKeyReadsZeroAndAnUnknownKeyIsIgnored() {
        val minimal = assertNotNull(decodeLifetime("LT1;runs=3"))
        val newer = assertNotNull(decodeLifetime("LT1;runs=3,futureStat=5"))

        assertEquals(3L, minimal.runs)
        assertEquals(0L, minimal.score)
        assertEquals(minimal, newer)
    }

    @Test
    fun aMalformedLifetimeLineReturnsNull() {
        val bad =
            listOf(
                "",
                "LT1",
                "LT2;runs=1",
                "RN1;runs=1",
                "LT1;runs=-1",
                "LT1;runs=x",
                "LT1;runs=1,runs=2",
                "LT1;bestScore=99999999999",
                "LT1;a;b",
            )
        for (line in bad) assertNull(decodeLifetime(line), "'$line'")
    }
}
