package dev.kesatria.gemmatch.core

import kotlin.math.abs

/** Two colour gems: the one that landed on [origin] takes the whole board. */
internal fun clearBoard(origin: Pos): Step.Activate = Step.Activate(origin, Special.COLOUR, ALL_CELLS)

/**
 * A colour gem on [colourAt] swapped with the special on [partnerAt]. The colour gem converts every piece of the
 * partner's colour into the partner's special and lists those cells; then each converted piece fires, in board
 * order, from where it sits. Converted line gems copy the partner's direction because they copy its kind.
 */
internal fun colourWithSpecial(
    swapped: Board,
    colourAt: Pos,
    partnerAt: Pos,
): List<Step.Activate> {
    val partner = checkNotNull(swapped[partnerAt])
    val converted = colourCells(swapped, colourAt, partner.gem) ?: return emptyList()
    val special = checkNotNull(partner.special)
    val fires = converted.filter { it != colourAt }.sortedWith(BOARD_ORDER)
    return listOf(Step.Activate(colourAt, Special.COLOUR, converted)) +
        fires.map { Step.Activate(it, special, effectCells(it, special)) }
}

/** Two of these swapped together make one bigger effect instead of two overlapping ones (D20). */
internal val DOUBLED = setOf(Special.BLAST, Special.LINE_ROW, Special.LINE_COL)

/**
 * What two [special] gems of the same kind clear when swapped, centred on [landing], the cell the dragged one moved
 * to: three columns or three rows, always including both gems' own, or a 5x5 square for two blasts.
 */
internal fun pairCells(
    special: Special,
    landing: Pos,
): Set<Pos> =
    ALL_CELLS.filterTo(mutableSetOf()) { cell ->
        when (special) {
            Special.LINE_COL -> abs(cell.col - landing.col) <= 1
            Special.LINE_ROW -> abs(cell.row - landing.row) <= 1
            else -> abs(cell.col - landing.col) <= 2 && abs(cell.row - landing.row) <= 2
        }
    }
