package dev.kesatria.gemmatch

/** A short beat before the clear of a fired special. The effect animations (ring, beam, streaks) replace it. */
class ActivateAnimation : Animation {
    override val seconds = BEAT_SECONDS

    override fun apply(
        scene: Scene,
        progress: Float,
    ) = Unit

    override fun finish(scene: Scene) = Unit

    private companion object {
        const val BEAT_SECONDS = 0.12f
    }
}
