package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A save one move from the end, for checking game over on a real phone (see `docs/game-over-runbook.md`). A dead board
 * is far too rare to reach by playing, and the release build carries no debug switch. These strings are pasted into
 * the runbook, so this test fails if the board stops being one move from dead.
 */
class NearDeadFixtureTest {
    private fun fixture(extra: String = "started=1000,active=2000") = "$HEAD$STATS,$extra,end=1"

    @Test
    fun theFixtureLoadsAsAGameInProgressWithOneValidMove() {
        val game = assertNotNull(Game.decode(fixture()))

        assertFalse(game.isOver)
        assertEquals(listOf(Move(Pos(4, 1), Pos(5, 1))), findValidMoves(game.board))
        assertEquals(12_430, game.score)
        assertEquals(184, game.stats.moves)
    }

    @Test
    fun theBoardHoldsNoSpecialSoThatItCanDie() {
        val game = assertNotNull(Game.decode(fixture()))

        assertTrue(ALL_CELLS.none { game.board[it]?.isSpecial == true })
    }

    @Test
    fun theOneMoveLeavesADeadBoard() {
        val game = assertNotNull(Game.decode(fixture()))

        val outcome = game.trySwap(Pos(4, 1), Pos(5, 1)) as SwapOutcome.Accepted

        assertTrue(outcome.game.isOver)
        assertEquals(185, outcome.game.stats.moves)
        assertTrue(outcome.game.score > 12_430)
    }

    @Test
    fun theRunbookCanAppendItsOwnClocks() {
        val game = assertNotNull(Game.decode(fixture("started=1760000000000,active=600000")))

        assertEquals(RunTime(1_760_000_000_000L, 600_000L), game.time)
    }

    private companion object {
        const val HEAD =
            "GM3;12430;6923093808337748099;" +
                "2-5-4-1-3-4-0-1-5-3-2-3-5-6-5-6-4-6-6-4-0-1-0-1-2-3-5-3-4-5-4-6-" +
                "1-4-1-5-2-1-5-3-4-6-4-6-2-2-6-1-2-3-0-5-1-5-1-4-5-1-2-0-6-0-4-0-;"
        const val STATS =
            "moves=184,cascade=5,moveScore=450,gems=970,match=5," +
                "cB=10,cR=22,cC=18,cS=1,cW=1,tB=10,tR=22,tC=18,tS=1,tW=1"
    }
}
