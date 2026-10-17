package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComboTest {
    private val colour = Piece(null, Special.COLOUR)
    private val effects = Special.entries.filter { it != Special.COLOUR }

    private fun activates(steps: List<Step>) = steps.filterIsInstance<Step.Activate>()

    @Test
    fun twoColourGemsClearTheWholeBoardForSixHundredFortyPoints() {
        val board = patterned(Pos(4, 4) to colour, Pos(4, 5) to colour)

        val result = resolveSwap(board, Pos(4, 4), Pos(4, 5), Rng(1L))

        val activate = activates(result.steps).single()
        assertEquals(Special.COLOUR, activate.special)
        assertEquals(ALL_CELLS, activate.cells)
        val clear = result.steps.filterIsInstance<Step.Clear>().first()
        assertEquals(ALL_CELLS, clear.cells)
        assertEquals(640, clear.points)
        assertEquals(1, clear.depth)
        assertTrue(isFull(result.board))
        assertEquals(result.board, replay(board, result.steps))
    }

    @Test
    fun aColourGemWithABlastConvertsEveryGemOfTheBlastsColourAndFiresThemAll() {
        val board = patterned(*greens(), Pos(4, 4) to colour, Pos(4, 5) to Piece(Gem.GREEN, Special.BLAST))

        val result = resolveSwap(board, Pos(4, 4), Pos(4, 5), Rng(1L))

        val activates = activates(result.steps)
        assertEquals(listOf(Special.COLOUR) + List(3) { Special.BLAST }, activates.map { it.special })
        assertEquals(listOf(Pos(4, 5), Pos(0, 0), Pos(4, 4), Pos(6, 6)), activates.map { it.origin })
        assertEquals(cells(0 to 0, 4 to 5, 6 to 6, 4 to 4), activates[0].cells)
        assertEquals(effectCells(Pos(0, 0), Special.BLAST), activates[1].cells)
        assertEquals(effectCells(Pos(4, 4), Special.BLAST), activates[2].cells)
        assertEquals(effectCells(Pos(6, 6), Special.BLAST), activates[3].cells)
        val clear = result.steps.filterIsInstance<Step.Clear>().first()
        assertEquals(activates.flatMap { it.cells }.toSet(), clear.cells)
        assertEquals(clear.cells.size * POINTS_PER_EFFECT_GEM, clear.points)
    }

    @Test
    fun convertedLineGemsCopyThePartnersDirection() {
        for (line in listOf(Special.LINE_ROW, Special.LINE_COL)) {
            val board = patterned(*greens(), Pos(4, 4) to colour, Pos(4, 5) to Piece(Gem.GREEN, line))

            val result = activates(resolveSwap(board, Pos(4, 4), Pos(4, 5), Rng(1L)).steps)

            assertEquals(listOf(Special.COLOUR) + List(3) { line }, result.map { it.special })
            result.drop(1).forEach { assertEquals(effectCells(it.origin, line), it.cells) }
        }
    }

    @Test
    fun aColourGemWithEitherXConvertsToThatX() {
        for (x in listOf(Special.X_SHORT, Special.X_FULL)) {
            val board = patterned(*greens(), Pos(4, 4) to colour, Pos(4, 5) to Piece(Gem.GREEN, x))

            val result = activates(resolveSwap(board, Pos(4, 4), Pos(4, 5), Rng(1L)).steps)

            assertEquals(listOf(Special.COLOUR) + List(3) { x }, result.map { it.special }, "$x")
            result.drop(1).forEach { assertEquals(effectCells(it.origin, x), it.cells, "$x") }
        }
    }

    @Test
    fun convertedGemsFireInBoardOrderAfterTheColourGemAndOnlySameKindPairsAreDoubled() {
        val board = patterned(*greens(), Pos(4, 4) to colour, Pos(4, 5) to Piece(Gem.GREEN, Special.BLAST))

        val origins = activates(resolveSwap(board, Pos(4, 4), Pos(4, 5), Rng(1L)).steps).map { it.origin }

        val converted = origins.drop(1)
        assertEquals(converted.sortedWith(BOARD_ORDER), converted)
        assertEquals(setOf(Special.BLAST, Special.LINE_ROW, Special.LINE_COL), DOUBLED)
    }

    @Test
    fun aColourGemWithACrossConvertsToCross() {
        val board = patterned(*greens(), Pos(4, 4) to colour, Pos(4, 5) to Piece(Gem.GREEN, Special.CROSS))

        val result = activates(resolveSwap(board, Pos(4, 4), Pos(4, 5), Rng(1L)).steps)

        assertEquals(listOf(Special.COLOUR) + List(3) { Special.CROSS }, result.map { it.special })
    }

    @Test
    fun convertedPiecesChainIntoOtherSpecialsTheyCover() {
        val board =
            patterned(
                *greens(),
                Pos(1, 1) to Piece(Gem.BLUE, Special.LINE_ROW),
                Pos(4, 4) to colour,
                Pos(4, 5) to Piece(Gem.GREEN, Special.BLAST),
            )

        val result = activates(resolveSwap(board, Pos(4, 4), Pos(4, 5), Rng(1L)).steps)

        assertEquals(Pos(1, 1), result.last().origin)
        assertEquals(Special.LINE_ROW, result.last().special)
        assertEquals(5, result.size)
    }

    @Test
    fun everyOtherPairFiresBothEffectsFromTheirOwnCells() {
        val doubled = setOf(Special.BLAST, Special.LINE_ROW, Special.LINE_COL)
        for (first in effects) {
            for (second in effects) {
                if (first == second && first in doubled) continue
                val board = patterned(Pos(4, 4) to Piece(Gem.WHITE, first), Pos(4, 5) to Piece(Gem.BLUE, second))

                val result = activates(resolveSwap(board, Pos(4, 4), Pos(4, 5), Rng(1L)).steps)

                // The pieces trade places: the first lands on (4, 5) and the second on (4, 4).
                assertEquals(
                    setOf(
                        Triple(Pos(4, 5), first, effectCells(Pos(4, 5), first)),
                        Triple(Pos(4, 4), second, effectCells(Pos(4, 4), second)),
                    ),
                    result.map { Triple(it.origin, it.special, it.cells) }.toSet(),
                    "$first with $second",
                )
                assertEquals(2, result.size, "$first with $second")
            }
        }
    }

    @Test
    fun comboBoardsStayFullAndReplayOnManySeeds() {
        for (special in effects) {
            for (seed in 1L..SEEDS) {
                val board = patterned(*greens(), Pos(4, 4) to colour, Pos(4, 5) to Piece(Gem.GREEN, special))

                val result = resolveSwap(board, Pos(4, 4), Pos(4, 5), Rng(seed))

                assertTrue(isFull(result.board), "$special seed $seed")
                assertEquals(result.board, replay(board, result.steps), "$special seed $seed")
            }
        }
    }

    private fun greens() = arrayOf(Pos(0, 0) to Piece(Gem.GREEN), Pos(6, 6) to Piece(Gem.GREEN))

    private companion object {
        const val SEEDS = 30L
    }

    @Test
    fun twoOfTheSameLineOrBlastMakeOneBiggerEffectCentredOnTheDraggedGem() {
        for (special in listOf(Special.LINE_ROW, Special.LINE_COL, Special.BLAST)) {
            val board = patterned(Pos(3, 3) to Piece(Gem.WHITE, special), Pos(3, 4) to Piece(Gem.BLUE, special))

            val fired = activates(resolveSwap(board, Pos(3, 3), Pos(3, 4), Rng(1L)).steps)

            assertEquals(1, fired.size, "$special")
            assertEquals(Pos(3, 4), fired.single().origin, "$special")
            assertEquals(pairCells(special, Pos(3, 4)), fired.single().cells, "$special")
        }
    }

    @Test
    fun twoColumnLinesAlwaysClearThreeColumnsWhicheverWayTheyAreSwapped() {
        fun columns(
            from: Pos,
            to: Pos,
        ): Set<Int> {
            val board = patterned(from to Piece(Gem.WHITE, Special.LINE_COL), to to Piece(Gem.BLUE, Special.LINE_COL))
            val cleared =
                resolveSwap(board, from, to, Rng(1L))
                    .steps
                    .filterIsInstance<Step.Clear>()
                    .first()
                    .cells
            return cleared.map { it.col }.toSet()
        }

        assertEquals(setOf(3, 4, 5), columns(Pos(3, 3), Pos(3, 4)))
        assertEquals(setOf(2, 3, 4), columns(Pos(3, 4), Pos(3, 3)))
        assertEquals(setOf(2, 3, 4), columns(Pos(3, 3), Pos(4, 3)))
        assertEquals(setOf(2, 3, 4), columns(Pos(4, 3), Pos(3, 3)))
    }
}
