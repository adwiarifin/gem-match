package dev.kesatria.gemmatch.core

private val KEY = Regex("[A-Za-z]+")

/** `key=value` pairs separated by commas, in the order given. Values are never negative. */
internal fun encodePairs(pairs: List<Pair<String, Long>>): String {
    val written = pairs.map { (key, value) -> "$key=$value" }
    return written.joinToString(",")
}

/**
 * Reads what [encodePairs] wrote. Null if any pair is malformed (no `=`, an empty or non-letter key, a value that is
 * not a plain non-negative number that fits a `Long`) or a key appears twice. A missing key is the caller's to read
 * as 0.
 */
internal fun decodePairs(text: String): Map<String, Long>? {
    val pieces = if (text.isEmpty()) emptyList() else text.split(',')
    val pairs = pieces.map { readPair(it) }
    return if (null in pairs) null else pairs.filterNotNull().toMap().takeIf { it.size == pairs.size }
}

private fun readPair(piece: String): Pair<String, Long>? {
    val halves = piece.split('=')
    val key = halves.firstOrNull()?.takeIf { halves.size == 2 && KEY.matches(it) }
    val value = halves.getOrNull(1)?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }?.toLongOrNull()
    return if (key != null && value != null) key to value else null
}
