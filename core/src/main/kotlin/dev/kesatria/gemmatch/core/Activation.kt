package dev.kesatria.gemmatch.core

/**
 * The cells a [special] fired from [origin] takes, clipped to the board. The colour gem needs a partner colour and
 * the board, so [activations] works it out instead.
 */
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
 * Everything that fires in one clear, in firing order. First the swap's own effects, when [swap] is the player's
 * swap on the board [board] where it has already happened: a special fires from the cell it moved to, both do if
 * both are special, and a colour gem takes every gem of its partner's colour and itself. Then every special that is
 * part of a match in [matched] fires from where it sits. Last, an effect that covers another special fires it too,
 * once. A covered colour gem has no partner, so it is cleared and fires nothing.
 */
fun firings(
    board: Board,
    matched: Set<Pos>,
    swap: Pair<Pos, Pos>?,
): List<Step.Activate> {
    val fired = swap?.let { initialActivations(board, it.first, it.second).toMutableList() } ?: mutableListOf()
    val seen = fired.mapTo(mutableSetOf()) { it.origin }
    for (pos in matched.sortedWith(compareBy({ it.row }, { it.col }))) {
        val special = board[pos]?.special
        if (special != null && special != Special.COLOUR && seen.add(pos)) {
            fired += Step.Activate(pos, special, effectCells(pos, special))
        }
    }
    chain(board, fired, seen)
    return fired
}

/** Fires every special that an effect in [fired] covers and that has not fired yet, in the order they are reached. */
private fun chain(
    board: Board,
    fired: MutableList<Step.Activate>,
    seen: MutableSet<Pos>,
) {
    var next = 0
    while (next < fired.size) {
        for (pos in fired[next++].cells.sortedWith(compareBy({ it.row }, { it.col }))) {
            val special = board[pos]?.special
            if (special != null && special != Special.COLOUR && seen.add(pos)) {
                fired += Step.Activate(pos, special, effectCells(pos, special))
            }
        }
    }
}

private fun initialActivations(
    swapped: Board,
    a: Pos,
    b: Pos,
): List<Step.Activate> {
    val first = swapped[a]?.special
    val second = swapped[b]?.special
    return when {
        first == Special.COLOUR && second == Special.COLOUR -> listOf(clearBoard(b))
        first == Special.COLOUR && second != null -> colourWithSpecial(swapped, a, b)
        second == Special.COLOUR && first != null -> colourWithSpecial(swapped, b, a)
        else ->
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
    }
}

internal fun colourCells(
    board: Board,
    origin: Pos,
    colour: Gem?,
): Set<Pos>? {
    if (colour == null) return null
    return ALL_CELLS.filterTo(mutableSetOf(origin)) { board[it]?.gem == colour }
}
