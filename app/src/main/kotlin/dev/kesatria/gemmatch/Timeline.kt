package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.Board
import dev.kesatria.gemmatch.core.GemMove
import dev.kesatria.gemmatch.core.Placed
import dev.kesatria.gemmatch.core.Pos
import dev.kesatria.gemmatch.core.Step
import kotlin.math.PI
import kotlin.math.sin

/** A timed change to the [Scene]. [apply] is called with progress 0..1, then [finish] once. */
interface Animation {
    val seconds: Float

    /** Called once, just before the first [apply]. */
    fun start(scene: Scene) = Unit

    fun apply(
        scene: Scene,
        progress: Float,
    )

    fun finish(scene: Scene)
}

/** Where a sprite was when an animation began, so it can carry on from a half-dragged position. */
private class Frame(
    val row: Float,
    val col: Float,
    val scale: Float,
) {
    companion object {
        fun of(sprite: Sprite?): Frame? = sprite?.let { Frame(it.row, it.col, it.scale) }
    }
}

/** Moves [sprite] from where it started to ([row], [col]) at normal size, [t] being 0..1. */
private fun glide(
    sprite: Sprite?,
    start: Frame?,
    row: Float,
    col: Float,
    t: Float,
) {
    if (sprite == null || start == null) return
    sprite.row = start.row + (row - start.row) * t
    sprite.col = start.col + (col - start.col) * t
    sprite.scale = start.scale + (1f - start.scale) * t
}

/**
 * Slides the sprites at [a] and [b] into each other's cell, then swaps their slots. Starts from
 * wherever the sprites are now, so a swap dragged half way continues without a jump.
 */
class SwapAnimation(
    private val a: Pos,
    private val b: Pos,
) : Animation {
    override val seconds = SWAP_SECONDS
    private var startA: Frame? = null
    private var startB: Frame? = null

    override fun start(scene: Scene) {
        startA = Frame.of(scene[a])
        startB = Frame.of(scene[b])
    }

    override fun apply(
        scene: Scene,
        progress: Float,
    ) {
        val eased = Easing.inOut(progress)
        glide(scene[a], startA, b.row.toFloat(), b.col.toFloat(), eased)
        glide(scene[b], startB, a.row.toFloat(), a.col.toFloat(), eased)
    }

    override fun finish(scene: Scene) {
        apply(scene, 1f)
        scene.swapSlots(a, b)
    }

    private companion object {
        const val SWAP_SECONDS = 0.18f
    }
}

/** A drag that was let go short of the commit point: the gem (and the neighbour it nudged) spring back. */
class SettleAnimation(
    private val held: Pos,
    private val neighbour: Pos?,
) : Animation {
    override val seconds = SETTLE_SECONDS
    private var startHeld: Frame? = null
    private var startNeighbour: Frame? = null

    override fun start(scene: Scene) {
        startHeld = Frame.of(scene[held])
        startNeighbour = neighbour?.let { Frame.of(scene[it]) }
    }

    override fun apply(
        scene: Scene,
        progress: Float,
    ) {
        val eased = Easing.out(progress)
        glide(scene[held], startHeld, held.row.toFloat(), held.col.toFloat(), eased)
        neighbour?.let { glide(scene[it], startNeighbour, it.row.toFloat(), it.col.toFloat(), eased) }
    }

    override fun finish(scene: Scene) = apply(scene, 1f)

    private companion object {
        const val SETTLE_SECONDS = 0.12f
    }
}

/** Shrinks and fades the gems on [cells], then removes them. */
class ClearAnimation(
    private val cells: Set<Pos>,
    private val points: Int,
    private val onCleared: (Int) -> Unit,
) : Animation {
    override val seconds = CLEAR_SECONDS

    override fun apply(
        scene: Scene,
        progress: Float,
    ) {
        for (pos in cells) {
            val sprite = scene[pos] ?: continue
            sprite.scale = 1f - progress
            sprite.alpha = 1f - progress
        }
    }

    override fun finish(scene: Scene) {
        for (pos in cells) scene.remove(pos)
        onCleared(points)
    }

    private companion object {
        const val CLEAR_SECONDS = 0.2f
    }
}

/** Drops gems straight down their columns, speeding up as they fall. */
class FallAnimation(
    private val moves: List<GemMove>,
) : Animation {
    override val seconds = fallSeconds(moves.maxOfOrNull { it.to.row - it.from.row } ?: 0)

    override fun apply(
        scene: Scene,
        progress: Float,
    ) {
        val eased = Easing.fall(progress)
        for (move in moves) {
            scene[move.from]?.row = move.from.row + (move.to.row - move.from.row) * eased
        }
    }

    override fun finish(scene: Scene) {
        apply(scene, 1f)
        val sprites = moves.map { scene.remove(it.from) }
        moves.forEachIndexed { index, move -> sprites[index]?.let { scene.place(move.to, it) } }
    }
}

/** New gems enter from above the board and fall into place. */
class SpawnAnimation(
    private val gems: List<Placed>,
) : Animation {
    private class Entry(
        val sprite: Sprite,
        val startRow: Float,
        val targetRow: Float,
    )

    private val perColumn = gems.groupingBy { it.pos.col }.eachCount()
    private var entries: List<Entry> = emptyList()
    override val seconds = fallSeconds(perColumn.values.maxOrNull() ?: 0)

    override fun start(scene: Scene) {
        entries =
            gems.map { placed ->
                val start = (placed.pos.row - checkNotNull(perColumn[placed.pos.col])).toFloat()
                val sprite = Sprite(placed.piece, start, placed.pos.col.toFloat())
                scene.place(placed.pos, sprite)
                Entry(sprite, start, placed.pos.row.toFloat())
            }
    }

    override fun apply(
        scene: Scene,
        progress: Float,
    ) {
        val eased = Easing.fall(progress)
        for (entry in entries) entry.sprite.row = entry.startRow + (entry.targetRow - entry.startRow) * eased
    }

    override fun finish(scene: Scene) = apply(scene, 1f)
}

