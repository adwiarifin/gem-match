package dev.kesatria.gemmatch.core

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
    val fires = converted.filter { it != colourAt }.sortedWith(compareBy({ it.row }, { it.col }))
    return listOf(Step.Activate(colourAt, Special.COLOUR, converted)) +
        fires.map { Step.Activate(it, special, effectCells(it, special)) }
}

internal val ALL_CELLS: Set<Pos> =
    (0 until BOARD_SIZE).flatMapTo(mutableSetOf()) { row -> (0 until BOARD_SIZE).map { col -> Pos(row, col) } }
