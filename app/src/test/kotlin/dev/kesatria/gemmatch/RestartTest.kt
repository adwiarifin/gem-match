package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.SwapOutcome
import dev.kesatria.gemmatch.core.findValidMoves
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Success criteria 7 and 8 end to end: killed, reopened from disk, and carried on as if nothing happened. */
class RestartTest {
    @TempDir
    lateinit var directory: File

    private fun openSession(seedForNewGames: Long = 99L): Pair<GameSession, GameStore> {
        val store = GameStore(directory)
        val game = store.loadGame() ?: Game.new(seedForNewGames)
        return GameSession(game, store, store.loadBest()) { seedForNewGames } to store
    }

    private fun playOne(
        session: GameSession,
        scene: Scene,
        turn: Int,
    ) {
        val moves = findValidMoves(session.board)
        playToEnd(session.play(moves[(turn * 5) % moves.size]), scene)
        session.finish(scene)
    }

    @Test
    fun aKilledAndReopenedGameHasTheSameBoardScoreAndFutureAsOneThatNeverStopped() {
        // The uninterrupted game, played on the core alone.
        var expected = Game.new(99L)
        repeat(6) { turn ->
            val moves = findValidMoves(expected.board)
            val move = moves[(turn * 5) % moves.size]
            expected = (expected.trySwap(move.from, move.to) as SwapOutcome.Accepted).game
        }

        // The same game through the session, killed after 3 moves and reopened from disk.
        val (first, firstStore) = openSession()
        val firstScene = Scene().also { it.load(first.board) }
        repeat(3) { playOne(first, firstScene, it) }
        first.save()
        firstStore.awaitWrites()

        val (reopened, reopenedStore) = openSession()
        val reopenedScene = Scene().also { it.load(reopened.board) }
        assertEquals(first.board, reopened.board)
        assertEquals(first.shownScore, reopened.shownScore)
        assertEquals(first.bestScore, reopened.bestScore)
        for (turn in 3 until 6) playOne(reopened, reopenedScene, turn)
        reopened.save()
        reopenedStore.awaitWrites()

        assertEquals(expected.board, reopened.board)
        assertEquals(expected.score, reopened.shownScore)
        assertEquals(expected.encode(), File(directory, "game.sav").readText())
    }

    @Test
    fun aKillMidCascadeReopensOnThePostMoveBoardNotThePreMoveBoard() {
        val (session, store) = openSession()
        val scene = Scene().also { it.load(session.board) }
        val move = findValidMoves(session.board).first()
        val expected = (Game.new(99L).trySwap(move.from, move.to) as SwapOutcome.Accepted).game

        session.play(move).advance(0.05f, scene) // killed before a single clear has finished
        store.awaitWrites()
        val (reopened, _) = openSession()

        assertEquals(expected.board, reopened.board)
        assertEquals(expected.score, reopened.shownScore)
    }

    @Test
    fun aBestScoreThatLaggedBehindAKilledGameIsRepairedOnReopen() {
        val (session, store) = openSession()
        val scene = Scene().also { it.load(session.board) }
        val move = findValidMoves(session.board).first()
        session.play(move).advance(0.05f, scene) // best.txt is not written until the move ends
        store.awaitWrites()
        assertTrue(!File(directory, "best.txt").exists())

        val (reopened, _) = openSession()

        assertTrue(reopened.shownScore > 0)
        assertEquals(reopened.shownScore, reopened.bestScore)
        assertNotNull(File(directory, "game.sav").takeIf { it.isFile })
    }
}
