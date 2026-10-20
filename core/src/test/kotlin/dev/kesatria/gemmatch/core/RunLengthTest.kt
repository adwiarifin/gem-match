package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * How long a run lasts before the game is over (the board has no valid move). The sample is small so the suite
 * stays fast; set `RUN_LENGTH_SEEDS`
 * (and optionally `RUN_LENGTH_CAP`) to measure properly and read the report with `./gradlew :core:test -i`.
 */
class RunLengthTest {
    @Test
    fun theSameSeedAndPlayerGiveTheSameRun() {
        for (player in RunPlayer.entries) {
            assertEquals(playUntilDead(7L, player, SMALL_CAP), playUntilDead(7L, player, SMALL_CAP), "$player")
        }
    }

    @Test
    fun aRunEndsAtTheMoveThatLeavesTheDeadBoardAndNotBefore() {
        var ended = 0
        for (player in RunPlayer.entries) {
            for (seed in 1L..SMALL_SEEDS) {
                val run = playUntilDead(seed, player, SMALL_CAP)
                if (!run.ended) continue
                ended++
                val before = playUntilDead(seed, player, run.moves - 1)
                assertFalse(before.ended, "$player seed $seed: dead board came earlier than move ${run.moves}")
                assertEquals(run, playUntilDead(seed, player, run.moves), "$player seed $seed")
            }
        }
        assertTrue(ended > 0, "no run ended in the sample, so the end rule was never checked")
    }

    @Test
    fun aRunThatNeverGetsDeadStopsAtTheCap() {
        val run = playUntilDead(1L, RunPlayer.RANDOM, cap = 3)

        assertFalse(run.ended)
        assertEquals(3, run.moves)
    }

    @Test
    fun theGreedyPlayerTakesAMatchMoveWheneverThereIsOne() {
        var game = Game.new(5L)
        val chooser = Rng(5L)
        repeat(SMALL_CAP) {
            val matchMoves = findMatchMoves(game.board)
            val move = RunPlayer.GREEDY.choose(game.board, chooser)
            if (matchMoves.isNotEmpty()) assertTrue(move in matchMoves, "turn $it")
            game = (game.trySwap(move.from, move.to) as SwapOutcome.Accepted).game
        }
    }

    @Test
    fun percentilesPickTheNearestRank() {
        val lengths = (1..10).toList()

        assertEquals(1, percentile(lengths, 10))
        assertEquals(5, percentile(lengths, 50))
        assertEquals(9, percentile(lengths, 90))
        assertEquals(10, percentile(lengths, 100))
        assertNull(percentile(emptyList(), 50))
    }

    @Test
    fun theReportCountsRunsThatHitTheCap() {
        val runs = listOf(PlayedRun(10, true), PlayedRun(20, true), PlayedRun(30, true), PlayedRun(40, false))

        val report = summarize(runs, cap = 40)

        assertEquals(4, report.games)
        assertEquals(1, report.capped)
        assertEquals(20, report.median)
    }

    @Test
    fun measureRunLengthOnTheSeedsAskedFor() {
        val seeds = System.getenv("RUN_LENGTH_SEEDS")?.toLongOrNull() ?: SMALL_SEEDS
        val cap = System.getenv("RUN_LENGTH_CAP")?.toIntOrNull() ?: SMALL_CAP
        for (player in RunPlayer.entries) {
            val runs = (1L..seeds).map { playUntilDead(it, player, cap) }
            val report = summarize(runs, cap)
            println("RUN LENGTH $player: ${report.describe()}")
            assertTrue(runs.all { it.moves in 1..cap }, "$player: a run is outside 1..$cap moves")
        }
    }

    private companion object {
        const val SMALL_SEEDS = 20L
        const val SMALL_CAP = 300
    }
}
