package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Seeded games played with random legal moves, special swaps included, checking the rules after every move. */
class SoakTest {
    private class Tally {
        var created = 0
        var fired = 0
        var specialMoves = 0
        var overs = 0
    }

    private fun hasSpecial(board: Board) = ALL_CELLS.any { board[it]?.isSpecial == true }

    private fun play(
        seed: Long,
        tally: Tally,
        onMove: (Game, SwapOutcome.Accepted) -> Unit = { _, _ -> },
    ): Pair<Game, List<List<Step>>> {
        var game = Game.new(seed)
        val chooser = Rng(seed * CHOOSER_SALT)
        val history = mutableListOf<List<Step>>()
        for (turn in 0 until MOVES_PER_GAME) {
            if (game.isOver) {
                tally.overs++
                break
            }
            val moves = findValidMoves(game.board)
            val move = moves[chooser.nextInt(moves.size)]
            val outcome = game.trySwap(move.from, move.to)
            assertIs<SwapOutcome.Accepted>(outcome, "seed $seed turn $turn")
            onMove(game, outcome)
            tally.created += outcome.steps.count { it is Step.Create }
            tally.fired += outcome.steps.count { it is Step.Activate }
            if (listOf(move.from, move.to).any { game.board[it]?.isSpecial == true }) tally.specialMoves++
            history += outcome.steps
            game = outcome.game
        }
        return game to history
    }

    @Test
    fun theRulesHoldAfterEveryMoveOfManyGames() {
        val tally = Tally()
        for (seed in 1L..SEEDS) {
            play(seed, tally) { before, outcome ->
                val after = outcome.game
                val label = "seed $seed"
                assertTrue(isFull(after.board), label)
                assertTrue(findRuns(after.board).isEmpty(), label)
                if (!after.isOver) assertTrue(hasValidMove(after.board), label)
                val restored = assertNotNull(Game.decode(after.encode()), label)
                assertEquals(after.board, restored.board, label)
                assertEquals(after.score, restored.score, label)
                assertEquals(after.board, replay(before.board, outcome.steps), label)
                val total = outcome.steps.filterIsInstance<Step.Clear>().sumOf { it.points }
                assertEquals(after.score - before.score, total, label)
            }
        }
        assertTrue(tally.created > 0, "the soak never made a special")
        assertTrue(tally.fired > 0, "the soak never fired a special")
        assertTrue(tally.specialMoves > 0, "the soak never swapped a special")
    }

    @Test
    fun everyKindOfSpecialSurvivesAGameOfRandomMoves() {
        for (special in Special.entries) {
            val piece = Piece(if (special == Special.COLOUR) null else Gem.GREEN, special)
            var game = gameOf(patterned(Pos(3, 3) to piece, Pos(5, 5) to piece))
            val chooser = Rng(special.ordinal + 1L)
            for (turn in 0 until KIND_MOVES) {
                if (game.isOver) break
                val moves = findValidMoves(game.board)
                val move = moves[chooser.nextInt(moves.size)]

                val outcome = game.trySwap(move.from, move.to)

                assertIs<SwapOutcome.Accepted>(outcome, "$special turn $turn")
                assertEquals(outcome.game.board, replay(game.board, outcome.steps), "$special turn $turn")
                assertTrue(isFull(outcome.game.board), "$special turn $turn")
                assertTrue(findRuns(outcome.game.board).isEmpty(), "$special turn $turn")
                if (!outcome.game.isOver) assertTrue(hasValidMove(outcome.game.board), "$special turn $turn")
                val restored = assertNotNull(Game.decode(outcome.game.encode()))
                assertEquals(outcome.game.board, restored.board, "$special turn $turn")
                game = outcome.game
            }
        }
    }

    @Test
    fun aBoardOfNothingButSpecialsStillResolvesAndSettles() {
        val kinds = Special.entries.filter { it != Special.COLOUR }
        val everyCell = (0 until BOARD_SIZE).flatMap { r -> (0 until BOARD_SIZE).map { c -> Pos(r, c) } }
        val allSpecial =
            patterned(
                *everyCell
                    .map {
                        it to
                            Piece(
                                gemOf("ROY"[(it.row + it.col) % 3]),
                                kinds[(it.row * 3 + it.col) % kinds.size],
                            )
                    }.toTypedArray(),
            )
        val game = gameOf(allSpecial)

        for (move in findValidMoves(allSpecial).take(40)) {
            val outcome = game.trySwap(move.from, move.to)

            assertIs<SwapOutcome.Accepted>(outcome, "$move")
            assertTrue(outcome.steps.size < MAX_STEPS, "$move took ${outcome.steps.size} steps")
            assertTrue(isFull(outcome.game.board), "$move")
            assertTrue(findRuns(outcome.game.board).isEmpty(), "$move")
        }
    }

    @Test
    fun aBoardHoldingASpecialIsNeverOver() {
        val tally = Tally()
        var withSpecial = 0
        for (seed in 1L..SEEDS) {
            play(seed, tally) { _, outcome ->
                if (hasSpecial(outcome.game.board)) {
                    withSpecial++
                    assertTrue(!outcome.game.isOver, "seed $seed ended on a board holding a special")
                }
            }
        }
        assertTrue(withSpecial > 0, "no board held a special, so this checked nothing")
        assertTrue(tally.overs > 0, "no game ended, so the end was never checked")
    }

    @Test
    fun aGameThatEndsKeepsTheBoardItsStepsLeftAndStillSaves() {
        var ended = 0
        for (seed in 1L..SEEDS) {
            var game = Game.new(seed)
            val chooser = Rng(seed * CHOOSER_SALT)
            while (!game.isOver) {
                val moves = findValidMoves(game.board)
                val move = moves[chooser.nextInt(moves.size)]
                val outcome = game.trySwap(move.from, move.to) as SwapOutcome.Accepted
                assertEquals(outcome.game.board, replay(game.board, outcome.steps), "seed $seed")
                game = outcome.game
            }
            ended++
            assertTrue(findValidMoves(game.board).isEmpty(), "seed $seed")
            assertTrue(assertNotNull(Game.decode(game.encode())).isOver, "seed $seed")
        }
        assertEquals(SEEDS, ended.toLong())
    }

    @Test
    fun theSameSeedAndMovesGiveTheSameStepsAndScore() {
        for (seed in 1L..REPEAT_SEEDS) {
            val (first, firstSteps) = play(seed, Tally())
            val (second, secondSteps) = play(seed, Tally())

            assertEquals(firstSteps, secondSteps, "seed $seed")
            assertEquals(first.score, second.score, "seed $seed")
            assertEquals(first.encode(), second.encode(), "seed $seed")
        }
    }

    private companion object {
        const val SEEDS = 120L
        const val REPEAT_SEEDS = 30L
        const val MOVES_PER_GAME = 80
        const val KIND_MOVES = 40
        const val MAX_STEPS = 5_000
        const val CHOOSER_SALT = 7919L
    }
}
