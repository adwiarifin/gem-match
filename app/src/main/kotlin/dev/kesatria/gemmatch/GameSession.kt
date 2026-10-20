package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.Board
import dev.kesatria.gemmatch.core.FinishedRun
import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.Move
import dev.kesatria.gemmatch.core.Placed
import dev.kesatria.gemmatch.core.Pos
import dev.kesatria.gemmatch.core.RunTime
import dev.kesatria.gemmatch.core.SwapOutcome
import dev.kesatria.gemmatch.core.finish
import dev.kesatria.gemmatch.core.saturatingAdd

/**
 * The game being played, its score and what has been written to disk. A move resolves instantly in the
 * core, so [game] can run ahead of what is on screen: [pending] is the resolved game that the playing
 * animation is heading for, and [shownScore] counts up as that animation reaches each clear.
 */
class GameSession(
    private var game: Game,
    private val store: GameStore,
    initialBest: Int,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val history: RunHistory = FileRunHistory(store),
    private val newSeed: () -> Long,
) {
    private val clock = RunClock(game.time.activeMs)

    private var pending: Game? = null
    private var savedBest = maxOf(initialBest, game.score)

    /** The newest resolved game, including a move whose animation is still playing. What gets saved. */
    @Volatile private var latest = game

    /** The run that just ended, once its board is dead and it has been recorded. Null while a run is in progress. */
    @Volatile
    var finishedRun: FinishedRun? = null
        private set

    var shownScore = game.score
        private set

    @Volatile
    var bestScore = savedBest
        private set

    val board: Board get() = game.board

    /** When this run started and how long it has been played in the foreground. */
    val time: RunTime get() = RunTime(latest.time.startedMs, clock.activeMs)

    /** One game-loop step with the app in front. The only thing that adds active time. */
    fun tick(seconds: Float) = clock.advance(seconds)

    /** True once the board has no valid move and the move that left it so has finished animating. */
    val isOver: Boolean get() = pending == null && game.isOver

    /** True while an accepted move or a new game animates, so a tap may skip it. Not for a rejected swap. */
    val isMovePlaying: Boolean get() = pending != null

    /** Tries [move]. Returns the animation to play: the steps of an accepted move, or a swap and back. */
    fun play(move: Move): Timeline =
        when (val outcome = game.trySwap(move.from, move.to)) {
            SwapOutcome.Rejected ->
                Timeline(listOf(SwapAnimation(move.from, move.to), SwapAnimation(move.from, move.to)))

            is SwapOutcome.Accepted -> {
                val timeline = Timeline.forSteps(outcome.steps, ::addPoints)
                pending = outcome.game
                latest = outcome.game
                store.saveGame(outcome.game.withActiveMs(clock.activeMs))
                timeline
            }
        }

    /** The old gems shrink away, then the new board rains in from the top. Score resets at once; best stays. */
    fun newGame(scene: Scene): Timeline {
        // A move still animating was already scored by the core; its points count even though the player walked away.
        val abandoned = pending ?: game
        bestScore = maxOf(bestScore, abandoned.score)
        scene.load(abandoned.board)
        game = Game.new(newSeed(), startedMs = nowMs())
        finishedRun = null
        clock.reset()
        pending = game
        latest = game
        store.saveGame(game)
        saveBestIfImproved()
        shownScore = 0
        val allCells = (0 until BOARD_SIZE).flatMap { row -> (0 until BOARD_SIZE).map { col -> Pos(row, col) } }
        val arrivals =
            allCells
                .sortedWith(compareBy({ it.col }, { it.row }))
                .mapNotNull { pos -> game.board[pos]?.let { Placed(it, pos) } }
        return Timeline(listOf(ClearAnimation(allCells.toSet(), 0) {}, SpawnAnimation(arrivals)))
    }

    /** The animation has ended: the scene snaps to the resolved board and the score is final. */
    fun finish(scene: Scene) {
        pending?.let {
            game = it
            scene.load(it.board)
            shownScore = it.score
            bestScore = maxOf(bestScore, shownScore)
            if (game.isOver) endRun()
        }
        pending = null
        saveBestIfImproved()
    }

    /** Writes the current game and best score to disk and waits. Called from the UI thread when the app is paused. */
    fun save() = store.flush({ latest.withActiveMs(clock.activeMs) }, { bestScore })

    /**
     * Records the dead game as a finished run, once, then makes a fresh game the one that gets saved. The dead board
     * stays in [game] for the card until the player starts the next game.
     */
    private fun endRun() {
        val (run, fresh) = closeRun(game.withActiveMs(clock.activeMs), store, history, nowMs(), newSeed())
        finishedRun = run
        clock.reset()
        latest = fresh
    }

    private fun addPoints(points: Int) {
        shownScore = saturatingAdd(shownScore, points)
        bestScore = maxOf(bestScore, shownScore)
    }

    private fun saveBestIfImproved() {
        if (bestScore > savedBest) {
            savedBest = bestScore
            store.saveBest(bestScore)
        }
    }
}

/**
 * Records the dead [game] as a finished run, once, and saves a fresh game in its place so that a relaunch starts a
 * new game. Returns the run and the fresh game.
 */
internal fun closeRun(
    game: Game,
    store: GameStore,
    history: RunHistory,
    nowMs: Long,
    seed: Long,
): Pair<FinishedRun, Game> {
    val run = game.finish(nowMs, history.lifetime().bestScore)
    history.record(run)
    val fresh = Game.new(seed, startedMs = nowMs)
    store.saveGame(fresh)
    return run to fresh
}

/**
 * The game to start with: the saved one, or a new one if there is none. A kill after the last move can leave a dead
 * board on disk; that run is recorded (once) and a new game starts.
 */
fun startupGame(
    store: GameStore,
    history: RunHistory,
    nowMs: Long,
    seed: Long,
): Game {
    val saved = store.loadGame(nowMs)
    return when {
        saved == null -> Game.new(seed, startedMs = nowMs)
        saved.isOver -> closeRun(saved, store, history, nowMs, seed).second
        else -> saved
    }
}
