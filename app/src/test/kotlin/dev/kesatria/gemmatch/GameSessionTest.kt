package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.Move
import dev.kesatria.gemmatch.core.Pos
import dev.kesatria.gemmatch.core.SwapOutcome
import dev.kesatria.gemmatch.core.findValidMoves
import dev.kesatria.gemmatch.core.isValidSwap
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameSessionTest {
    @TempDir
    lateinit var directory: File

    private val store get() = GameStore(directory)

    private fun sessionOf(
        game: Game,
        best: Int = 0,
        seed: Long = 99L,
    ) = GameSession(game, store.also { storeInUse = it }, best) { seed }

    private var storeInUse: GameStore? = null

    /** A move saves in the background; let it land before the temp directory is deleted. */
    @AfterTest
    fun drainWrites() {
        storeInUse?.awaitWrites()
    }

    private fun saved(name: String): String? = File(directory, name).takeIf { it.isFile }?.readText()

    private fun invalidMove(game: Game): Move {
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE - 1) {
                val move = Move(Pos(row, col), Pos(row, col + 1))
                if (!isValidSwap(game.board, move.from, move.to)) return move
            }
        }
        error("every swap is valid")
    }

    @Test
    fun anAcceptedMoveIsOnDiskBeforeItsAnimationHasPlayed() {
        val game = Game.new(11L)
        val move = findValidMoves(game.board).first()
        val expected = (game.trySwap(move.from, move.to) as SwapOutcome.Accepted).game
        val session = sessionOf(game)

        session.play(move)
        storeInUse!!.awaitWrites()

        assertEquals(expected.encode(), saved("game.sav"))
    }

    @Test
    fun theBoardAndScoreStayAsTheyWereUntilTheAnimationFinishes() {
        val game = Game.new(12L)
        val move = findValidMoves(game.board).first()
        val outcome = game.trySwap(move.from, move.to) as SwapOutcome.Accepted
        val scene = Scene().also { it.load(game.board) }
        val session = sessionOf(game)

        val timeline = session.play(move)
        assertEquals(game.board, session.board)
        assertEquals(0, session.shownScore)

        playToEnd(timeline, scene)
        session.finish(scene)

        assertEquals(outcome.game.board, session.board)
        assertEquals(outcome.game.score, session.shownScore)
        assertSceneShows(outcome.game.board, scene)
    }

    @Test
    fun theScoreCountsUpAsEachClearFinishesNotAllAtOnce() {
        val game = Game.new(13L)
        val move = findValidMoves(game.board).first()
        val outcome = game.trySwap(move.from, move.to) as SwapOutcome.Accepted
        val scene = Scene().also { it.load(game.board) }
        val session = sessionOf(game)
        val timeline = session.play(move)

        val shownAlongTheWay = mutableListOf<Int>()
        while (!timeline.isFinished) {
            timeline.advance(1f / 60, scene)
            shownAlongTheWay += session.shownScore
        }

        assertEquals(outcome.game.score, session.shownScore)
        assertTrue(shownAlongTheWay.first() < outcome.game.score, "score should not jump at the first frame")
        assertEquals(shownAlongTheWay.sorted(), shownAlongTheWay, "score must never go down")
    }

    @Test
    fun theShownScoreNeverWrapsNegativeNearTheCeiling() {
        val fresh = Game.new(23L)
        val game = Game.decode(fresh.encode().replace(";0;", ";${Int.MAX_VALUE - 5};"))!!
        val move = findValidMoves(game.board).first()
        val scene = Scene().also { it.load(game.board) }
        val session = sessionOf(game)
        val timeline = session.play(move)

        while (!timeline.isFinished) {
            timeline.advance(1f / 60, scene)
            assertTrue(session.shownScore >= 0 && session.bestScore >= 0, "score wrapped negative")
        }
        session.finish(scene)

        assertEquals(Int.MAX_VALUE, session.shownScore)
    }

    @Test
    fun aRejectedMoveChangesNothingAndWritesNothing() {
        val game = Game.new(14L)
        val scene = Scene().also { it.load(game.board) }
        val move = invalidMove(game)
        val session = sessionOf(game)

        val timeline = session.play(move)
        playToEnd(timeline, scene)
        session.finish(scene)
        storeInUse!!.awaitWrites()

        assertEquals(game.board, session.board)
        assertEquals(0, session.shownScore)
        assertSceneShows(game.board, scene)
        assertEquals(null, saved("game.sav"))
        assertEquals(null, saved("best.txt"))
    }

    @Test
    fun bestIsWrittenOnlyWhenTheScoreBeatsIt() {
        val game = Game.new(15L)
        val move = findValidMoves(game.board).first()
        val outcome = game.trySwap(move.from, move.to) as SwapOutcome.Accepted
        val scene = Scene().also { it.load(game.board) }
        val session = sessionOf(game, best = outcome.game.score + 500)

        playToEnd(session.play(move), scene)
        session.finish(scene)
        storeInUse!!.awaitWrites()

        assertEquals(outcome.game.score + 500, session.bestScore)
        assertEquals(null, saved("best.txt"), "an unbeaten best needs no write")
    }

    @Test
    fun aNewBestIsWrittenOnceTheMoveEnds() {
        val game = Game.new(16L)
        val move = findValidMoves(game.board).first()
        val outcome = game.trySwap(move.from, move.to) as SwapOutcome.Accepted
        val scene = Scene().also { it.load(game.board) }
        val session = sessionOf(game, best = 0)

        playToEnd(session.play(move), scene)
        session.finish(scene)
        storeInUse!!.awaitWrites()

        assertTrue(outcome.game.score > 0)
        assertEquals(outcome.game.score, session.bestScore)
        assertEquals(outcome.game.score.toString(), saved("best.txt"))
    }

    @Test
    fun aNewGameResetsTheScoreKeepsBestAndSavesTheFreshBoard() {
        val game = Game.new(17L)
        val move = findValidMoves(game.board).first()
        val scene = Scene().also { it.load(game.board) }
        val session = sessionOf(game, seed = 4242L)
        playToEnd(session.play(move), scene)
        session.finish(scene)
        val best = session.bestScore
        assertTrue(best > 0)

        val timeline = session.newGame(scene)
        playToEnd(timeline, scene)
        session.finish(scene)
        storeInUse!!.awaitWrites()

        val fresh = Game.new(4242L)
        assertEquals(0, session.shownScore)
        assertEquals(best, session.bestScore)
        assertEquals(fresh.board, session.board)
        assertSceneShows(fresh.board, scene)
        assertEquals(fresh.encode(), saved("game.sav"))
        assertEquals(best.toString(), saved("best.txt"))
    }

    @Test
    fun startingANewGameMidAnimationStillCreditsTheAbandonedMovesScoreToBest() {
        val game = Game.new(11L)
        val move = findValidMoves(game.board).first()
        val outcome = game.trySwap(move.from, move.to) as SwapOutcome.Accepted
        val scene = Scene().also { it.load(game.board) }
        val session = sessionOf(game, seed = 5L)

        session.play(move).advance(0.05f, scene) // mid-swap: no clear has finished, so no points counted yet
        session.newGame(scene)
        storeInUse!!.awaitWrites()

        assertTrue(outcome.game.score > 0)
        assertEquals(outcome.game.score, session.bestScore, "the move's points must not be lost from Best")
        assertEquals(outcome.game.score.toString(), saved("best.txt"))
        assertEquals(0, session.shownScore)
    }

    @Test
    fun theScoreResetsTheMomentNewGameStartsNotWhenItsAnimationEnds() {
        val game = Game.new(20L)
        val move = findValidMoves(game.board).first()
        val scene = Scene().also { it.load(game.board) }
        val session = sessionOf(game)
        playToEnd(session.play(move), scene)
        session.finish(scene)
        assertTrue(session.shownScore > 0)

        session.newGame(scene)

        assertEquals(0, session.shownScore)
    }

    @Test
    fun aNewGameConfirmedMidReshuffleLeavesNoNoticeOnTheFreshBoard() {
        val game = Game.new(61L)
        val scene = Scene().also { it.load(game.board) }
        val session = sessionOf(game, seed = 7L)
        val reshuffle = Timeline(listOf(ReshuffleAnimation(Game.new(62L).board)))
        reshuffle.advance(0.4f, scene) // the notice is up and the player confirms New Game right now

        playToEnd(session.newGame(scene), scene)
        session.finish(scene)

        assertSceneShows(session.board, scene)
    }

    @Test
    fun theNewGameTransitionShrinksTheOldGemsThenDropsInTheNewOnes() {
        val game = Game.new(18L)
        val scene = Scene().also { it.load(game.board) }
        val session = sessionOf(game, seed = 5L)
        val fresh = Game.new(5L)
        val timeline = session.newGame(scene)

        timeline.advance(0.1f, scene)
        scene.forEach { assertTrue(it.scale < 1f && it.scale > 0f, "old gems shrink while clearing") }

        timeline.advance(0.11f, scene)
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                assertEquals(fresh.board[Pos(row, col)], scene[Pos(row, col)]?.piece, "new gem at ($row, $col)")
                assertTrue((scene[Pos(row, col)]?.row ?: 99f) < row.toFloat() + 0.01f, "arriving from above")
            }
        }
    }

    @Test
    fun savingOnPauseWritesTheNewestResolvedGameEvenMidAnimation() {
        val game = Game.new(19L)
        val move = findValidMoves(game.board).first()
        val expected = (game.trySwap(move.from, move.to) as SwapOutcome.Accepted).game
        val session = sessionOf(game)
        session.play(move)

        session.save()

        assertEquals(expected.encode(), saved("game.sav"))
        assertFalse(saved("best.txt").isNullOrBlank())
    }
}
