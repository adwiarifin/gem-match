package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CascadeTest {
    /** Diagonal 3-colour pattern has no runs and no moves. Edits below add exactly one move. */
    private val oneMoveBoard =
        boardOf(
            "ROYROYRO",
            "OYROYROY",
            "YROYROYR",
            "ROYROYRO",
            "OYROYROY",
            "YROYROYR",
            "ROYROYRO",
            "YYROYROY",
        )

    private val swapA = Pos(7, 2)
    private val swapB = Pos(6, 2)

    @Test
    fun fixtureHasExactlyTheIntendedMove() {
        assertTrue(findRuns(oneMoveBoard).isEmpty())
        assertEquals(listOf(Move(swapB, swapA)), findValidMoves(oneMoveBoard))
    }

    @Test
    fun simpleMatchEmitsSwapClearFallThenSpawn() {
        val result = resolveSwap(oneMoveBoard, swapA, swapB, Rng(1L))

        val steps = result.steps
        assertEquals(Step.Swap(swapA, swapB), steps[0])

        val clear = steps[1] as Step.Clear
        assertEquals(cells(7 to 0, 7 to 1, 7 to 2), clear.cells)
        assertEquals(30, clear.points)
        assertEquals(1, clear.depth)

        val fall = steps[2] as Step.Fall
        assertEquals(3 * (BOARD_SIZE - 1), fall.moves.size)
        assertTrue(fall.moves.all { it.to.row == it.from.row + 1 && it.to.col == it.from.col })

        val spawn = steps[3] as Step.Spawn
        assertEquals(cells(0 to 0, 0 to 1, 0 to 2), spawn.gems.map { it.pos }.toSet())
    }

    @Test
    fun aSpecialPieceFallsAsAUnit() {
        val special = Piece(Gem.WHITE, Special.BLAST)
        val board = oneMoveBoard.with(Pos(5, 0), special)

        val result = resolveSwap(board, swapA, swapB, Rng(1L))

        val fall = result.steps.filterIsInstance<Step.Fall>().first()
        assertTrue(GemMove(special, Pos(5, 0), Pos(6, 0)) in fall.moves)
        assertEquals(1, result.board.let { b -> (0 until BOARD_SIZE).count { b[Pos(it, 0)] == special } })
        assertEquals(result.board, replay(board, result.steps))
    }

    @Test
    fun stepsDescribeTheWholeTransformationOnManyBoards() {
        var checked = 0
        for (seed in 1L..SEEDS) {
            val board = randomStableBoard(seed)
            val move = findValidMoves(board).firstOrNull() ?: continue

            val result = resolveSwap(board, move.from, move.to, Rng(seed))

            assertEquals(result.board, replay(board, result.steps), "seed $seed")
            assertTrue(isFull(result.board), "seed $seed")
            assertTrue(findRuns(result.board).isEmpty(), "seed $seed")
            assertEquals(result.steps.filterIsInstance<Step.Clear>().sumOf { it.points }, result.score, "seed $seed")
            checked++
        }
        assertTrue(checked > SEEDS / 2, "too few boards had a move: $checked")
    }

    @Test
    fun cascadeDepthCountsUpFromOne() {
        var sawCascade = false
        for (seed in 1L..SEEDS) {
            val board = randomStableBoard(seed)
            val move = findValidMoves(board).firstOrNull() ?: continue

            val steps = resolveSwap(board, move.from, move.to, Rng(seed)).steps
            val depths = steps.filterIsInstance<Step.Clear>().map { it.depth }

            assertEquals((1..depths.size).toList(), depths, "seed $seed")
            if (depths.size >= 2) sawCascade = true
        }
        assertTrue(sawCascade, "no cascade appeared in $SEEDS seeds")
    }

    @Test
    fun fallsMoveGemsDownWithinTheirColumn() {
        for (seed in 1L..SEEDS) {
            val board = randomStableBoard(seed)
            val move = findValidMoves(board).firstOrNull() ?: continue

            val falls = resolveSwap(board, move.from, move.to, Rng(seed)).steps.filterIsInstance<Step.Fall>()

            val moves = falls.flatMap { it.moves }
            assertTrue(moves.all { it.to.col == it.from.col && it.to.row > it.from.row }, "seed $seed")
        }
    }

    @Test
    fun sameInputsGiveIdenticalResults() {
        val board = randomStableBoard(7L)
        val move = findValidMoves(board).first()

        val first = resolveSwap(board, move.from, move.to, Rng(42L))
        val second = resolveSwap(board, move.from, move.to, Rng(42L))

        assertEquals(first, second)
    }

    @Test
    fun rngIsAdvancedByExactlyTheSpawnedGems() {
        val rng = Rng(5L)

        val result = resolveSwap(oneMoveBoard, swapA, swapB, rng)

        val spawned = result.steps.filterIsInstance<Step.Spawn>().sumOf { it.gems.size }
        val reference = Rng(5L)
        repeat(spawned) { reference.nextGem() }
        assertEquals(reference.state, rng.state)
    }

    @Test
    fun invalidSwapIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            resolveSwap(oneMoveBoard, Pos(0, 0), Pos(0, 1), Rng(1L))
        }
    }

    private companion object {
        const val SEEDS = 300L
    }
}
