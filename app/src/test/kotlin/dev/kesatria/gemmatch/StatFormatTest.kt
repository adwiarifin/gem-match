package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.FinishedRun
import dev.kesatria.gemmatch.core.RunStats
import dev.kesatria.gemmatch.core.Special
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatFormatTest {
    private fun run(
        score: Int = 1_200,
        stats: RunStats = RunStats(40, 5, 300, 1_210, 6, emptyMap(), emptyMap()),
        wall: Long = 754_000,
        active: Long = 605_000,
    ) = FinishedRun(score, stats, 1L, 1L + wall, wall, active, newBest = false)

    private fun rows(run: FinishedRun) = statRows(run).associate { it.label to it.value }

    @Test
    fun countsGetThousandsSeparators() {
        assertEquals("0", formatCount(0))
        assertEquals("999", formatCount(999))
        assertEquals("1,000", formatCount(1_000))
        assertEquals("1,234,567", formatCount(1_234_567))
        assertEquals("2,147,483,647", formatCount(Int.MAX_VALUE.toLong()))
    }

    @Test
    fun durationsReadMinutesAndSecondsThenHours() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:09", formatDuration(9_400))
        assertEquals("1:05", formatDuration(65_000))
        assertEquals("59:59", formatDuration(3_599_999))
        assertEquals("1:00:00", formatDuration(3_600_000))
        assertEquals("12:03:07", formatDuration(43_387_000))
        assertEquals("0:00", formatDuration(-5_000))
    }

    @Test
    fun theTableListsEveryPerRunStatWithItsValue() {
        val rows = rows(run())

        assertEquals("40", rows["Moves"])
        assertEquals("5", rows["Longest cascade"])
        assertEquals("300", rows["Biggest move score"])
        assertEquals("1,210", rows["Gems cleared"])
        assertEquals("6", rows["Biggest match"])
        assertEquals("30", rows["Average score per move"])
        assertEquals("10:05", rows["Active time"])
        assertEquals("12:34", rows["Total time"])
    }

    @Test
    fun theAverageScorePerMoveIsWholeAndSafeWithNoMoves() {
        assertEquals("30", rows(run())["Average score per move"])
        assertEquals("0", rows(run(score = 0, stats = RunStats.EMPTY))["Average score per move"])
        val threeMoves = RunStats(3, 1, 1, 1, 3, emptyMap(), emptyMap())
        assertEquals("33", rows(run(score = 100, stats = threeMoves))["Average score per move"])
    }

    @Test
    fun specialsShowATotalAndOneRowForEachKindThatHappened() {
        val stats =
            RunStats(
                10,
                2,
                90,
                50,
                4,
                created = mapOf(Special.BLAST to 2, Special.COLOUR to 1),
                fired = mapOf(Special.BLAST to 3),
            )

        val rows = rows(run(stats = stats))

        assertEquals("3", rows["Special gems made"])
        assertEquals("2", rows["  Blast made"])
        assertEquals("1", rows["  Colour made"])
        assertEquals("3", rows["Special gems fired"])
        assertEquals("3", rows["  Blast fired"])
        assertFalse("  Cross made" in rows, "a kind that never happened has no row")
    }

    @Test
    fun aRunWithNoSpecialsStillShowsTheTotalsAtZero() {
        val rows = rows(run())

        assertEquals("0", rows["Special gems made"])
        assertEquals("0", rows["Special gems fired"])
        assertTrue(rows.keys.none { it.startsWith("  ") })
    }

    @Test
    fun everyLabelIsUnique() {
        val labels =
            statRows(
                run(
                    stats =
                        RunStats(
                            1,
                            1,
                            1,
                            1,
                            3,
                            Special.entries.associateWith {
                                1
                            },
                            Special.entries.associateWith { 2 },
                        ),
                ),
            ).map { it.label }

        assertEquals(labels.size, labels.toSet().size)
    }
}
