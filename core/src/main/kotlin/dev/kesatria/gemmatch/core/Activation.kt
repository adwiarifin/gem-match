package dev.kesatria.gemmatch.core

/** The cells a [special] fired from [origin] takes, clipped to the board. The colour gem needs a partner, see [activations]. */
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
 * The effects the swap of [a] and [b] fires, on the board [swapped] where it has already happened. A special
 * fires from the cell it moved to; if both are special both fire, [a] first.
 */
fun activations(
    swapped: Board,
    a: Pos,
    b: Pos,
): List<Step.Activate> =
    listOf(a, b).mapNotNull { origin ->
        val special = swapped[origin]?.special
        if (special == null || special == Special.COLOUR) null else Step.Activate(origin, special, effectCells(origin, special))
    }
