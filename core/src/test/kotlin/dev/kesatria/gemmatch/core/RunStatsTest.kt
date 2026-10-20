package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunStatsTest {
    private fun greens(vararg cells: Pair<Int, Int>): Board {
        val greenCells = cells.map { Pos(it.first, it.second) to Piece(Gem.GREEN) }
        return patternedBoard(greenCells)
    }

    /** The outcome for the first seed whose move satisfies [accept], so the fixture does not depend on luck. */
    private fun firstSeedWhere(
        board: Board,
        from: Pos,
        to: Pos,
        accept: (List<Step>) -> Boolean,
    ): SwapOutcome.Accepted {
        for (seed in 1L..SEARCH_SEEDS) {
            val outcome = gameOf(board, rng = seed).trySwap(from, to)
            if (outcome is SwapOutcome.Accepted && accept(outcome.steps)) return outcome
        }
        error("no seed in 1..$SEARCH_SEEDS gave the move the fixture needs")
    }

    private fun clears(steps: List<Step>) = steps.filterIsInstance<Step.Clear>()

    @Test
    fun aNewRunHasNoStats() {
        assertEquals(RunStats.EMPTY, Game.new(1L).stats)
        assertEquals(0, RunStats.EMPTY.moves)
        assertEquals(0, RunStats.EMPTY.created[Special.BLAST] ?: 0)
    }

    @Test
    fun aPlainThreeMatchCountsOneMoveThreeGemsAndAMatchOfThree() {
        val board = greens(3 to 1, 3 to 2, 4 to 3)
        val outcome = firstSeedWhere(board, Pos(4, 3), Pos(3, 3)) { clears(it).size == 1 }

        val stats = outcome.game.stats

        assertEquals(1, stats.moves)
        assertEquals(1, stats.cascade)
        assertEquals(3, stats.gems)
        assertEquals(3, stats.match)
        assertEquals(30, stats.moveScore)
        assertEquals(outcome.game.score, stats.moveScore)
        assertTrue(stats.created.values.all { it == 0 } && stats.fired.values.all { it == 0 })
    }

    @Test
    fun aCascadeRaisesTheLongestCascadeAndCountsEveryClearedGem() {
        val board = greens(3 to 1, 3 to 2, 4 to 3)
        val outcome = firstSeedWhere(board, Pos(4, 3), Pos(3, 3)) { clears(it).size == 2 }

        val stats = outcome.game.stats

        assertEquals(2, stats.cascade)
        assertEquals(clears(outcome.steps).sumOf { it.cells.size }, stats.gems)
        assertEquals(outcome.game.score, stats.moveScore)
    }

    @Test
    fun aRunOfFourCountsTheBlastItMadeAndAMatchOfFour() {
        val board = greens(3 to 1, 3 to 2, 3 to 4, 4 to 3)
        val outcome = firstSeedWhere(board, Pos(4, 3), Pos(3, 3)) { clears(it).size == 1 }

        val stats = outcome.game.stats

        assertEquals(4, stats.match)
        assertEquals(1, stats.created[Special.BLAST])
        assertEquals(4, stats.gems)
        assertTrue(stats.fired.values.all { it == 0 })
    }

    @Test
    fun aBlastSwappedIntoAnEmptyNeighbourhoodCountsOneFireAndNineGemsOnce() {
        val board = patterned(Pos(4, 4) to Piece(Gem.WHITE, Special.BLAST))
        val outcome = firstSeedWhere(board, Pos(4, 4), Pos(4, 5)) { clears(it).size == 1 }

        val stats = outcome.game.stats

        assertEquals(1, stats.fired[Special.BLAST])
        assertEquals(9, stats.gems)
        assertEquals(90, stats.moveScore)
    }

    @Test
    fun aCellClearedByARunAndAnEffectCountsOnce() {
        val board =
            patternedBoard(
                listOf(
                    Pos(4, 4) to Piece(Gem.WHITE, Special.BLAST),
                    Pos(3, 6) to Piece(Gem.GREEN),
                    Pos(3, 7) to Piece(Gem.GREEN),
                    Pos(4, 6) to Piece(Gem.GREEN),
                ),
            )
        val outcome = firstSeedWhere(board, Pos(4, 4), Pos(4, 5)) { clears(it).size >= 1 }

        val first = clears(outcome.steps).first()

        assertTrue(Pos(3, 6) in first.cells, "the green run sits inside the blast")
        assertEquals(clears(outcome.steps).sumOf { it.cells.size }, outcome.game.stats.gems)
    }

    @Test
    fun movesAddUpCascadeAndMoveScoreAndMatchKeepTheBestAndGemsAndSpecialsAdd() {
        val first = RunStats(1, 2, 90, 10, 4, mapOf(Special.BLAST to 1), mapOf(Special.CROSS to 2))
        val second = RunStats(1, 5, 60, 7, 3, mapOf(Special.BLAST to 1, Special.COLOUR to 1), mapOf(Special.CROSS to 1))

        val both = first + second

        assertEquals(2, both.moves)
        assertEquals(5, both.cascade)
        assertEquals(90, both.moveScore)
        assertEquals(17, both.gems)
        assertEquals(4, both.match)
        assertEquals(mapOf(Special.BLAST to 2, Special.COLOUR to 1), both.created)
        assertEquals(mapOf(Special.CROSS to 3), both.fired)
    }

    @Test
    fun addingStatsIsAssociativeAndEmptyChangesNothing() {
        val a = RunStats(1, 2, 90, 10, 4, mapOf(Special.BLAST to 1), emptyMap())
        val b = RunStats(1, 1, 30, 3, 3, emptyMap(), mapOf(Special.LINE_ROW to 1))
        val c = RunStats(1, 4, 150, 12, 5, mapOf(Special.COLOUR to 1), mapOf(Special.BLAST to 2))

        assertEquals((a + b) + c, a + (b + c))
        assertEquals(a, a + RunStats.EMPTY)
        assertEquals(a, RunStats.EMPTY + a)
    }

    @Test
    fun theStatsOfAWholeGameMatchWhatTheStepsAndScoresSay() {
        for (seed in 1L..GAMES) {
            var game = Game.new(seed)
            val chooser = Rng(seed * CHOOSER_SALT)
            var moves = 0
            var cascade = 0
            var best = 0
            var gems = 0
            var created = 0
            var fired = 0
            while (!game.isOver && moves < MOVE_CAP) {
                val options = findValidMoves(game.board)
                val pick = options[chooser.nextInt(options.size)]
                val outcome = game.trySwap(pick.from, pick.to)
                assertIs<SwapOutcome.Accepted>(outcome, "seed $seed")
                moves++
                cascade = maxOf(cascade, clears(outcome.steps).maxOf { it.depth })
                best = maxOf(best, outcome.game.score - game.score)
                gems += clears(outcome.steps).sumOf { it.cells.size }
                created += outcome.steps.count { it is Step.Create }
                fired += outcome.steps.count { it is Step.Activate }
                game = outcome.game
            }
            val stats = game.stats
            assertEquals(moves, stats.moves, "seed $seed")
            assertEquals(cascade, stats.cascade, "seed $seed")
            assertEquals(best, stats.moveScore, "seed $seed")
            assertEquals(gems, stats.gems, "seed $seed")
            assertEquals(created, stats.created.values.sum(), "seed $seed")
            assertEquals(fired, stats.fired.values.sum(), "seed $seed")
            assertTrue(stats.match in 3..BOARD_SIZE * BOARD_SIZE, "seed $seed")
        }
    }

    @Test
    fun aMoveLeavesTheOriginalGamesStatsAlone() {
        val game = Game.new(5L)
        val move = findValidMoves(game.board).first()

        game.trySwap(move.from, move.to)

        assertEquals(RunStats.EMPTY, game.stats)
    }

    private companion object {
        const val SEARCH_SEEDS = 20_000L
        const val GAMES = 40L
        const val MOVE_CAP = 400
        const val CHOOSER_SALT = 7919L
    }
}
