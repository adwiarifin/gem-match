package dev.kesatria.gemmatch.core

/** How far a short X reaches along each diagonal, in cells. */
const val SHORT_X_REACH = 2

/** What a special gem does when the player swaps it. */
enum class Special {
    BLAST,
    LINE_ROW,
    LINE_COL,
    CROSS,

    /** Both diagonals through it, out to [SHORT_X_REACH] cells. Made by a short T. */
    X_SHORT,

    /** Both diagonals through it, right across the board. Made by a long T or `+`. */
    X_FULL,

    COLOUR,
}
