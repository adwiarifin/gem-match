package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpecialScoringTest {
    private fun firstClear(
        board: Board,
        a: Pos,
        b: Pos,
    ) = resolveSwap(board, a, b, Rng(1L)).steps.filterIsInstance<Step.Clear>().first()

    @Test
    fun theEffectConstantIsTen() {
        assertEquals(10, POINTS_PER_EFFECT_GEM)
    }

    @Test
    fun aBlastScoresNinetyAtDepthOne() {
        val board = patterned(Pos(4, 4) to Piece(Gem.WHITE, Special.BLAST))

        assertEquals(90, firstClear(board, Pos(4, 4), Pos(4, 5)).points)
    }

    @Test
    fun aBlastAtACornerScoresOnlyTheCellsOnTheBoard() {
        val board = patterned(Pos(0, 1) to Piece(Gem.WHITE, Special.BLAST))

        assertEquals(40, firstClear(board, Pos(0, 1), Pos(0, 0)).points)
    }

    @Test
    fun aLineScoresEightyAndACrossFifteenCells() {
        val line = patterned(Pos(4, 4) to Piece(Gem.WHITE, Special.LINE_ROW))
        val cross = patterned(Pos(4, 4) to Piece(Gem.WHITE, Special.CROSS))

        assertEquals(80, firstClear(line, Pos(4, 4), Pos(4, 5)).points)
        assertEquals(150, firstClear(cross, Pos(4, 4), Pos(4, 5)).points)
    }

    @Test
    fun twoColourGemsScoreSixHundredFortyAtDepthOne() {
        val colour = Piece(null, Special.COLOUR)
        val board = patterned(Pos(4, 4) to colour, Pos(4, 5) to colour)

        assertEquals(640, firstClear(board, Pos(4, 4), Pos(4, 5)).points)
    }

    @Test
    fun aChainScoresEveryDistinctCellOnce() {
        val board =
            patterned(
                Pos(4, 4) to Piece(Gem.WHITE, Special.BLAST),
                Pos(3, 5) to Piece(Gem.BLUE, Special.LINE_ROW),
            )

        // The blast's nine cells and the line's eight, three of which are shared: fourteen cells.
        assertEquals(140, firstClear(board, Pos(4, 4), Pos(4, 5)).points)
    }

    @Test
    fun aCellClearedByARunAndAnEffectScoresOnce() {
        val board =
            patterned(
                Pos(4, 2) to Piece(Gem.GREEN),
                Pos(4, 3) to Piece(Gem.GREEN),
                Pos(4, 4) to Piece(Gem.WHITE, Special.BLAST),
                Pos(4, 5) to Piece(Gem.GREEN),
            )

        assertEquals(POINTS_RUN_OF_3 + 8 * POINTS_PER_EFFECT_GEM, firstClear(board, Pos(4, 4), Pos(4, 5)).points)
    }

    @Test
    fun makingASpecialAddsNothing() {
        val greens = listOf(3 to 1, 3 to 2, 3 to 4, 4 to 3).map { Pos(it.first, it.second) to Piece(Gem.GREEN) }
        val board = patterned(*greens.toTypedArray())

        val result = resolveSwap(board, Pos(4, 3), Pos(3, 3), Rng(1L))

        assertEquals(
            POINTS_RUN_OF_4,
            result.steps
                .filterIsInstance<Step.Clear>()
                .first()
                .points,
        )
    }

    @Test
    fun theClearPointsAlwaysAddUpToTheScoreWithSpecialsOnTheBoard() {
        val specials = Special.entries.filter { it != Special.COLOUR }
        var checked = 0
        for (seed in 1L..SEEDS) {
            val special = Piece(Gem.WHITE, specials[(seed % specials.size).toInt()])
            val colour = Piece(null, Special.COLOUR)
            val board = randomStableBoard(seed).with(Pos(2, 2), special).with(Pos(5, 5), colour)

            for (move in listOf(Move(Pos(2, 2), Pos(2, 3)), Move(Pos(5, 5), Pos(5, 6)), Move(Pos(2, 2), Pos(3, 2)))) {
                val result = resolveSwap(board, move.from, move.to, Rng(seed))

                val total = result.steps.filterIsInstance<Step.Clear>().sumOf { it.points }
                assertEquals(total, result.score, "seed $seed $move")
                assertTrue(result.score > 0, "seed $seed $move")
                checked++
            }
        }
        assertEquals(SEEDS * 3, checked.toLong())
    }

    private companion object {
        const val SEEDS = 60L
    }
}
