package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ActivationTest {
    /** The red, orange, yellow diagonal has no runs and no moves; only the cells passed in are overwritten. */
    private fun patterned(vararg overrides: Pair<Pos, Piece>): Board {
        val rows =
            (0 until BOARD_SIZE).map { row ->
                CharArray(BOARD_SIZE) { col -> "ROY"[(row + col) % 3] }
            }
        val base = boardOf(*rows.map { String(it) }.toTypedArray())
        return overrides.fold(base) { board, (pos, piece) -> board.with(pos, piece) }
    }

    private fun square(
        rows: IntRange,
        cols: IntRange,
    ) = rows.flatMap { row -> cols.map { col -> Pos(row, col) } }.toSet()

    private fun row(index: Int) = (0 until BOARD_SIZE).map { Pos(index, it) }.toSet()

    private fun column(index: Int) = (0 until BOARD_SIZE).map { Pos(it, index) }.toSet()

    @Test
    fun blastCoversTheThreeByThreeAroundIt() {
        assertEquals(square(3..5, 3..5), effectCells(Pos(4, 4), Special.BLAST))
    }

    @Test
    fun blastIsClippedAtEdgesAndCorners() {
        assertEquals(cells(0 to 0, 0 to 1, 1 to 0, 1 to 1), effectCells(Pos(0, 0), Special.BLAST))
        assertEquals(square(6..7, 6..7), effectCells(Pos(7, 7), Special.BLAST))
        assertEquals(square(0..1, 2..4), effectCells(Pos(0, 3), Special.BLAST))
        assertEquals(square(3..5, 6..7), effectCells(Pos(4, 7), Special.BLAST))
    }

    @Test
    fun lineCoversItsWholeRowOrColumn() {
        assertEquals(row(2), effectCells(Pos(2, 5), Special.LINE_ROW))
        assertEquals(column(5), effectCells(Pos(2, 5), Special.LINE_COL))
        assertEquals(row(0), effectCells(Pos(0, 0), Special.LINE_ROW))
        assertEquals(column(7), effectCells(Pos(7, 7), Special.LINE_COL))
    }

    @Test
    fun crossCoversItsRowAndColumn() {
        assertEquals(row(3) + column(3), effectCells(Pos(3, 3), Special.CROSS))
        assertEquals(15, effectCells(Pos(3, 3), Special.CROSS).size)
        assertEquals(row(0) + column(0), effectCells(Pos(0, 0), Special.CROSS))
        assertEquals(15, effectCells(Pos(0, 0), Special.CROSS).size)
    }

    @Test
    fun aSpecialSwapWithNoMatchIsAcceptedButTwoPlainGemsAreNot() {
        val board = patterned(Pos(4, 4) to Piece(Gem.WHITE, Special.BLAST))
        val game = assertNotNull(Game.decode(encodeFor(board)))

        assertIs<SwapOutcome.Accepted>(game.trySwap(Pos(4, 4), Pos(4, 5)))
        assertIs<SwapOutcome.Rejected>(game.trySwap(Pos(1, 1), Pos(1, 2)))
    }

    @Test
    fun theSwappedSpecialFiresFromTheCellItMovedTo() {
        val board = patterned(Pos(4, 4) to Piece(Gem.WHITE, Special.BLAST))

        val result = resolveSwap(board, Pos(4, 4), Pos(4, 5), Rng(1L))

        val activate = result.steps[1] as Step.Activate
        assertEquals(Pos(4, 5), activate.origin)
        assertEquals(Special.BLAST, activate.special)
        assertEquals(square(3..5, 4..6), activate.cells)
    }

    @Test
    fun theSpecialFiresWhicheverSideTheNeighbourIsOn() {
        val board = patterned(Pos(4, 4) to Piece(Gem.WHITE, Special.LINE_ROW))

        val dragged = resolveSwap(board, Pos(4, 4), Pos(5, 4), Rng(1L)).steps[1] as Step.Activate
        val received = resolveSwap(board, Pos(5, 4), Pos(4, 4), Rng(1L)).steps[1] as Step.Activate

        assertEquals(Pos(5, 4), dragged.origin)
        assertEquals(row(5), dragged.cells)
        assertEquals(Pos(5, 4), received.origin)
        assertEquals(row(5), received.cells)
    }

    @Test
    fun theEffectClearsExactlyItsCellsInOneClearThatScoresTenEach() {
        val board = patterned(Pos(4, 4) to Piece(Gem.WHITE, Special.BLAST))

        val result = resolveSwap(board, Pos(4, 4), Pos(4, 5), Rng(1L))

        val clear = result.steps[2] as Step.Clear
        assertEquals(square(3..5, 4..6), clear.cells)
        assertEquals(9 * POINTS_PER_EFFECT_GEM, clear.points)
        assertEquals(1, clear.depth)
    }

    @Test
    fun stepOrderIsSwapActivateClearFallSpawnWithNoCreate() {
        val board = patterned(Pos(4, 4) to Piece(Gem.WHITE, Special.CROSS))

        val steps = resolveSwap(board, Pos(4, 4), Pos(4, 5), Rng(1L)).steps

        assertTrue(steps[0] is Step.Swap)
        assertTrue(steps[1] is Step.Activate)
        assertTrue(steps[2] is Step.Clear)
        assertTrue(steps[3] is Step.Fall)
        assertTrue(steps[4] is Step.Spawn)
        assertTrue(steps.none { it is Step.Create })
    }

    @Test
    fun aNormalMatchFormedByTheSameSwapClearsInTheSameClear() {
        // The blast moves right; the plain green moving left lands beside two greens and makes a run of three.
        val board =
            patterned(
                Pos(4, 2) to Piece(Gem.GREEN),
                Pos(4, 3) to Piece(Gem.GREEN),
                Pos(4, 4) to Piece(Gem.WHITE, Special.BLAST),
                Pos(4, 5) to Piece(Gem.GREEN),
            )

        val result = resolveSwap(board, Pos(4, 4), Pos(4, 5), Rng(1L))

        val clear = result.steps.filterIsInstance<Step.Clear>().first()
        assertEquals(square(3..5, 4..6) + cells(4 to 2, 4 to 3), clear.cells)
        assertEquals(POINTS_RUN_OF_3 + 8 * POINTS_PER_EFFECT_GEM, clear.points, "the shared cell scores once")
        assertEquals(1, result.steps.filterIsInstance<Step.Clear>().count { it.depth == 1 })
        assertTrue(result.steps.none { it is Step.Create })
    }

    @Test
    fun cascadesAfterAnEffectRunAtDepthTwoAndUp() {
        var cascaded = 0
        for (seed in 1L..SEEDS) {
            val board = patterned(Pos(4, 4) to Piece(Gem.WHITE, Special.CROSS))

            val clears = resolveSwap(board, Pos(4, 4), Pos(4, 5), Rng(seed)).steps.filterIsInstance<Step.Clear>()

            assertEquals(1, clears.first().depth, "seed $seed")
            assertEquals((2 until clears.size + 1).toList(), clears.drop(1).map { it.depth }, "seed $seed")
            if (clears.size > 1) cascaded++
        }
        assertTrue(cascaded > 0, "no seed cascaded")
    }

    @Test
    fun theBoardStaysFullAndReplaysAfterAnyEffect() {
        for (special in listOf(Special.BLAST, Special.LINE_ROW, Special.LINE_COL, Special.CROSS)) {
            for (seed in 1L..SEEDS) {
                val board = patterned(Pos(2, 2) to Piece(Gem.WHITE, special))

                val result = resolveSwap(board, Pos(2, 2), Pos(3, 2), Rng(seed))

                assertTrue(isFull(result.board), "$special seed $seed")
                assertEquals(result.board, replay(board, result.steps), "$special seed $seed")
            }
        }
    }

    private fun every(
        board: Board,
        gem: Gem,
    ) = (0 until BOARD_SIZE)
        .flatMap { row -> (0 until BOARD_SIZE).map { col -> Pos(row, col) } }
        .filter { board[it]?.gem == gem }
        .toSet()

    @Test
    fun aColourGemClearsEveryGemOfItsPartnersColourAndItself() {
        val colour = Piece(null, Special.COLOUR)
        val board = patterned(Pos(2, 2) to colour)
        val swapped = board.swap(Pos(2, 2), Pos(2, 3))
        val partner = checkNotNull(swapped[Pos(2, 2)]?.gem)
        val expected = every(swapped, partner) + Pos(2, 3)

        val result = resolveSwap(board, Pos(2, 2), Pos(2, 3), Rng(1L))

        val activate = result.steps[1] as Step.Activate
        assertEquals(Special.COLOUR, activate.special)
        assertEquals(Pos(2, 3), activate.origin)
        assertEquals(expected, activate.cells)
        val clear = result.steps[2] as Step.Clear
        assertEquals(expected, clear.cells)
        assertEquals(expected.size * POINTS_PER_EFFECT_GEM, clear.points)
    }

    @Test
    fun aColourGemWorksWhenItsPartnerIsTheOnlyGemOfItsColour() {
        val board = patterned(Pos(2, 2) to Piece(null, Special.COLOUR), Pos(2, 3) to Piece(Gem.WHITE))

        val result = resolveSwap(board, Pos(2, 2), Pos(2, 3), Rng(1L))

        val clear = result.steps.filterIsInstance<Step.Clear>().first()
        assertEquals(cells(2 to 2, 2 to 3), clear.cells)
        assertEquals(2 * POINTS_PER_EFFECT_GEM, clear.points)
    }

    @Test
    fun aColourGemFormsNoRunEvenBesideGemsOfOneColour() {
        val board =
            patterned(
                Pos(3, 2) to Piece(Gem.WHITE),
                Pos(3, 4) to Piece(Gem.WHITE),
                Pos(4, 3) to Piece(null, Special.COLOUR),
            )
        val swapped = board.swap(Pos(4, 3), Pos(3, 3))
        assertTrue(findRuns(swapped).isEmpty())
        val expected = every(swapped, checkNotNull(swapped[Pos(4, 3)]?.gem)) + Pos(3, 3)

        val result = resolveSwap(board, Pos(4, 3), Pos(3, 3), Rng(1L))

        assertEquals(
            expected,
            result.steps
                .filterIsInstance<Step.Clear>()
                .first()
                .cells,
        )
    }

    private fun assertNotNull(game: Game?): Game = checkNotNull(game)

    private fun encodeFor(board: Board): String {
        val cells =
            (0 until BOARD_SIZE).joinToString("") { r ->
                (0 until BOARD_SIZE).joinToString("") { c ->
                    val piece = checkNotNull(board[Pos(r, c)])
                    val letter = mapOf(Special.BLAST to 'B', Special.LINE_ROW to 'R', Special.LINE_COL to 'C', Special.CROSS to 'X')
                    "${piece.gem?.ordinal ?: '-'}${piece.special?.let { letter.getValue(it) } ?: '-'}"
                }
            }
        return "GM2;0;1;$cells"
    }

    private companion object {
        const val SEEDS = 40L
    }
}
