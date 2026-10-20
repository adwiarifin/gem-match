package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.RunTime
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameStoreTest {
    @TempDir
    lateinit var directory: File

    private fun store() = GameStore(directory)

    @Test
    fun aSavedGameComesBackExactlyIncludingItsFutureGems() {
        val game = Game.new(7L)
        val store = store()

        store.saveGame(game)
        store.awaitWrites()

        val loaded = assertNotNull(store.loadGame())
        assertEquals(game.encode(), loaded.encode())
    }

    @Test
    fun flushHasBothFilesOnDiskWhenItReturns() {
        val game = Game.new(8L)
        val store = store()

        store.flush({ game }, { 1234 })

        assertEquals(game.encode(), File(directory, "game.sav").readText())
        assertEquals("1234", File(directory, "best.txt").readText())
    }

    @Test
    @Timeout(30)
    fun flushReadsTheNewestStateWhenItsWriteRunsNotWhenItWasCalled() {
        val older = Game.new(10L)
        val newer = Game.new(11L)
        val current = AtomicReference(older)
        val gate = CountDownLatch(1)
        val writerIsBusy = CountDownLatch(1)
        val store = store()
        // Keeps the writer thread busy, like a slow disk, so the next flush has to wait its turn.
        val busy =
            thread {
                store.flush({
                    writerIsBusy.countDown()
                    gate.await()
                    older
                }, { 0 })
            }
        assertTrue(writerIsBusy.await(10, TimeUnit.SECONDS))
        val pause = thread { store.flush({ current.get() }, { 0 }) }
        // WAITING means the pause thread has called flush and is blocked on its write: whatever flush was going
        // to read at call time it has read by now, and nothing has been written yet.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (pause.state != Thread.State.WAITING) {
            check(System.nanoTime() < deadline) { "the pause thread never reached flush" }
            Thread.onSpinWait()
        }

        current.set(newer) // a move finishes after the pause was requested but before its write ran
        gate.countDown()
        busy.join(10_000)
        pause.join(10_000)

        assertEquals(newer.encode(), File(directory, "game.sav").readText())
    }

    @Test
    fun nothingSavedMeansNoGameAndBestZero() {
        val store = store()

        assertNull(store.loadGame())
        assertEquals(0, store.loadBest())
    }

    @Test
    fun aDamagedGameFileLoadsAsNoGame() {
        val good = Game.new(9L).encode()
        val damaged =
            listOf(
                "",
                "garbage",
                good.take(good.length / 2),
                good.replaceFirst("GM3", "GM9"),
                good + ";extra",
            )
        for (text in damaged) {
            File(directory, "game.sav").writeText(text)

            assertNull(store().loadGame(), "should reject: \"${text.take(30)}\"")
        }
    }

    // Core's own length check rejects this, so it does not prove the size guard in GameStore.
    // aHugeFileIsNeverReadIntoMemory below is the test that does.
    @Test
    fun aFileFarLargerThanASaveLoadsAsNothing() {
        File(directory, "game.sav").writeText("1".repeat(50_000))
        File(directory, "best.txt").writeText("1".repeat(50_000))

        assertNull(store().loadGame())
        assertEquals(0, store().loadBest())
    }

    @Test
    fun theSizeCapIsExactlyOneKilobyte() {
        File(directory, "best.txt").writeText("1200".padEnd(1024))
        assertEquals(1200, store().loadBest(), "1024 bytes is still read")

        File(directory, "best.txt").writeText("1200".padEnd(1025))
        assertEquals(0, store().loadBest(), "1025 bytes is refused")
    }

    @Test
    fun aHugeFileIsNeverReadIntoMemory() {
        // Sparse, so it costs no disk. Reading it with readText() would throw OutOfMemoryError.
        for (name in listOf("game.sav", "best.txt")) {
            java.io.RandomAccessFile(File(directory, name), "rw").use { it.setLength(HUGE_BYTES) }
        }

        assertNull(store().loadGame())
        assertEquals(0, store().loadBest())
    }

    @Test
    fun anUnreadableBestScoreLoadsAsZero() {
        for (text in listOf("", "abc", "-5", "1.5", "99999999999999999999")) {
            File(directory, "best.txt").writeText(text)

            assertEquals(0, store().loadBest(), "should reject: \"$text\"")
        }
    }

    @Test
    fun aBestScoreWithWhitespaceIsStillRead() {
        File(directory, "best.txt").writeText(" 1200\n")

        assertEquals(1200, store().loadBest())
    }

    @Test
    fun savingTwiceKeepsTheNewestAndLeavesNoTempFiles() {
        val first = Game.new(1L)
        val second = Game.new(2L)
        val store = store()

        store.saveGame(first)
        store.saveGame(second)
        store.saveBest(10)
        store.saveBest(20)
        store.awaitWrites()

        assertEquals(second.encode(), File(directory, "game.sav").readText())
        assertEquals(20, store.loadBest())
        assertEquals(listOf("best.txt", "game.sav"), directory.list().orEmpty().sorted())
    }

    @Test
    fun aWriteThatFailsMidwayLeavesTheExistingSaveUntouched() {
        val first = Game.new(1L)
        val store = store()
        store.saveGame(first)
        store.awaitWrites()
        // A directory where the temp file belongs makes the new write fail before it can touch anything.
        File(directory, "game.sav.tmp").mkdir()

        store.saveGame(Game.new(2L))
        store.awaitWrites()

        assertEquals(first.encode(), File(directory, "game.sav").readText())
    }

    @Test
    fun aWriteThatCannotHappenIsDroppedWithoutCrashing() {
        val store = GameStore(File(directory, "does/not/exist"))

        store.saveGame(Game.new(3L))
        store.saveBest(5)
        store.flush({ Game.new(3L) }, { 5 })

        assertNull(store.loadGame())
        assertEquals(0, store.loadBest())
    }

    private companion object {
        const val HUGE_BYTES = 3_000_000_000L
    }

    private val releasedGm1 =
        "GM1;210;92694865759587301;" +
            "6145022601136334006053121631042355443363401001654464611054132212"

    @Test
    fun anOldGm1FileOnDiskLoadsAndIsRewrittenAsGm3() {
        File(directory, "game.sav").writeText(releasedGm1)
        val store = store()

        val loaded = assertNotNull(store.loadGame())
        assertEquals(210, loaded.score)
        store.saveGame(loaded)
        store.awaitWrites()

        val onDisk = File(directory, "game.sav").readText()
        assertTrue(onDisk.startsWith("GM3;210;92694865759587301;"), onDisk)
        assertEquals(loaded.board, assertNotNull(store().loadGame()).board)
    }

    @Test
    fun anOldFileStartsItsClockAtTheTimeItIsLoaded() {
        File(directory, "game.sav").writeText(releasedGm1)

        val loaded = assertNotNull(store().loadGame(nowMs = 7_000L))

        assertEquals(RunTime(7_000L, 0L), loaded.time)
    }

    @Test
    fun aGm2FileWithOneBadCellLoadsAsNoGame() {
        val good = Game.new(9L).encode()
        val cells = good.split(';')[3]
        val damaged =
            listOf(
                good.replace(cells, "Z-" + cells.drop(2)),
                good.replace(cells, "0Q" + cells.drop(2)),
                good.replace(cells, "-B" + cells.drop(2)),
                good.replace(cells, "0W" + cells.drop(2)),
            )
        for (text in damaged) {
            File(directory, "game.sav").writeText(text)

            assertNull(store().loadGame(), "should reject: \"${text.takeLast(20)}\"")
        }
    }

    @Test
    fun aTruncatedGm2FileLoadsAsNoGame() {
        val good = Game.new(9L).encode()
        for (length in listOf(good.length - 1, good.length - 2, good.length - 64, good.length / 2, 4)) {
            File(directory, "game.sav").writeText(good.take(length))

            assertNull(store().loadGame(), "truncated to $length characters")
        }
    }
}
