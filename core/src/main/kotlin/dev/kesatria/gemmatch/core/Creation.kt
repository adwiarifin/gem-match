package dev.kesatria.gemmatch.core

/**
 * The special gem a cleared [group] makes, or null for a plain match.
 * - Two or more crossing runs beat everything else, even a run of 5: an L (both arms 3 or more) is a cross gem, a T
 *   or + whose runs are all exactly 3 is a short X, and a T or + with any run of 4 or more is a full X.
 * - A run of 3 with a gem hanging off an end is a line gem along the run.
 * - A straight run of 5 or more is the colour gem, and a straight run of 4 is a blast.
 */
fun classify(group: MatchGroup): Special? {
    val longest = group.runs.maxOf { it.length }
    return when {
        group.isCompound -> compoundSpecial(group, longest)
        group.hanging != null -> if (group.runs.single().isHorizontal) Special.LINE_ROW else Special.LINE_COL
        longest >= COLOUR_RUN -> Special.COLOUR
        longest == BLAST_RUN -> Special.BLAST
        else -> null
    }
}

private const val BLAST_RUN = 4
private const val COLOUR_RUN = 5

private fun compoundSpecial(
    group: MatchGroup,
    longest: Int,
): Special =
    when {
        crossings(group).none { it.inside } -> Special.CROSS
        longest > MIN_MATCH -> Special.X_FULL
        else -> Special.X_SHORT
    }

/** A place where a horizontal and a vertical run cross, and whether it is inside at least one of the two runs. */
private class Crossing(
    val pos: Pos,
    val inside: Boolean,
)

private fun crossings(group: MatchGroup): List<Crossing> {
    val horizontal = group.runs.filter { it.isHorizontal }
    val vertical = group.runs.filter { !it.isHorizontal }
    return horizontal.flatMap { h ->
        vertical.mapNotNull { v ->
            h.cells.firstOrNull { it in v.cells }?.let { Crossing(it, !(h.endsAt(it) && v.endsAt(it))) }
        }
    }
}

private fun Run.endsAt(pos: Pos): Boolean = pos == cells.first() || pos == cells.last()

/**
 * The special pieces a clear earns, one per qualifying group. On the player's own swap each goes on the swapped
 * cell that belongs to the group, the first of [swapped] (the dragged cell comes first). Anywhere else, a cascade
 * match or a group the swap did not touch, it goes at the group's centre: the crossing of an L, T or +, the end gem
 * a hanging gem touches, the middle of a straight run.
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
    val hanging = group.hanging
    val run = group.runs.first()
    return when {
        hanging != null -> run.cells.first { it.isAdjacentTo(hanging) }
        else -> crossings(group).firstOrNull()?.pos ?: run.cells[run.cells.size / 2]
    }
}
