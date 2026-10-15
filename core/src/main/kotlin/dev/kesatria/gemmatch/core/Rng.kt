package dev.kesatria.gemmatch.core

private const val GOLDEN_GAMMA = -0x61c8864680b583ebL
private const val MIX_1 = -0x40a7b892e31b1a47L
private const val MIX_2 = -0x6b2fb644ecceee15L
private const val SHIFT_1 = 30
private const val SHIFT_2 = 27
private const val SHIFT_3 = 31
private const val INT_SHIFT = 33

/**
 * SplitMix64 generator with readable state, so a saved game resumes the exact same future.
 * Mutable on purpose and kept private to whoever owns it; [copy] before sharing.
 */
class Rng(
    state: Long,
) {
    var state: Long = state
        private set

    fun nextLong(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr SHIFT_1)) * MIX_1
        z = (z xor (z ushr SHIFT_2)) * MIX_2
        return z xor (z ushr SHIFT_3)
    }

    /** A value in `0 until bound`. The modulo bias is below 2^-28 for small bounds. */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive, was $bound" }
        return ((nextLong() ushr INT_SHIFT) % bound).toInt()
    }

    fun nextGem(): Gem = Gem.entries[nextInt(Gem.entries.size)]

    fun copy(): Rng = Rng(state)
}
