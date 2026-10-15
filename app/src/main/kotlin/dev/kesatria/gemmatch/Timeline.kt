package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.Pos

/** A timed change to the [Scene]. [apply] is called with progress 0..1, then [finish] once. */
interface Animation {
    val seconds: Float

    fun apply(
        scene: Scene,
        progress: Float,
    )

    fun finish(scene: Scene)
}

/** Slides the sprites at [a] and [b] into each other's cell, then swaps their slots. */
class SwapAnimation(
    private val a: Pos,
    private val b: Pos,
) : Animation {
    override val seconds = SWAP_SECONDS

    override fun apply(
        scene: Scene,
        progress: Float,
    ) {
        val eased = Easing.inOut(progress)
        move(scene[a], a, b, eased)
        move(scene[b], b, a, eased)
    }

    override fun finish(scene: Scene) {
        apply(scene, 1f)
        scene.swapSlots(a, b)
    }

    private fun move(
        sprite: Sprite?,
        from: Pos,
        to: Pos,
        t: Float,
    ) {
        if (sprite == null) return
        sprite.row = from.row + (to.row - from.row) * t
        sprite.col = from.col + (to.col - from.col) * t
    }

    private companion object {
        const val SWAP_SECONDS = 0.18f
    }
}

/** Plays animations one after another. Time left over at the end of one carries into the next. */
class Timeline(
    private val animations: List<Animation>,
) {
    private var index = 0
    private var elapsed = 0f

    val isFinished: Boolean get() = index >= animations.size

    fun advance(
        seconds: Float,
        scene: Scene,
    ) {
        elapsed += seconds
        while (!isFinished) {
            val current = animations[index]
            if (elapsed < current.seconds) {
                current.apply(scene, elapsed / current.seconds)
                return
            }
            current.finish(scene)
            elapsed -= current.seconds
            index++
        }
    }
}

object Easing {
    /** Slow start, slow end: smoothstep. */
    fun inOut(t: Float): Float = t * t * (3f - 2f * t)
}
