package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.FinishedRun
import dev.kesatria.gemmatch.core.Special

private const val MS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
private const val DIGITS_PER_GROUP = 3

/** One line of the stats list: what it is and its value, already formatted for the screen. */
data class StatRow(
    val label: String,
    val value: String,
)

/** `1234567` as `1,234,567`, the same in every locale. */
fun formatCount(count: Long): String {
    val digits = count.toString().removePrefix("-")
    val grouped =
        digits
            .reversed()
            .chunked(DIGITS_PER_GROUP)
            .joinToString(",")
            .reversed()
    return if (count < 0) "-$grouped" else grouped
}

/** `m:ss` under an hour, `h:mm:ss` from an hour on. Negative time reads as zero. */
fun formatDuration(ms: Long): String {
    val totalSeconds = maxOf(ms, 0L) / MS_PER_SECOND
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    val minutes = totalSeconds / SECONDS_PER_MINUTE % MINUTES_PER_HOUR
    val hours = totalSeconds / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR)
    val ss = seconds.toString().padStart(2, '0')
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:$ss" else "$minutes:$ss"
}

private fun nameOf(special: Special): String =
    when (special) {
        Special.BLAST -> "Blast"
        Special.LINE_ROW -> "Row line"
        Special.LINE_COL -> "Column line"
        Special.CROSS -> "Cross"
        Special.X_SHORT -> "Short X"
        Special.X_FULL -> "Full X"
        Special.COLOUR -> "Colour"
    }

/** Every stat of a finished run, in the order the card and the stats screen list them. */
fun statRows(run: FinishedRun): List<StatRow> {
    val stats = run.stats
    val average = if (stats.moves == 0) 0 else run.score / stats.moves
    val made = Special.entries.filter { (stats.created[it] ?: 0) > 0 }
    val fired = Special.entries.filter { (stats.fired[it] ?: 0) > 0 }
    return listOf(
        StatRow("Moves", formatCount(stats.moves.toLong())),
        StatRow("Longest cascade", formatCount(stats.cascade.toLong())),
        StatRow("Biggest move score", formatCount(stats.moveScore.toLong())),
        StatRow("Average score per move", formatCount(average.toLong())),
        StatRow("Gems cleared", formatCount(stats.gems.toLong())),
        StatRow("Biggest match", formatCount(stats.match.toLong())),
        StatRow("Special gems made", formatCount(stats.created.values.sumOf { it.toLong() })),
    ) + made.map { StatRow("  ${nameOf(it)} made", formatCount((stats.created[it] ?: 0).toLong())) } +
        StatRow("Special gems fired", formatCount(stats.fired.values.sumOf { it.toLong() })) +
        fired.map { StatRow("  ${nameOf(it)} fired", formatCount((stats.fired[it] ?: 0).toLong())) } +
        listOf(
            StatRow("Active time", formatDuration(run.activeMs)),
            StatRow("Total time", formatDuration(run.wallMs)),
        )
}
