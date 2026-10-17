package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CreationTest {
    private fun classifyShape(vararg rows: String): Special? {
        val groups = findMatchGroups(boardOf(*rows))
        assertEquals(1, groups.size, "the shape must form exactly one group")
        return classify(groups.single())
    }

    @Test
    fun aRunOfThreeMakesNothing() {
        assertNull(classifyShape("RRR"))
        assertNull(classifyShape("R", "R", "R"))
    }

    @Test
    fun aRunOfFourMakesABlast() {
        assertEquals(Special.BLAST, classifyShape("RRRR"))
        assertEquals(Special.BLAST, classifyShape("R", "R", "R", "R"))
    }

    @Test
    fun aRunOfFiveOrMoreMakesAColourGem() {
        assertEquals(Special.COLOUR, classifyShape("RRRRR"))
        assertEquals(Special.COLOUR, classifyShape("R", "R", "R", "R", "R"))
        assertEquals(Special.COLOUR, classifyShape("RRRRRR"))
        assertEquals(Special.COLOUR, classifyShape("RRRRRRRR"))
    }

    @Test
    fun anLWithEqualArmsClearsTheRow() {
        assertEquals(Special.LINE_ROW, classifyShape("RRR", "R..", "R.."))
    }

    @Test
    fun anLWithALongerHorizontalArmClearsTheRow() {
        assertEquals(Special.LINE_ROW, classifyShape("RRRR", "R...", "R..."))
    }

    @Test
    fun anLWithALongerVerticalArmClearsTheColumn() {
        assertEquals(Special.LINE_COL, classifyShape("RRR", "R..", "R..", "R.."))
    }

    @Test
    fun theCornerOfAnLCanBeAnyOfTheFour() {
        assertEquals(Special.LINE_ROW, classifyShape("RRR", "..R", "..R"))
        assertEquals(Special.LINE_ROW, classifyShape("R..", "R..", "RRR"))
        assertEquals(Special.LINE_ROW, classifyShape("..R", "..R", "RRR"))
        assertEquals(Special.LINE_COL, classifyShape("..R", "..R", "..R", "RRR"))
        assertEquals(Special.LINE_COL, classifyShape("RRR", "..R", "..R", "..R"))
    }

    @Test
    fun aTMakesACrossGem() {
        assertEquals(Special.CROSS, classifyShape("RRR", ".R.", ".R."))
        assertEquals(Special.CROSS, classifyShape(".R.", ".R.", "RRR"))
        assertEquals(Special.CROSS, classifyShape("R..", "RRR", "R.."))
        assertEquals(Special.CROSS, classifyShape("..R", "RRR", "..R"))
    }

    @Test
    fun aPlusMakesACrossGem() {
        assertEquals(Special.CROSS, classifyShape(".R.", "RRR", ".R."))
    }

    @Test
    fun aFourRunInATBeatsTheBlast() {
        assertEquals(Special.CROSS, classifyShape("RRRR", ".R..", ".R.."))
    }

    @Test
    fun aFourRunInAnLBeatsTheBlast() {
        assertEquals(Special.LINE_ROW, classifyShape("RRRR", "R...", "R..."))
    }

    @Test
    fun aRunOfFiveBeatsEveryShape() {
        assertEquals(Special.COLOUR, classifyShape("RRRRR", "..R..", "..R.."))
        assertEquals(Special.COLOUR, classifyShape("RRRRR", "R....", "R...."))
    }

    @Test
    fun threeRunsAllMeetingAtTheirEndsAreAnL() {
        assertEquals(Special.LINE_ROW, classifyShape("RRR", "R..", "RRR"))
    }

    @Test
    fun threeRunsWithOneCrossingInsideARunAreAT() {
        assertEquals(Special.CROSS, classifyShape("RRR", ".RR", ".RR"))
    }

    private fun anchoredAt(vararg rows: String): List<Step.Create> {
        val groups = findMatchGroups(boardOf(*rows))
        return createdPieces(groups, emptyList())
    }

    @Test
    fun withNoSwappedCellARunMakesItsSpecialAtTheMiddle() {
        assertEquals(listOf(Step.Create(Pos(0, 2), Piece(Gem.RED, Special.BLAST))), anchoredAt("RRRR"))
        assertEquals(listOf(Step.Create(Pos(2, 0), Piece(Gem.RED, Special.BLAST))), anchoredAt("R", "R", "R", "R"))
        assertEquals(listOf(Step.Create(Pos(0, 2), Piece(null, Special.COLOUR))), anchoredAt("RRRRR"))
    }

    @Test
    fun withNoSwappedCellAShapeMakesItsSpecialAtTheCrossing() {
        assertEquals(listOf(Step.Create(Pos(0, 0), Piece(Gem.RED, Special.LINE_ROW))), anchoredAt("RRR", "R..", "R.."))
        assertEquals(listOf(Step.Create(Pos(0, 1), Piece(Gem.RED, Special.CROSS))), anchoredAt("RRR", ".R.", ".R."))
    }

    @Test
    fun aSwappedCellInTheGroupBeatsTheMiddle() {
        val groups = findMatchGroups(boardOf("RRRR"))

        val made = createdPieces(groups, listOf(Pos(1, 3), Pos(0, 3)))

        assertEquals(listOf(Step.Create(Pos(0, 3), Piece(Gem.RED, Special.BLAST))), made)
    }
}
