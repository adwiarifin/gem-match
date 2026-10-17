package dev.kesatria.gemmatch.core

/**
 * What sits in one board cell: a gem, optionally made special.
 * The colour gem has no colour, so [gem] is null for it and for nothing else.
 */
data class Piece(
    val gem: Gem?,
    val special: Special? = null,
) {
    init {
        require((gem == null) == (special == Special.COLOUR)) { "only the colour gem has no gem: $this" }
    }

    val isSpecial: Boolean get() = special != null
}
