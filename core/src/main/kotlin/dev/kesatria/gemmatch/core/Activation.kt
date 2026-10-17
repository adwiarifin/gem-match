package dev.kesatria.gemmatch.core

/**
 * The cells a [special] fired from [origin] takes, clipped to the board. The colour gem has no fixed cells (they depend
 * on its partner's colour and the board), so `firing` and `initialActivations` handle it and this fails loudly if it
 * is ever asked.
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
        Special.X_SHORT -> diagonalCells(origin, SHORT_X_REACH)
        Special.X_FULL -> diagonalCells(origin, BOARD_SIZE)
        Special.COLOUR -> error("the colour gem has no fixed cells, they depend on its partner: see colourCells")
    }

private fun blastCells(origin: Pos): Set<Pos> =
    (origin.row - 1..origin.row + 1)
        .flatMap { row -> (origin.col - 1..origin.col + 1).map { col -> Pos(row, col) } }
        .filter { it.isInBounds() }
        .toSet()

/** The cell itself and both diagonals through it, [reach] cells each way, clipped to the board. */
private fun diagonalCells(
    origin: Pos,
    reach: Int,
): Set<Pos> =
    (-reach..reach)
        .flatMap { step ->
            listOf(Pos(origin.row + step, origin.col + step), Pos(origin.row + step, origin.col - step))
        }.filterTo(mutableSetOf()) { it.isInBounds() }

private fun rowCells(row: Int): Set<Pos> = (0 until BOARD_SIZE).mapTo(mutableSetOf()) { Pos(row, it) }

private fun columnCells(col: Int): Set<Pos> = (0 until BOARD_SIZE).mapTo(mutableSetOf()) { Pos(it, col) }

/**
 * Everything that fires in one clear, in firing order. First the swap's own effects, when [swap] is the player's
 * swap on the board [board] where it has already happened: a special fires from the cell it moved to, both do if
 * both are special, and a colour gem takes every gem of its partner's colour and itself. Then every special that is
 * part of a match in [matched] fires from where it sits. Last, an effect that covers another special fires it too,
 * once. A colour gem covered by an effect takes the colour of the gem that covered it (see [fireNew]).
 */
fun firings(
    board: Board,
    matched: Set<Pos>,
    swap: Pair<Pos, Pos>?,
): List<Step.Activate> {
    val fired = swap?.let { initialActivations(board, it.first, it.second).toMutableList() } ?: mutableListOf()
    val seen = fired.mapTo(mutableSetOf()) { it.origin }
    swap?.let { seen += listOf(it.first, it.second) }
    fireNew(board, matched, null, fired, seen)
    var next = 0
    while (next < fired.size) {
        val cover = fired[next++]
        fireNew(board, cover.cells, cover, fired, seen)
    }
    return fired
}

/**
 * Fires, in board order, every special among [cells] that has not fired yet. A colour gem has no colour of its own,
 * so it fires only when an effect [cover]s it, and then takes every gem of the covering gem's colour and itself.
 * Only another colour gem has no colour, and one never covers another: it takes gems of one colour, never a colour gem.
 */
private fun fireNew(
    board: Board,
    cells: Set<Pos>,
    cover: Step.Activate?,
    fired: MutableList<Step.Activate>,
    seen: MutableSet<Pos>,
) {
    for (pos in cells.sortedWith(BOARD_ORDER)) {
        val fire = if (pos in seen) null else firing(board, pos, cover)
        if (fire != null) {
            seen += pos
            fired += fire
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
        first == second && first in DOUBLED -> listOf(Step.Activate(b, checkNotNull(first), pairCells(first, b)))
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

/** The effect of the special at [pos], or null if it has none (no special, or a colour gem with no colour to take). */
private fun firing(
    board: Board,
    pos: Pos,
    cover: Step.Activate?,
): Step.Activate? {
    val special = board[pos]?.special ?: return null
    val take =
        if (special == Special.COLOUR) {
            colourCells(board, pos, cover?.let { board[it.origin]?.gem })
        } else {
            effectCells(pos, special)
        }
    return take?.let { Step.Activate(pos, special, it) }
}
