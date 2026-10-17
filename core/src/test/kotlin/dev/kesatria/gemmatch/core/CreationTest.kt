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
    fun anLWithEqualArmsMakesACrossGem() {
        assertEquals(Special.CROSS, classifyShape("RRR", "R..", "R.."))
    }

    @Test
    fun anLWithALongerHorizontalArmMakesACrossGem() {
        assertEquals(Special.CROSS, classifyShape("RRRR", "R...", "R..."))
    }

    @Test
    fun anLWithALongerVerticalArmMakesACrossGem() {
        assertEquals(Special.CROSS, classifyShape("RRR", "R..", "R..", "R.."))
    }

    @Test
    fun theCornerOfAnLCanBeAnyOfTheFour() {
        assertEquals(Special.CROSS, classifyShape("RRR", "..R", "..R"))
        assertEquals(Special.CROSS, classifyShape("R..", "R..", "RRR"))
        assertEquals(Special.CROSS, classifyShape("..R", "..R", "RRR"))
        assertEquals(Special.CROSS, classifyShape("..R", "..R", "..R", "RRR"))
        assertEquals(Special.CROSS, classifyShape("RRR", "..R", "..R", "..R"))
    }

    @Test
    fun aLongTMakesAFullXGem() {
        assertEquals(Special.X_FULL, classifyShape("RRR", ".R.", ".R."))
        assertEquals(Special.X_FULL, classifyShape(".R.", ".R.", "RRR"))
        assertEquals(Special.X_FULL, classifyShape("R..", "RRR", "R.."))
        assertEquals(Special.X_FULL, classifyShape("..R", "RRR", "..R"))
    }

    @Test
    fun aPlusMakesAFullXGem() {
        assertEquals(Special.X_FULL, classifyShape(".R.", "RRR", ".R."))
    }

    @Test
    fun aFourRunInATBeatsTheBlast() {
        assertEquals(Special.X_FULL, classifyShape("RRRR", ".R..", ".R.."))
    }

    @Test
    fun aFourRunInAnLBeatsTheBlast() {
        assertEquals(Special.CROSS, classifyShape("RRRR", "R...", "R..."))
    }

    @Test
    fun aRunOfFiveBeatsEveryShape() {
        assertEquals(Special.COLOUR, classifyShape("RRRRR", "..R..", "..R.."))
        assertEquals(Special.COLOUR, classifyShape("RRRRR", "R....", "R...."))
    }

    @Test
    fun threeRunsAllMeetingAtTheirEndsAreAnL() {
        assertEquals(Special.CROSS, classifyShape("RRR", "R..", "RRR"))
    }

    @Test
    fun threeRunsWithOneCrossingInsideARunAreAT() {
        assertEquals(Special.X_FULL, classifyShape("RRR", ".RR", ".RR"))
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
        assertEquals(listOf(Step.Create(Pos(0, 0), Piece(Gem.RED, Special.CROSS))), anchoredAt("RRR", "R..", "R.."))
        assertEquals(listOf(Step.Create(Pos(0, 1), Piece(Gem.RED, Special.X_FULL))), anchoredAt("RRR", ".R.", ".R."))
    }

    @Test
    fun aSwappedCellInTheGroupBeatsTheMiddle() {
        val groups = findMatchGroups(boardOf("RRRR"))

        val made = createdPieces(groups, listOf(Pos(1, 3), Pos(0, 3)))

        assertEquals(listOf(Step.Create(Pos(0, 3), Piece(Gem.RED, Special.BLAST))), made)
    }

    @Test
    fun aGemHangingOffAnEndOfARunOfThreeMakesALineGemAlongTheRun() {
        assertEquals(Special.LINE_ROW, classifyShape("RRR", "R.."))
        assertEquals(Special.LINE_ROW, classifyShape("RRR", "..R"))
        assertEquals(Special.LINE_ROW, classifyShape("R..", "RRR"))
        assertEquals(Special.LINE_COL, classifyShape("RR", "R.", "R."))
        assertEquals(Special.LINE_COL, classifyShape("RR", ".R", ".R"))
        assertEquals(Special.LINE_COL, classifyShape("R.", "R.", "RR"))
    }

    @Test
    fun aGemHangingOffTheMiddleOfARunOfThreeMakesAShortXGem() {
        assertEquals(Special.X_SHORT, classifyShape("RRR", ".R."))
        assertEquals(Special.X_SHORT, classifyShape(".R.", "RRR"))
        assertEquals(Special.X_SHORT, classifyShape("R.", "RR", "R."))
        assertEquals(Special.X_SHORT, classifyShape(".R", "RR", ".R"))
    }

    @Test
    fun aShortLOrShortTClearsFourGemsAndPlacesTheGemAtTheJoint() {
        val short = findMatchGroups(boardOf("RRR", "R..")).single()
        val middle = findMatchGroups(boardOf("RRR", ".R.")).single()

        assertEquals(4, short.cells.size)
        assertEquals(4, middle.cells.size)
        val lineGem = createdPieces(listOf(short), emptyList())
        val shortX = createdPieces(listOf(middle), emptyList())
        assertEquals(listOf(Step.Create(Pos(0, 0), Piece(Gem.RED, Special.LINE_ROW))), lineGem)
        assertEquals(listOf(Step.Create(Pos(0, 1), Piece(Gem.RED, Special.X_SHORT))), shortX)
    }

    @Test
    fun aRunOfFourOrMoreTakesNoHangingGem() {
        val group = findMatchGroups(boardOf("RRRR", "R...")).single()

        assertEquals(4, group.cells.size)
        assertEquals(Special.BLAST, classify(group))
    }

    @Test
    fun aHangingGemThatIsPartOfARunIsNotHanging() {
        val groups = findMatchGroups(boardOf("RRR", "R..", "R.."))

        assertEquals(1, groups.size)
        assertEquals(5, groups.single().cells.size)
        assertEquals(Special.CROSS, classify(groups.single()))
    }

    @Test
    fun withTwoGemsHangingTheFirstInBoardOrderIsTaken() {
        val group = findMatchGroups(boardOf("RRR", "R.R")).single()

        assertEquals(4, group.cells.size)
        assertEquals(Pos(1, 0), group.hanging)
    }

    @Test
    fun aHangingGemBelongsToOnlyOneRun() {
        val groups = findMatchGroups(boardOf("RRR.", "..RR", "...R", "...R"))

        assertEquals(2, groups.size)
        assertEquals(1, groups.count { it.hanging == Pos(1, 2) })
    }
}
