package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.Special
import dev.kesatria.gemmatch.core.Step

/**
 * Plays one fired special: a ring over the 3x3 for a blast, a beam along the row or column for a line (both for a
 * cross), streaks to each taken gem for the colour gem. The gems are still on the board while it plays; the Clear
 * that follows removes them. A [first] effect of a move is slower so the player sees what started; the rest of a
 * chain is quicker so a long chain does not stall the board.
 */
class ActivateAnimation(
    step: Step.Activate,
    first: Boolean = true,
) : Animation {
    private val effect = Effect(step.origin, step.special, step.cells)
    override val seconds = baseSeconds(step.special) * if (first) 1f else CHAIN_SPEEDUP

    override fun start(scene: Scene) {
        scene.effects += effect
    }

    override fun apply(
        scene: Scene,
        progress: Float,
    ) {
        effect.progress = progress
    }

    override fun finish(scene: Scene) {
        scene.effects -= effect
    }

    private fun baseSeconds(special: Special) =
        when (special) {
            Special.BLAST -> BLAST_SECONDS
            Special.LINE_ROW, Special.LINE_COL -> LINE_SECONDS
            Special.CROSS -> CROSS_SECONDS
            Special.COLOUR -> COLOUR_SECONDS
        }

    private companion object {
        const val BLAST_SECONDS = 0.4f
        const val LINE_SECONDS = 0.4f
        const val CROSS_SECONDS = 0.45f
        const val COLOUR_SECONDS = 0.55f
        const val CHAIN_SPEEDUP = 0.6f
    }
}
