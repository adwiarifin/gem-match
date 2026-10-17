package dev.kesatria.gemmatch.core

const val POINTS_RUN_OF_3 = 30
const val POINTS_RUN_OF_4 = 60
const val POINTS_RUN_OF_5_OR_MORE = 100
const val POINTS_SHAPE_BONUS = 50
const val POINTS_PER_EFFECT_GEM = 10

private const val LENGTH_4 = 4
private const val LENGTH_5 = 5

private fun scoreFor(run: Run): Int =
    when {
        run.length >= LENGTH_5 -> POINTS_RUN_OF_5_OR_MORE
        run.length == LENGTH_4 -> POINTS_RUN_OF_4
        else -> POINTS_RUN_OF_3
    }

/** Each run by length, plus one flat bonus when the group is an L, T or + shape. */
fun scoreFor(group: MatchGroup): Int {
    if (group.hanging != null) return POINTS_RUN_OF_4
    val shapeBonus = if (group.isCompound) POINTS_SHAPE_BONUS else 0
    return group.runs.sumOf { scoreFor(it) } + shapeBonus
}

/** Adds two scores, stopping at [Int.MAX_VALUE] so a score can never wrap around to a negative number. */
fun saturatingAdd(
    a: Int,
    b: Int,
): Int = (a.toLong() + b).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

/** All groups cleared together at one cascade [depth] (1 = the player's own match). */
fun scoreGroups(
    groups: List<MatchGroup>,
    depth: Int,
): Int = groups.sumOf { scoreFor(it) } * depth

/** Gems cleared by an effect and by no run, [depth] times over. Every fired special counts as one of them. */
fun scoreEffectGems(
    count: Int,
    depth: Int,
): Int = count * POINTS_PER_EFFECT_GEM * depth
