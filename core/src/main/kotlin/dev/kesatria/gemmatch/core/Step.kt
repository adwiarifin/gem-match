package dev.kesatria.gemmatch.core

/** A piece that dropped from [from] to [to] within one column. */
data class GemMove(
    val piece: Piece,
    val from: Pos,
    val to: Pos,
)

/** A new piece entering at [pos]. The renderer starts it above the board, stacked by column. */
data class Placed(
    val piece: Piece,
    val pos: Pos,
)

/** One animatable event produced by resolving a move. Replaying them in order rebuilds the board. */
sealed interface Step {
    data class Swap(
        val a: Pos,
        val b: Pos,
    ) : Step

    /** [special] fired from [origin] and takes [cells]. Every Activate of a move comes before its Clear. */
    data class Activate(
        val origin: Pos,
        val special: Special,
        val cells: Set<Pos>,
    ) : Step

    data class Clear(
        val cells: Set<Pos>,
        val points: Int,
        val depth: Int,
    ) : Step

    /** A special piece made by the match that the preceding [Clear] cleared, placed on one of the cells it emptied. */
    data class Create(
        val pos: Pos,
        val piece: Piece,
    ) : Step

    data class Fall(
        val moves: List<GemMove>,
    ) : Step

    data class Spawn(
        val gems: List<Placed>,
    ) : Step
}
