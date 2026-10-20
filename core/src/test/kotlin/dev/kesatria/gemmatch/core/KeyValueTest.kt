package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KeyValueTest {
    @Test
    fun pairsAreWrittenInOrderAsKeyEqualsValueSeparatedByCommas() {
        assertEquals("moves=3,gems=12", encodePairs(listOf("moves" to 3L, "gems" to 12L)))
    }

    @Test
    fun noPairsIsAnEmptyString() {
        assertEquals("", encodePairs(emptyList()))
        assertEquals(emptyMap(), decodePairs(""))
    }

    @Test
    fun pairsReadBackAsWritten() {
        val pairs = listOf("a" to 0L, "bC" to 7L, "big" to Long.MAX_VALUE)

        assertEquals(pairs.toMap(), decodePairs(encodePairs(pairs)))
    }

    @Test
    fun aMalformedPairMakesTheWholeLineUnreadable() {
        val bad =
            listOf(
                "moves",
                "moves=",
                "=3",
                "moves=-1",
                "moves=1.5",
                "moves=abc",
                "moves=3,",
                ",moves=3",
                "moves=3,,gems=1",
                "moves=3,moves=4",
                "mo ves=3",
                "moves=3=4",
                "moves=99999999999999999999",
            )
        for (text in bad) assertNull(decodePairs(text), "'$text'")
    }
}
