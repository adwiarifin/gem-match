package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.Game
import org.junit.jupiter.api.io.TempDir
import java.io.File
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

        store.flush(game, 1234)

        assertEquals(game.encode(), File(directory, "game.sav").readText())
        assertEquals("1234", File(directory, "best.txt").readText())
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
                good.replaceFirst("GM1", "GM9"),
                good + ";extra",
            )
        for (text in damaged) {
            File(directory, "game.sav").writeText(text)

            assertNull(store().loadGame(), "should reject: \"${text.take(30)}\"")
        }
    }

    @Test
    fun anOversizedSaveFileIsIgnoredWithoutBeingReadIntoMemory() {
        File(directory, "game.sav").writeText("1".repeat(50_000))
        File(directory, "best.txt").writeText("1".repeat(50_000))

        assertNull(store().loadGame())
        assertEquals(0, store().loadBest())
    }

    @Test
    fun aHugeFileIsNeverReadIntoMemory() {
        // Sparse, so it costs no disk. Reading it with readText() would throw OutOfMemoryError.
        for (name in listOf("game.sav", "best.txt")) {
            java.io.RandomAccessFile(File(directory, name), "rw").use { it.setLength(3_000_000_000L) }
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
        store.flush(Game.new(3L), 5)

        assertTrue(store.loadGame() == null && store.loadBest() == 0)
    }
}