/**
 * Tells the player the board was stuck: the gems rattle under a notice, shrink away, and the
 * rearranged [board] grows back in. Long enough to see that something happened.
 */
class ReshuffleAnimation(
    private val board: Board,
) : Animation {
    override val seconds = SHAKE_SECONDS + SHRINK_SECONDS + GROW_SECONDS
    private var swapped = false

    override fun start(scene: Scene) {
        swapped = false
        scene.message = MESSAGE
        scene.messageAlpha = 0f
    }

    override fun apply(
        scene: Scene,
        progress: Float,
    ) {
        val t = progress * seconds
        when {
            t < SHAKE_SECONDS -> {
                scene.messageAlpha = minOf(1f, t / NOTICE_FADE_SECONDS)
                scene.forEach { it.dx = rattle(it, t) }
            }

            t < SHAKE_SECONDS + SHRINK_SECONDS -> {
                val scale = 1f - (t - SHAKE_SECONDS) / SHRINK_SECONDS
                scene.forEach {
                    it.dx = 0f
                    it.scale = scale
                }
            }

            else -> {
                if (!swapped) {
                    scene.load(board)
                    swapped = true
                }
                val scale = (t - SHAKE_SECONDS - SHRINK_SECONDS) / GROW_SECONDS
                scene.forEach { it.scale = scale }
            }
        }
    }

    override fun finish(scene: Scene) = scene.load(board)

    /** Neighbouring gems rattle in opposite directions; the shake builds up over its first moments. */
    private fun rattle(
        sprite: Sprite,
        t: Float,
    ): Float {
        val phase = if ((sprite.row + sprite.col).toInt() % 2 == 0) 0.0 else PI
        val buildUp = minOf(1f, t / RATTLE_BUILD_UP_SECONDS)
        return (RATTLE_AMPLITUDE * buildUp * sin(2 * PI * RATTLE_HZ * t + phase)).toFloat()
    }

    private companion object {
        const val MESSAGE = "No moves left - reshuffling"
        const val SHAKE_SECONDS = 0.8f
        const val SHRINK_SECONDS = 0.3f
        const val GROW_SECONDS = 0.4f
        const val NOTICE_FADE_SECONDS = 0.2f
        const val RATTLE_BUILD_UP_SECONDS = 0.15f
        const val RATTLE_AMPLITUDE = 0.07f
        const val RATTLE_HZ = 9.0
    }
}

private const val FALL_BASE_SECONDS = 0.1f
private const val FALL_SECONDS_PER_ROW = 0.06f

private fun fallSeconds(rows: Int): Float = FALL_BASE_SECONDS + FALL_SECONDS_PER_ROW * rows

/** Plays animations one after another. Time left over at the end of one carries into the next. */
class Timeline(
    private val animations: List<Animation>,
) {
    private var index = 0
    private var elapsed = 0f
    private var started = false

    val isFinished: Boolean get() = index >= animations.size

    /** Plays everything that is left at once, so a tap during playback jumps to the end of the move. */
    fun skip(scene: Scene) = advance(SKIP_SECONDS, scene)

    fun advance(
        seconds: Float,
        scene: Scene,
    ) {
        elapsed += seconds
        while (!isFinished) {
            val current = animations[index]
            if (!started) {
                current.start(scene)
                started = true
            }
            if (elapsed < current.seconds) {
                current.apply(scene, elapsed / current.seconds)
                return
            }
            current.finish(scene)
            elapsed -= current.seconds
            index++
            started = false
        }
    }

    companion object {
        private const val SKIP_SECONDS = 3_600f

        /** One animation per core step, in order. [onCleared] hears the points of each clear as it finishes. */
        fun forSteps(
            steps: List<Step>,
            onCleared: (Int) -> Unit,
        ): Timeline {
            val animations = mutableListOf<Animation>()
            var index = 0
            while (index < steps.size) {
                val step = steps[index]
                if (step is Step.Activate) {
                    val rest = steps.subList(index, steps.size)
                    val chain = rest.takeWhile { it is Step.Activate }.filterIsInstance<Step.Activate>()
                    animations += ActivateAnimation(chain)
                    index += chain.size
                } else {
                    animations += animationFor(step, onCleared)
                    index++
                }
            }
            return Timeline(animations)
        }

        private fun animationFor(
            step: Step,
            onCleared: (Int) -> Unit,
        ): Animation =
            when (step) {
                is Step.Swap -> SwapAnimation(step.a, step.b)
                is Step.Clear -> ClearAnimation(step.cells, step.points, onCleared)
                is Step.Activate -> ActivateAnimation(listOf(step))
                is Step.Create -> CreateAnimation(step.pos, step.piece)
                is Step.Fall -> FallAnimation(step.moves)
                is Step.Spawn -> SpawnAnimation(step.gems)
                is Step.Reshuffle -> ReshuffleAnimation(step.board)
            }
    }
}

object Easing {
    private const val SMOOTHSTEP_BASE = 3f

    /** Slow start, slow end: smoothstep. */
    fun inOut(t: Float): Float = t * t * (SMOOTHSTEP_BASE - 2f * t)

    /** Fast start, gentle stop. */
    fun out(t: Float): Float = 1f - (1f - t) * (1f - t)

    /** Starts slow and speeds up, like something falling. */
    fun fall(t: Float): Float = t * t
}
