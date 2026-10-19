package dev.kesatria.gemmatch.core

private const val PLAYER_SALT = 0x2545F4914F6CDD1DL

/** Who picks the moves when measuring how long a run lasts. */
enum class RunPlayer {
    /** Any legal move, special swaps included. */
    RANDOM,

    /** A move that makes a match whenever one exists, otherwise any legal move. */
    GREEDY,
    ;

    fun choose(
        board: Board,
        rng: Rng,
    ): Move {
        val moves =
            when (this) {
                RANDOM -> findValidMoves(board)
                GREEDY -> findMatchMoves(board).ifEmpty { findValidMoves(board) }
            }
        return moves[rng.nextInt(moves.size)]
    }
}

/** [moves] accepted swaps were played; [ended] is true if the last one left a board with no valid move. */
data class PlayedRun(
    val moves: Int,
    val ended: Boolean,
)

/** Plays [seed] with [player] until the first dead board (the first reshuffle) or [cap] moves. */
fun playUntilDead(
    seed: Long,
    player: RunPlayer,
    cap: Int,
): PlayedRun {
    var game = Game.new(seed)
    val chooser = Rng(seed * PLAYER_SALT)
    for (move in 1..cap) {
        val pick = player.choose(game.board, chooser)
        val outcome = game.trySwap(pick.from, pick.to) as SwapOutcome.Accepted
        if (outcome.steps.any { it is Step.Reshuffle }) return PlayedRun(move, ended = true)
        game = outcome.game
    }
    return PlayedRun(maxOf(cap, 0), ended = false)
}

/** The smallest value that at least [percent] percent of [lengths] are at or below, or null for no values. */
fun percentile(
    lengths: List<Int>,
    percent: Int,
): Int? {
    if (lengths.isEmpty()) return null
    val rank = (percent * lengths.size + PERCENT - 1) / PERCENT
    return lengths.sorted()[(rank - 1).coerceIn(0, lengths.size - 1)]
}

/** A run that hit the cap counts as [cap] moves, so the percentiles are lower bounds when [capped] is above 0. */
data class RunReport(
    val games: Int,
    val capped: Int,
    val cap: Int,
    val p10: Int?,
    val median: Int?,
    val p90: Int?,
) {
    fun describe(): String = "games=$games capped=$capped (cap $cap moves) p10=$p10 median=$median p90=$p90"
}

fun summarize(
    runs: List<PlayedRun>,
    cap: Int,
): RunReport {
    val lengths = runs.map { it.moves }
    return RunReport(
        games = runs.size,
        capped = runs.count { !it.ended },
        cap = cap,
        p10 = percentile(lengths, P10),
        median = percentile(lengths, P50),
        p90 = percentile(lengths, P90),
    )
}

private const val PERCENT = 100
private const val P10 = 10
private const val P50 = 50
private const val P90 = 90
