package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ScoringTest {
    private fun onlyGroup(vararg rows: String): MatchGroup {
        val groups = findMatchGroups(boardOf(*rows))
        assertEquals(1, groups.size, "fixture must hold exactly one group")
        return groups[0]
    }

    @Test
    fun threeInARowScoresThirty() {
        assertEquals(30, scoreFor(onlyGroup("RRRGB")))
    }

    @Test
    fun fourInARowScoresSixty() {
        assertEquals(60, scoreFor(onlyGroup("RRRRGB")))
    }

    @Test
    fun fiveInARowScoresOneHundred() {
        assertEquals(100, scoreFor(onlyGroup("RRRRRGB")))
    }

    @Test
    fun longerThanFiveStillScoresOneHundred() {
        assertEquals(100, scoreFor(onlyGroup("RRRRRRRR")))
    }

    @Test
    fun lShapeScoresBothRunsPlusShapeBonus() {
        val group =
            onlyGroup(
                "RRR",
                "RBG",
                "RGB",
            )

        assertEquals(30 + 30 + 50, scoreFor(group))
    }

    @Test
    fun plusShapeScoresBothRunsPlusShapeBonus() {
        val group =
            onlyGroup(
                "BRG",
                "RRR",
                "GRB",
            )

        assertEquals(30 + 30 + 50, scoreFor(group))
    }

    @Test
    fun shapeBonusIsAddedOncePerGroup() {
        val group =
            onlyGroup(
                "RRRR",
                "BRGB",
                "GRBG",
            )

        assertEquals(60 + 30 + 50, scoreFor(group))
    }

    @Test
    fun separateGroupsAddUp() {
        val groups =
            findMatchGroups(
                boardOf(
                    "RRR",
                    "BGB",
                    "YYYY",
                ),
            )

        assertEquals(30 + 60, scoreGroups(groups, depth = 1))
    }

    @Test
    fun depthMultipliesTheScore() {
        val groups = findMatchGroups(boardOf("RRRGB"))

        assertEquals(30, scoreGroups(groups, depth = 1))
        assertEquals(60, scoreGroups(groups, depth = 2))
        assertEquals(90, scoreGroups(groups, depth = 3))
    }

    @Test
    fun addingScoresStopsAtTheLargestScoreInsteadOfWrappingNegative() {
        assertEquals(130, saturatingAdd(100, 30))
        assertEquals(Int.MAX_VALUE, saturatingAdd(Int.MAX_VALUE - 10, 30))
        assertEquals(Int.MAX_VALUE, saturatingAdd(Int.MAX_VALUE, Int.MAX_VALUE))
    }
}
