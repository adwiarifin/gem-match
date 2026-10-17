package dev.kesatria.gemmatch.core

/** The cells a [special] fired from [origin] takes, clipped to the board. The colour gem needs a partner and board, see [activations]. */
fun effectCells(
    origin: Pos,
    special: Special,
): Set<Pos> =
    when (special) {
        Special.BLAST -> blastCells(origin)
        Special.LINE_ROW -> rowCells(origin.row)
        Special.LINE_COL -> columnCells(origin.col)
        Special.CROSS -> rowCells(origin.row) + columnCells(origin.col)
        Special.COLOUR -> emptySet()
    }

private fun blastCells(origin: Pos): Set<Pos> =
    (origin.row - 1..origin.row + 1)
        .flatMap { row -> (origin.col - 1..origin.col + 1).map { col -> Pos(row, col) } }
        .filter { it.isInBounds() }
        .toSet()

private fun rowCells(row: Int): Set<Pos> = (0 until BOARD_SIZE).mapTo(mutableSetOf()) { Pos(row, it) }

private fun columnCells(col: Int): Set<Pos> = (0 until BOARD_SIZE).mapTo(mutableSetOf()) { Pos(it, col) }

/**
 * Everything the swap of [a] and [b] fires, on the board [swapped] where it has already happened, in firing order.
 * A special fires from the cell it moved to; if both are special both fire, [a] first. A colour gem takes every gem
 * of its partner's colour, and itself. An effect that covers another special fires it too, once, from where it
 * sits; a covered colour gem has no partner, so it is cleared and fires nothing.
 */
fun activations(
    swapped: Board,
    a: Pos,
    b: Pos,
): List<Step.Activate> {
    val fired = initialActivations(swapped, a, b).toMutableList()
    val seen = fired.mapTo(mutableSetOf()) { it.origin }
    var next = 0
    while (next < fired.size) {
        for (pos in fired[next++].cells.sortedWith(compareBy({ it.row }, { it.col }))) {
            val special = swapped[pos]?.special
            if (special != null && special != Special.COLOUR && seen.add(pos)) {
                fired += Step.Activate(pos, special, effectCells(pos, special))
            }
        }
    }
    return fired
}

private fun initialActivations(
    swapped: Board,
    a: Pos,
    b: Pos,
): List<Step.Activate> =
    listOf(a to b, b to a).mapNotNull { (origin, partner) ->
        val special = swapped[origin]?.special ?: return@mapNotNull null
        val cells =
            if (special == Special.COLOUR) {
                colourCells(swapped, origin, swapped[partner]?.gem) ?: return@mapNotNull null
            } else {
                effectCells(origin, special)
            }
        Step.Activate(origin, special, cells)
    }

private fun colourCells(
    board: Board,
    origin: Pos,
    colour: Gem?,
): Set<Pos>? {
    if (colour == null) return null
    val all = (0 until BOARD_SIZE).flatMap { row -> (0 until BOARD_SIZE).map { col -> Pos(row, col) } }
    return all.filterTo(mutableSetOf(origin)) { board[it]?.gem == colour }
}
