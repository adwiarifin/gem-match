package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.Game
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors

/**
 * The saved game and the best score, as two small files in app-private storage. Every write goes
 * to a temp file first and is then renamed over the real one, so a kill mid-write can only leave
 * the old file or the new one, never half of each. Writes run on one background thread; a failed
 * write is dropped, never allowed to crash the game.
 */
class GameStore(
    private val directory: File,
) {
    private val writer =
        Executors.newSingleThreadExecutor { task ->
            Thread(task, "game-store").apply { isDaemon = true }
        }

    /** The saved game, or null if there is none or it is damaged. */
    fun loadGame(): Game? = read(GAME_FILE)?.let { Game.decode(it) }

    fun loadBest(): Int = read(BEST_FILE)?.trim()?.toIntOrNull()?.takeIf { it >= 0 } ?: 0

    fun saveGame(game: Game) {
        writer.execute { write(GAME_FILE, game.encode()) }
    }

    fun saveBest(best: Int) {
        writer.execute { write(BEST_FILE, best.toString()) }
    }

    /**
     * Writes both and waits until they are on disk. For when the app is going to the background.
     * The state is read by the writer thread when its turn comes, so a move that finishes after this was called
     * but before it runs is saved, and an older state can never be written on top of a newer one.
     */
    fun flush(
        game: () -> Game,
        best: () -> Int,
    ) {
        writer
            .submit {
                write(GAME_FILE, game().encode())
                write(BEST_FILE, best().toString())
            }.get()
    }

    /** Waits until every write queued so far has finished. Lets tests check the disk without polling. */
    internal fun awaitWrites() {
        writer.submit {}.get()
    }

    private fun read(name: String): String? =
        try {
            File(directory, name).takeIf { it.isFile && it.length() <= MAX_FILE_BYTES }?.readText()
        } catch (_: IOException) {
            null
        }

    private fun write(
        name: String,
        text: String,
    ) {
        try {
            val temp = File(directory, "$name.tmp")
            FileOutputStream(temp).use {
                it.write(text.toByteArray())
                it.fd.sync()
            }
            val target = File(directory, name).toPath()
            Files.move(temp.toPath(), target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: IOException) {
            // A lost save costs one move of progress. It must never take the game down.
        }
    }

    private companion object {
        const val GAME_FILE = "game.sav"
        const val BEST_FILE = "best.txt"

        /** Both files are under 128 bytes when the app wrote them; a bigger file is damaged or planted. */
        const val MAX_FILE_BYTES = 1024L
    }
}
