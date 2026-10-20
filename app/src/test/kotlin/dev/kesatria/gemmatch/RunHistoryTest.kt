package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.FinishedRun
import dev.kesatria.gemmatch.core.Lifetime
import dev.kesatria.gemmatch.core.RunStats
import dev.kesatria.gemmatch.core.encodeLifetime
import dev.kesatria.gemmatch.core.encodeRun
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The finished-run history on disk: `runs.txt` (the newest 100) and `lifetime.txt` (every run ever). */
class RunHistoryTest {
    @TempDir
    lateinit var directory: File

    private fun store() = FileRunHistory(GameStore(directory))

    private fun run(
        score: Int,
        started: Long = score.toLong(),
    ) = FinishedRun(score, RunStats.EMPTY, started, started + 1_000, 1_000, 500, newBest = false)

    private fun file(name: String) = File(directory, name)

    @Test
    fun withNothingOnDiskThereAreNoRunsAndAnEmptyLifetime() {
        assertEquals(emptyList(), store().runs())
        assertEquals(Lifetime.EMPTY, store().lifetime())
    }

    @Test
    fun aRecordedRunIsInBothFilesAndComesBack() {
        val store = store()

        assertTrue(store.record(run(1_200, started = 77L)))

        assertEquals(listOf(run(1_200, started = 77L)), store().runs())
        assertEquals(1L, store().lifetime().runs)
        assertEquals(1_200, store().lifetime().bestScore)
        assertEquals(77L, store().lifetime().last)
    }

    @Test
    fun aRunIsRecordedOnlyOnce() {
        val store = store()
        val first = run(900, started = 5L)

        assertTrue(store.record(first))
        assertFalse(store.record(first))

        assertEquals(1, store().runs().size)
        assertEquals(1L, store().lifetime().runs)
        assertEquals(900L, store().lifetime().score)
    }

    @Test
    fun theNewestRunComesFirst() {
        val store = store()
        store.record(run(10, started = 1L))
        store.record(run(20, started = 2L))

        assertEquals(listOf(20, 10), store().runs().map { it.score })
    }

    @Test
    fun theHundredAndFirstRunDropsTheOldestFromTheListButNotFromLifetime() {
        val store = store()
        store.record(run(9_999, started = 1L))
        for (i in 2..101) store.record(run(i, started = i.toLong()))

        val runs = store().runs()

        assertEquals(100, runs.size)
        assertEquals(101, runs.first().score)
        assertEquals(2, runs.last().score)
        assertEquals(101L, store().lifetime().runs)
        assertEquals(9_999, store().lifetime().bestScore)
    }

    @Test
    fun theBestScoreIsSeededFromTheOldBestFileWhenThereIsNoLifetimeYet() {
        file("best.txt").writeText("5000")

        assertEquals(5_000, store().lifetime().bestScore)
        store().record(run(300, started = 1L))

        assertEquals(5_000, store().lifetime().bestScore)
        assertEquals(1L, store().lifetime().runs)
    }

    @Test
    fun aDamagedRunLineCostsOnlyThatRun() {
        val lines = listOf(encodeRun(run(10, 1L)), "garbage", encodeRun(run(30, 3L)), "RN1;score=-4")
        file("runs.txt").writeText(lines.joinToString("\n"))

        assertEquals(listOf(10, 30), store().runs().map { it.score })
    }

    @Test
    fun aDamagedLifetimeFileReadsAsEmptyAndTheNextRunStartsFromIt() {
        file("lifetime.txt").writeText("not a lifetime")
        assertEquals(0L, store().lifetime().runs)

        assertTrue(store().record(run(40, started = 4L)))

        assertEquals(1L, store().lifetime().runs)
    }

    @Test
    fun anOversizeHistoryFileIsTreatedAsDamaged() {
        file("runs.txt").writeText((encodeRun(run(10, 1L)) + "\n").repeat(2_000))
        file("lifetime.txt").writeText(encodeLifetime(Lifetime.EMPTY) + " ".repeat(5_000))

        assertEquals(emptyList(), store().runs())
        assertEquals(0L, store().lifetime().runs)
    }

    @Test
    fun aKillAfterLifetimeWasWrittenButBeforeTheListNeverCountsTheRunTwice() {
        val finished = run(700, started = 9L)
        file("lifetime.txt").writeText(encodeLifetime(Lifetime.EMPTY + finished))

        assertFalse(store().record(finished))

        assertEquals(1L, store().lifetime().runs)
        assertEquals(emptyList(), store().runs(), "that one run is lost from the list, never counted twice")
    }

    @Test
    fun aKillBeforeAnythingWasWrittenLetsTheRunBeRecordedOnRelaunch() {
        val finished = run(700, started = 9L)

        assertTrue(store().record(finished))

        assertEquals(1L, store().lifetime().runs)
        assertEquals(listOf(finished), store().runs())
    }

    @Test
    fun aKillAfterBothFilesWereWrittenNeverCountsTheRunTwice() {
        val finished = run(700, started = 9L)
        file("lifetime.txt").writeText(encodeLifetime(Lifetime.EMPTY + finished))
        file("runs.txt").writeText(encodeRun(finished))

        assertFalse(store().record(finished))

        assertEquals(1L, store().lifetime().runs)
        assertEquals(1, store().runs().size)
    }

    @Test
    fun aFirstRunWithAZeroStartTimeIsStillRecorded() {
        assertTrue(store().record(run(10, started = 0L)))
        assertEquals(1L, store().lifetime().runs)
    }
}
