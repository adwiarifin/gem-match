package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatchesTest {
    @Test
    fun emptyBoardHasNoRuns() {
        assertTrue(findRuns(Board.empty()).isEmpty())
    }

    @Test
    fun threeInARowIsAHorizontalRun() {
        val runs = findRuns(boardOf("RRRGBYO"))

        assertEquals(1, runs.size)
        assertEquals(Gem.RED, runs[0].gem)
        assertTrue(runs[0].isHorizontal)
        assertEquals(cells(0 to 0, 0 to 1, 0 to 2), runs[0].cells.toSet())
    }

    @Test
    fun threeInAColumnIsAVerticalRun() {
        val runs = findRuns(boardOf("GB", "GO", "GY"))

        assertEquals(1, runs.size)
        assertEquals(Gem.GREEN, runs[0].gem)
        assertFalse(runs[0].isHorizontal)
        assertEquals(cells(0 to 0, 1 to 0, 2 to 0), runs[0].cells.toSet())
    }

    @Test
    fun fourAndFiveInARowAreSingleRuns() {
        val four = findRuns(boardOf("RRRRGBY"))
        val five = findRuns(boardOf("RRRRRGB"))

        assertEquals(listOf(4), four.map { it.length })
        assertEquals(listOf(5), five.map { it.length })
    }

    @Test
    fun sixInARowIsOneRunNotTwo() {
        val runs = findRuns(boardOf("RRRRRRGB"))

        assertEquals(listOf(6), runs.map { it.length })
    }

    @Test
    fun twoInARowIsNotARun() {
        assertTrue(findRuns(boardOf("RRGGBBYY")).isEmpty())
    }

    @Test
    fun differentGemBreaksARun() {
        assertTrue(findRuns(boardOf("RRGRR")).isEmpty())
    }

    @Test
    fun emptyCellBreaksARun() {
        assertTrue(findRuns(boardOf("RR.RR")).isEmpty())
    }

    @Test
    fun runsTouchingTheBoardEdgesAreFound() {
        val board =
            boardOf(
                "BBBBBBBB",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                ".......R",
            )

        val runs = findRuns(board)

        assertEquals(listOf(BOARD_SIZE), runs.map { it.length })
    }

    @Test
    fun twoRunsOfTheSameGemInOneRowStaySeparate() {
        val runs = findRuns(boardOf("RRRBRRR"))

        val expected = setOf(cells(0 to 0, 0 to 1, 0 to 2), cells(0 to 4, 0 to 5, 0 to 6))

        assertEquals(2, runs.size)
        assertEquals(expected, runs.map { it.cells.toSet() }.toSet())
    }

    @Test
    fun lShapeMergesIntoOneGroupOfFiveCells() {
        val groups =
            findMatchGroups(
                boardOf(
                    "RRR",
                    "RBG",
                    "RGB",
                ),
            )

        assertEquals(1, groups.size)
        assertEquals(2, groups[0].runs.size)
        assertTrue(groups[0].isCompound)
        assertEquals(cells(0 to 0, 0 to 1, 0 to 2, 1 to 0, 2 to 0), groups[0].cells)
    }

    @Test
    fun tShapeMergesIntoOneGroup() {
        val groups =
            findMatchGroups(
                boardOf(
                    "RRR",
                    "BRG",
                    "GRB",
                ),
            )

        assertEquals(1, groups.size)
        assertEquals(cells(0 to 0, 0 to 1, 0 to 2, 1 to 1, 2 to 1), groups[0].cells)
    }

    @Test
    fun plusShapeMergesIntoOneGroup() {
        val groups =
            findMatchGroups(
                boardOf(
                    "BRG",
                    "RRR",
                    "GRB",
                ),
            )

        assertEquals(1, groups.size)
        assertEquals(cells(0 to 1, 1 to 0, 1 to 1, 1 to 2, 2 to 1), groups[0].cells)
    }

    @Test
    fun aSingleRunIsNotCompound() {
        val groups = findMatchGroups(boardOf("RRRGB"))

        assertEquals(1, groups.size)
        assertFalse(groups[0].isCompound)
    }

    @Test
    fun runsOfDifferentGemsFormSeparateGroups() {
        val groups =
            findMatchGroups(
                boardOf(
                    "RRR",
                    "BGB",
                    "YYY",
                ),
            )

        assertEquals(2, groups.size)
    }

    @Test
    fun parallelRunsOfTheSameGemThatShareNoCellStaySeparateGroups() {
        val groups =
            findMatchGroups(
                boardOf(
                    "RRR",
                    "RRR",
                ),
            )

        assertEquals(2, groups.size)
    }

    @Test
    fun aSpecialPieceJoinsARunOfItsColour() {
        val board = boardOf("RRRR").with(Pos(0, 1), Piece(Gem.RED, Special.BLAST))

        assertEquals(listOf(4), findRuns(board).map { it.length })
    }

    @Test
    fun aSpecialInTheMiddleKeepsARunWhole() {
        val board = boardOf("RRRRR").with(Pos(0, 2), Piece(Gem.RED, Special.CROSS))

        assertEquals(listOf(5), findRuns(board).map { it.length })
    }

    @Test
    fun aColourGemNeverJoinsARun() {
        val board = boardOf("RR.R").with(Pos(0, 2), Piece(null, Special.COLOUR))

        assertEquals(emptyList(), findRuns(board))
    }

    @Test
    fun runsOnEitherSideOfASpecialStillCount() {
        val board = boardOf("RRRBRRR").with(Pos(0, 3), Piece(Gem.BLUE, Special.LINE_ROW))

        assertEquals(2, findRuns(board).size)
    }

    @Test
    fun withHangingGemsAtBothEndsTheFirstInBoardOrderIsTaken() {
        val above = findMatchGroups(boardOf(".R...", ".RRR.", "...R.")).single()
        val mirrored = findMatchGroups(boardOf("...R.", ".RRR.", ".R...")).single()

        assertEquals(Pos(0, 1), above.hanging)
        assertEquals(Pos(0, 3), mirrored.hanging)
    }
}
