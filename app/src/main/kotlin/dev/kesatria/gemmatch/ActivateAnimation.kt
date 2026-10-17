package dev.kesatria.gemmatch

/** A short beat before the clear of a fired special. The ring, beam and streaks replace it with the effect animations. */
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
