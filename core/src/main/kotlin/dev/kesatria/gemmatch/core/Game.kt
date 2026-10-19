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
    val stats: RunStats = RunStats.EMPTY,
) {
    /** Where the random sequence stands, so a saved game continues with the same future gems. */
    val rngState: Long get() = rng.state

    /** True when no swap is valid, so the run is over. Derived from the board, never stored. */
    val isOver: Boolean get() = !hasValidMove(board)

    fun trySwap(
        a: Pos,
        b: Pos,
    ): SwapOutcome {
        if (!isValidSwap(board, a, b)) return SwapOutcome.Rejected

        val nextRng = rng.copy()
        val resolution = resolveSwap(board, a, b, nextRng)
        val next = Game(resolution.board, saturatingAdd(score, resolution.score), nextRng, stats + RunStats.ofMove(resolution))
        return SwapOutcome.Accepted(next, resolution.steps)
    }

    fun encode(): String = encodeState(SavedState(board, score, rng.state))

    companion object {
        /** Restores a saved game, or null if [text] is not a complete, valid save. */
        fun decode(text: String): Game? = decodeState(text)?.let { Game(it.board, it.score, Rng(it.rngState)) }

        fun new(seed: Long): Game {
            val rng = Rng(seed)
            return Game(generateBoard(rng), 0, rng)
        }
    }
}
