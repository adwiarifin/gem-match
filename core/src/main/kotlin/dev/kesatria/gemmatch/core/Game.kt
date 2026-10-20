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
    val time: RunTime = RunTime(0L, 0L),
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
        val total = saturatingAdd(score, resolution.score)
        val next = Game(resolution.board, total, nextRng, stats + RunStats.ofMove(resolution), time)
        return SwapOutcome.Accepted(next, resolution.steps)
    }

    /** The same game with [activeMs] as its foreground time so far. */
    fun withActiveMs(activeMs: Long): Game = Game(board, score, rng.copy(), stats, RunTime(time.startedMs, activeMs))

    fun encode(): String = encodeState(SavedState(board, score, rng.state, stats, time))

    companion object {
        /**
         * Restores a saved game, or null if [text] is not a complete, valid save. A save from before `GM3` has no
         * start time, so its run starts at [nowMs].
         */
        fun decode(
            text: String,
            nowMs: Long = 0L,
        ): Game? = decodeState(text, nowMs)?.let { Game(it.board, it.score, Rng(it.rngState), it.stats, it.time) }

        /** A new game; [startedMs] is the epoch time the run begins. */
        fun new(
            seed: Long,
            startedMs: Long = 0L,
        ): Game {
            val rng = Rng(seed)
            return Game(generateBoard(rng), 0, rng, RunStats.EMPTY, RunTime(startedMs, 0L))
        }
    }
}
