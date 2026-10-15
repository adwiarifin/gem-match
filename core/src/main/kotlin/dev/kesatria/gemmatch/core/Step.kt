package dev.kesatria.gemmatch.core

/** A gem that dropped from [from] to [to] within one column. */
data class GemMove(
    val gem: Gem,
    val from: Pos,
    val to: Pos,
)

/** A new gem entering at [pos]. The renderer starts it above the board, stacked by column. */
data class Placed(
    val gem: Gem,
    val pos: Pos,
)

/** One animatable event produced by resolving a move. Replaying them in order rebuilds the board. */
sealed interface Step {
    data class Swap(
        val a: Pos,
        val b: Pos,
    ) : Step

    data class Clear(
        val cells: Set<Pos>,
        val points: Int,
        val depth: Int,
    ) : Step

    data class Fall(
        val moves: List<GemMove>,
    ) : Step

    data class Spawn(
        val gems: List<Placed>,
    ) : Step
}
