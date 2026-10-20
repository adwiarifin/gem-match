package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RunRecordTest {
    private fun played(moves: Int = 6): Game {
        var game = Game.new(8L, startedMs = STARTED).withActiveMs(ACTIVE)
        repeat(moves) {
            val move = findValidMoves(game.board).first()
            game = (game.trySwap(move.from, move.to) as SwapOutcome.Accepted).game
        }
        return game
    }

    private val sample =
        FinishedRun(
            score = 1_200,
            stats = RunStats(40, 5, 300, 210, 6, mapOf(Special.BLAST to 2), mapOf(Special.COLOUR to 1)),
            startedMs = STARTED,
            endedMs = STARTED + 90_000,
            wallMs = 90_000,
            activeMs = 60_000,
            newBest = true,
        )

    @Test
    fun aFinishedRunCarriesTheGamesScoreStatsAndClocks() {
        val game = played()

        val run = game.finish(endedMs = STARTED + 60_000, previousBest = 0)

        assertEquals(game.score, run.score)
        assertEquals(game.stats, run.stats)
        assertEquals(STARTED, run.startedMs)
        assertEquals(STARTED + 60_000, run.endedMs)
        assertEquals(60_000, run.wallMs)
        assertEquals(ACTIVE, run.activeMs)
    }

    @Test
    fun wallTimeIsAtLeastTheActiveTimeEvenIfTheClockWentBackwards() {
        val game = played().withActiveMs(10_000)

        assertEquals(10_000, game.finish(endedMs = STARTED + 1_000, previousBest = 0).wallMs)
        assertEquals(10_000, game.finish(endedMs = STARTED - 50_000, previousBest = 0).wallMs)
        assertEquals(30_000, game.finish(endedMs = STARTED + 30_000, previousBest = 0).wallMs)
    }

    @Test
    fun aRunIsANewBestOnlyWhenItBeatsThePreviousBest() {
        val game = played()
        assertTrue(game.score > 0, "the fixture must have scored")

        assertTrue(game.finish(STARTED, previousBest = 0).newBest, "first run with a score")
        assertTrue(game.finish(STARTED, previousBest = game.score - 1).newBest, "one point better")
        assertFalse(game.finish(STARTED, previousBest = game.score).newBest, "a tie")
        assertFalse(game.finish(STARTED, previousBest = game.score + 1).newBest, "worse")
        assertFalse(Game.new(1L).finish(STARTED, previousBest = 0).newBest, "a score of 0 is no best")
    }

    @Test
    fun aRunLineStartsWithTheVersionAndReadsBackAsWritten() {
        val line = encodeRun(sample)

        assertTrue(line.startsWith("RN1;"), line)
        assertEquals(sample, decodeRun(line))
    }

    @Test
    fun aRunWithEveryKindOfSpecialRoundTrips() {
        val created = Special.entries.mapIndexed { i, s -> s to i + 1 }.toMap()
        val fired = Special.entries.mapIndexed { i, s -> s to i + 10 }.toMap()
        val run = sample.copy(stats = sample.stats.copy(created = created, fired = fired))

        assertEquals(run, decodeRun(encodeRun(run)))
    }

    @Test
    fun aMissingKeyReadsZeroAndAnUnknownKeyIsIgnored() {
        val minimal = assertNotNull(decodeRun("RN1;score=7"))
        val newer = assertNotNull(decodeRun(encodeRun(sample) + ",futureStat=5"))

        assertEquals(7, minimal.score)
        assertEquals(RunStats.EMPTY, minimal.stats)
        assertEquals(0L, minimal.wallMs)
        assertFalse(minimal.newBest)
        assertEquals(sample, newer)
    }

    @Test
    fun aMalformedRunLineReturnsNull() {
        val good = encodeRun(sample)
        val bad =
            listOf(
                "",
                "RN1",
                "RN2;score=7",
                "XX1;score=7",
                "RN1;score=-7",
                "RN1;score=abc",
                "RN1;score=7,score=8",
                "RN1;score=99999999999",
                "RN1;moves=99999999999",
                "RN1;best=2",
                "$good;extra",
                "RN1;=7",
            )
        for (line in bad) assertNull(decodeRun(line), "'$line'")
    }

    @Test
    fun theNewestHundredRunsAreKeptNewestFirst() {
        var runs = emptyList<FinishedRun>()
        for (i in 1..101) runs = keepNewest(runs, sample.copy(score = i), limit = 100)

        assertEquals(100, runs.size)
        assertEquals(101, runs.first().score)
        assertEquals(2, runs.last().score)
    }

    @Test
    fun aListUnderTheLimitKeepsEveryRun() {
        val runs = keepNewest(listOf(sample.copy(score = 1)), sample.copy(score = 2), limit = 100)

        assertEquals(listOf(2, 1), runs.map { it.score })
    }

    private companion object {
        const val STARTED = 1_700_000_000_000L
        const val ACTIVE = 40_000L
    }
}
