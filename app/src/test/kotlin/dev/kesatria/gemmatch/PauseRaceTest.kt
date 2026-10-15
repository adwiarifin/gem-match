package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.SwapOutcome
import dev.kesatria.gemmatch.core.findValidMoves
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals

/** The UI thread saves on pause while the loop thread may be finishing a move. The disk must end on the newest. */
class PauseRaceTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun aPauseSaveRacingMovesNeverLeavesAnOlderGameOnDisk() {
        val moveCount = 400
        var expected = Game.new(99L)
        repeat(moveCount) { turn ->
            val moves = findValidMoves(expected.board)
            val move = moves[(turn * 5) % moves.size]
            expected = (expected.trySwap(move.from, move.to) as SwapOutcome.Accepted).game
        }

        val store = GameStore(directory)
        val session = GameSession(Game.new(99L), store, 0) { 99L }
        val scene = Scene().also { it.load(session.board) }
        val stop = AtomicBoolean(false)
        val pauseThread =
            thread {
                while (!stop.get()) session.save()
            }
        repeat(moveCount) { turn ->
            val moves = findValidMoves(session.board)
            playToEnd(session.play(moves[(turn * 5) % moves.size]), scene)
            session.finish(scene)
        }
        stop.set(true)
        pauseThread.join()
        store.awaitWrites()

        assertEquals(expected.encode(), File(directory, "game.sav").readText())
    }
}
