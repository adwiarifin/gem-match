package dev.kesatria.gemmatch.core

/**
 * The special gem a cleared [group] makes, or null for a plain match. Any run of 5 or more is the colour gem. A run
 * of 3 with a hanging gem is a line gem along the run (hanging off an end) or a short X (hanging off the middle). A
 * run of 4 is a blast. An L with both arms of 3 or more is a cross gem. A T or + is a full X.
 */
fun classify(group: MatchGroup): Special? {
    val longest = group.runs.maxOf { it.length }
    return when {
        longest >= COLOUR_RUN -> Special.COLOUR
        group.hanging != null -> hangingSpecial(group.runs.single(), group.hanging)
        !group.isCompound -> if (longest == BLAST_RUN) Special.BLAST else null
        hasCrossingInsideARun(group) -> Special.X_FULL
        else -> Special.CROSS
    }
}

private const val BLAST_RUN = 4
private const val COLOUR_RUN = 5

private fun hangingSpecial(
    run: Run,
    hanging: Pos,
): Special {
    val joint = run.cells.first { it.isAdjacentTo(hanging) }
    return when {
        joint == run.cells[run.cells.size / 2] -> Special.X_SHORT
        run.isHorizontal -> Special.LINE_ROW
        else -> Special.LINE_COL
    }
}

/** A crossing that is not the end of both runs makes a T or a +, a full X. */
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

private fun Run.endsAt(pos: Pos): Boolean = pos == cells.first() || pos == cells.last()

/**
 * The special pieces a clear earns, one per qualifying group. On the player's own swap each goes on the swapped
 * cell that belongs to the group, the first of [swapped] (the dragged cell comes first). Anywhere else, a cascade
 * match or a group the swap did not touch, it goes at the group's centre: the crossing of an L, T or +, the gem a
 * hanging gem touches, the middle of a straight run.
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
        else -> crossingOf(group) ?: run.cells[run.cells.size / 2]
    }
}

private fun crossingOf(group: MatchGroup): Pos? {
    val horizontal = group.runs.filter { it.isHorizontal }
    val vertical = group.runs.filter { !it.isHorizontal }
    return horizontal
        .flatMap { h -> vertical.map { v -> h.cells.firstOrNull { it in v.cells } } }
        .firstNotNullOfOrNull { it }
}
