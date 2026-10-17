package dev.kesatria.gemmatch.core

/** The special gem a cleared [group] makes, or null for a plain match. Priority: colour, cross, line, blast. */
fun classify(group: MatchGroup): Special? {
    val longest = group.runs.maxOf { it.length }
    return when {
        longest >= COLOUR_RUN -> Special.COLOUR
        !group.isCompound -> if (longest == BLAST_RUN) Special.BLAST else null
        hasCrossingInsideARun(group) -> Special.CROSS
        else -> lineFor(group)
    }
}

private const val BLAST_RUN = 4
private const val COLOUR_RUN = 5

/** A crossing that is not the end of both runs makes a T or a +, so a cross gem. */
private fun hasCrossingInsideARun(group: MatchGroup): Boolean {
    val horizontal = group.runs.filter { it.isHorizontal }
    val vertical = group.runs.filter { !it.isHorizontal }
    return horizontal.any { h ->
        vertical.any { v ->
            val crossing = h.cells.firstOrNull { it in v.cells }
            crossing != null && !(h.endsAt(crossing) && v.endsAt(crossing))
        }
    }
}

/** An L clears along its longer arm. Equal arms clear the row. */
private fun lineFor(group: MatchGroup): Special {
    val horizontal = group.runs.filter { it.isHorizontal }.maxOf { it.length }
    val vertical = group.runs.filter { !it.isHorizontal }.maxOf { it.length }
    return if (horizontal >= vertical) Special.LINE_ROW else Special.LINE_COL
}

private fun Run.endsAt(pos: Pos): Boolean = pos == cells.first() || pos == cells.last()

/**
 * The special pieces a clear earns, one per qualifying group. On the player's own swap each goes on the swapped
 * cell that belongs to the group, the first of [swapped] (the dragged cell comes first). Anywhere else, a cascade
 * match or a group the swap did not touch, it goes at the group's centre: the crossing of an L, T or +, the middle
 * of a straight run.
 */
fun createdPieces(
    groups: List<MatchGroup>,
    swapped: List<Pos>,
): List<Step.Create> =
    groups.mapNotNull { group ->
        val special = classify(group) ?: return@mapNotNull null
        val pos = swapped.firstOrNull { it in group.cells } ?: anchorOf(group)
        val piece = if (special == Special.COLOUR) Piece(null, special) else Piece(group.runs.first().gem, special)
        Step.Create(pos, piece)
    }

private fun anchorOf(group: MatchGroup): Pos {
    val horizontal = group.runs.filter { it.isHorizontal }
    val vertical = group.runs.filter { !it.isHorizontal }
    val crossing =
        horizontal
            .flatMap { h -> vertical.map { v -> h.cells.firstOrNull { it in v.cells } } }
            .firstNotNullOfOrNull { it }
    if (crossing != null) return crossing
    val run = group.runs.first()
    return run.cells[run.cells.size / 2]
}
