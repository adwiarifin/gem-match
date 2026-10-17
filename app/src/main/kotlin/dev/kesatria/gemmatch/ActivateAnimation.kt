package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.Piece
import dev.kesatria.gemmatch.core.Special
import dev.kesatria.gemmatch.core.Step
import kotlin.math.min

/**
 * Plays every special that fires in one clear: a ring over the 3x3 for a blast, a beam along the row or column for a
 * line (both for a cross), beams along the diagonals for the X gems, streaks to each taken gem for the colour gem.
 * The gems are still on the board while it plays; the Clear that follows removes them.
 *
 * The first effect plays at full length. The rest of a chain overlap it: each starts a short while after the one
 * before and runs quicker, and the whole chain is squeezed so it never takes much longer than one effect, however
 * long it is. A gem that a colour gem converts is shown as its new kind when its own effect starts.
 */
class ActivateAnimation(
    private val steps: List<Step.Activate>,
) : Animation {
    private class Timed(
        val step: Step.Activate,
        val effect: Effect,
        val start: Float,
        val length: Float,
    ) {
        var shown = false
        var done = false
    }

    private val timed: List<Timed>
    override val seconds: Float

    init {
        val gap = if (steps.size > 1) min(CHAIN_GAP, CHAIN_SPREAD / (steps.size - 1)) else 0f
        timed =
            steps.mapIndexed { index, step ->
                val scale = if (index == 0) 1f else CHAINED_SCALE
                val effect = Effect(step.origin, step.special, step.cells)
                Timed(step, effect, index * gap, baseSeconds(step.special) * scale)
            }
        seconds = timed.maxOf { it.start + it.length }
    }

    override fun start(scene: Scene) {
        for (index in timed.indices) {
            timed[index].shown = false
            timed[index].done = false
        }
    }

    override fun apply(
        scene: Scene,
        progress: Float,
    ) {
        val now = progress * seconds
        for (index in timed.indices) {
            val item = timed[index]
            if (item.done || now < item.start) continue
            if (!item.shown) show(scene, item)
            item.effect.progress = ((now - item.start) / item.length).coerceIn(0f, 1f)
            if (now >= item.start + item.length) hide(scene, item)
        }
    }

    override fun finish(scene: Scene) {
        for (index in timed.indices) {
            val item = timed[index]
            if (!item.shown) show(scene, item)
            if (!item.done) hide(scene, item)
        }
    }

    private fun show(
        scene: Scene,
        item: Timed,
    ) {
        item.shown = true
        scene.effects += item.effect
        convert(scene, item.step)
    }

    private fun hide(
        scene: Scene,
        item: Timed,
    ) {
        item.done = true
        scene.effects -= item.effect
    }

    /** A gem a colour gem turned into this special shows as it when its own effect starts. */
    private fun convert(
        scene: Scene,
        step: Step.Activate,
    ) {
        val sprite = scene[step.origin] ?: return
        if (step.special == Special.COLOUR || sprite.piece.special == step.special) return
        scene.place(step.origin, Sprite(Piece(sprite.piece.gem, step.special), sprite.row, sprite.col))
    }

    private fun baseSeconds(special: Special): Float =
        when (special) {
            Special.BLAST -> BLAST_SECONDS
            Special.LINE_ROW, Special.LINE_COL -> LINE_SECONDS
            Special.CROSS -> CROSS_SECONDS
            Special.X_SHORT -> X_SHORT_SECONDS
            Special.X_FULL -> X_FULL_SECONDS
            Special.COLOUR -> COLOUR_SECONDS
        }

    companion object {
        /** The most a chain adds beyond its first effect, however many effects it holds. */
        const val CHAIN_SPREAD = 0.7f
        private const val CHAIN_GAP = 0.12f
        private const val CHAINED_SCALE = 0.6f
        private const val BLAST_SECONDS = 0.4f
        private const val LINE_SECONDS = 0.4f
        private const val CROSS_SECONDS = 0.45f
        private const val X_SHORT_SECONDS = 0.35f
        private const val X_FULL_SECONDS = 0.45f
        private const val COLOUR_SECONDS = 0.55f
    }
}
