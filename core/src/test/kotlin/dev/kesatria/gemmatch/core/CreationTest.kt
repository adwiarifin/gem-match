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
}
