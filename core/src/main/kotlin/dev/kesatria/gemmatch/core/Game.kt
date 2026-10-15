package dev.kesatria.gemmatch.core

sealed interface SwapOutcome {
    data object Rejected : SwapOutcome

    data class Accepted(
        val game: Game,
        val steps: List<Step>,
    ) : SwapOutcome
}

/** Immutable game state. Every move returns a new [Game]; the receiver is never changed. */
class Game private constructor(
    val board: Board,
    val score: Int,
    private val rng: Rng,
) {
    /** Where the random sequence stands, so a saved game continues with the same future gems. */
    val rngState: Long get() = rng.state

    fun trySwap(
        a: Pos,
        b: Pos,
    ): SwapOutcome {
        if (!isValidSwap(board, a, b)) return SwapOutcome.Rejected

        val nextRng = rng.copy()
        val resolution = resolveSwap(board, a, b, nextRng)
        val (settled, reshuffle) = ensureMoveExists(resolution.board, nextRng)
        val steps = if (reshuffle != null) resolution.steps + reshuffle else resolution.steps
        return SwapOutcome.Accepted(Game(settled, score + resolution.score, nextRng), steps)
    }

    companion object {
        fun new(seed: Long): Game {
            val rng = Rng(seed)
            return Game(generateBoard(rng), 0, rng)
        }
    }
}
