package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.Piece
import dev.kesatria.gemmatch.core.Pos

/** A special piece pops in on the cell the clear just emptied. */
class CreateAnimation(
    private val pos: Pos,
    private val piece: Piece,
) : Animation {
    private var sprite: Sprite? = null
    override val seconds = POP_SECONDS

    override fun start(scene: Scene) {
        sprite =
            Sprite(piece, pos.row.toFloat(), pos.col.toFloat()).also {
                it.scale = 0f
                scene.place(pos, it)
            }
    }

    override fun apply(
        scene: Scene,
        progress: Float,
    ) {
        sprite?.scale = Easing.out(progress)
    }

    override fun finish(scene: Scene) {
        sprite?.scale = 1f
    }

    private companion object {
        const val POP_SECONDS = 0.15f
    }
}
