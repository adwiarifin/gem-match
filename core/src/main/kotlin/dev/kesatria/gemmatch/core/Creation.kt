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
