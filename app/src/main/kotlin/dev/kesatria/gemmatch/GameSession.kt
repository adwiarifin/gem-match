package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.Board
import dev.kesatria.gemmatch.core.Game
import dev.kesatria.gemmatch.core.Move
import dev.kesatria.gemmatch.core.Placed
import dev.kesatria.gemmatch.core.Pos
import dev.kesatria.gemmatch.core.SwapOutcome

/**
 * The game being played, its score and what has been written to disk. A move resolves instantly in the
 * core, so [game] can run ahead of what is on screen: [pending] is the resolved game that the playing
 * animation is heading for, and [shownScore] counts up as that animation reaches each clear.
 */
class GameSession(
    private var game: Game,
    private val store: GameStore,
    initialBest: Int,
    private val newSeed: () -> Long,
) {
    private var pending: Game? = null
    private var savedBest = maxOf(initialBest, game.score)

    /** The newest resolved game, including a move whose animation is still playing. What gets saved. */
    @Volatile private var latest = game

    var shownScore = game.score
        private set

    @Volatile
    var bestScore = savedBest
        private set

    val board: Board get() = game.board

    /** Tries [move]. Returns the animation to play: the steps of an accepted move, or a swap and back. */
    fun play(move: Move): Timeline =
        when (val outcome = game.trySwap(move.from, move.to)) {
            SwapOutcome.Rejected ->
                Timeline(listOf(SwapAnimation(move.from, move.to), SwapAnimation(move.from, move.to)))

            is SwapOutcome.Accepted -> {
                val timeline = Timeline.forSteps(outcome.steps, ::addPoints)
                pending = outcome.game
                latest = outcome.game
                store.saveGame(outcome.game)
                timeline
            }
        }

    /** The old gems shrink away, then the new board rains in from the top. Score resets at once; best stays. */
    fun newGame(scene: Scene): Timeline {
        scene.load((pending ?: game).board)
        game = Game.new(newSeed())
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
        }
        pending = null
        saveBestIfImproved()
    }

    /** Writes the current game and best score to disk and waits. Called from the UI thread when the app is paused. */
    fun save() = store.flush(latest, bestScore)

    private fun addPoints(points: Int) {
        shownScore += points
        bestScore = maxOf(bestScore, shownScore)
    }

    private fun saveBestIfImproved() {
        if (bestScore > savedBest) {
            savedBest = bestScore
            store.saveBest(bestScore)
        }
    }
}
