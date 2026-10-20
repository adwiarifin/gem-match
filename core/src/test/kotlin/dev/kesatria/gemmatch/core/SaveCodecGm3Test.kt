package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The `GM3` save: `GM2` plus the run's stats, start time and active time as key=value pairs, ending in `end=1`. */
class SaveCodecGm3Test {
    private fun played(
        seed: Long,
        moves: Int,
    ): Game {
        var game = Game.new(seed, startedMs = STARTED)
        repeat(moves) {
            val move = findValidMoves(game.board).first()
            game = (game.trySwap(move.from, move.to) as SwapOutcome.Accepted).game
        }
        return game
    }

    private fun withStats(
        save: String,
        pairs: String,
    ) = save.substringBeforeLast(';') + ";" + pairs

    private val sample = played(8L, 6).withActiveMs(ACTIVE).encode()

    @Test
    fun aGameIsWrittenAsGm3AndTheEncodingIsDeterministic() {
        assertTrue(sample.startsWith("GM3;"), sample)
        assertEquals(sample, played(8L, 6).withActiveMs(ACTIVE).encode())
    }

    @Test
    fun aFreshGameWritesNoPairsButTheEndMarker() {
        assertTrue(Game.new(5L).encode().endsWith(";end=1"), Game.new(5L).encode())
    }

    @Test
    fun theStatsAndTheClockSurviveASaveAndLoad() {
        val game = played(8L, 6).withActiveMs(ACTIVE)

        val restored = assertNotNull(Game.decode(game.encode()))

        assertEquals(game.stats, restored.stats)
        assertEquals(game.time, restored.time)
        assertEquals(RunTime(STARTED, ACTIVE), restored.time)
        assertEquals(game.board, restored.board)
        assertEquals(game.score, restored.score)
        assertEquals(game.rngState, restored.rngState)
        assertTrue(restored.stats.moves > 0, "the fixture must have played")
    }

    @Test
    fun aLoadedGamePlaysTheSameFutureAsTheOriginal() {
        val game = played(11L, 5).withActiveMs(ACTIVE)
        val restored = assertNotNull(Game.decode(game.encode()))
        val move = findValidMoves(game.board).first()

        val a = game.trySwap(move.from, move.to) as SwapOutcome.Accepted
        val b = restored.trySwap(move.from, move.to) as SwapOutcome.Accepted

        assertEquals(a.steps, b.steps)
        assertEquals(a.game.encode(), b.game.encode())
    }

    @Test
    fun everyKindOfSpecialRoundTripsInTheCreatedAndFiredCounts() {
        val kinds = "BRCXSFW"
        val created = kinds.mapIndexed { i, k -> "c$k=${i + 1}" }
        val fired = kinds.mapIndexed { i, k -> "t$k=${i + 10}" }
        val pairs = (created + fired + "end=1").joinToString(",")

        val game = assertNotNull(Game.decode(withStats(sample, pairs)))

        assertEquals(Special.entries.size, game.stats.created.size)
        assertEquals(Special.entries.size, game.stats.fired.size)
        assertEquals(1, game.stats.created[Special.BLAST])
        assertEquals(7, game.stats.created[Special.COLOUR])
        assertEquals(10, game.stats.fired[Special.BLAST])
        assertEquals(16, game.stats.fired[Special.COLOUR])
        assertEquals(game.stats, assertNotNull(Game.decode(game.encode())).stats)
    }

    @Test
    fun aMissingKeyReadsZeroAndAnUnknownKeyIsIgnored() {
        val onlyEnd = assertNotNull(Game.decode(withStats(sample, "end=1")))
        val extra = assertNotNull(Game.decode(withStats(sample, "moves=4,futureStat=9,end=1")))

        assertEquals(RunStats.EMPTY, onlyEnd.stats)
        assertEquals(RunTime(0L, 0L), onlyEnd.time)
        assertEquals(4, extra.stats.moves)
        assertEquals(0, extra.stats.gems)
    }

    @Test
    fun aBadStatsPartMakesTheWholeSaveUnreadable() {
        val bad =
            listOf(
                "moves=-1,end=1",
                "moves=abc,end=1",
                "moves=1,moves=2,end=1",
                "moves=99999999999,end=1",
                "=3,end=1",
                "moves=1,,end=1",
                "end=2",
                "end=0",
                "moves=1",
                "",
            )
        for (pairs in bad) assertNull(Game.decode(withStats(sample, pairs)), "'$pairs'")
    }

    @Test
    fun everyTruncationOfAGm3SaveReturnsNull() {
        for (length in sample.indices) {
            assertNull(Game.decode(sample.substring(0, length)), "prefix length $length")
        }
    }

    @Test
    fun aGm3SaveNeedsExactlyFiveParts() {
        assertNull(Game.decode(sample.substringBeforeLast(';')), "four parts")
        assertNull(Game.decode("$sample;extra"), "six parts")
    }

    @Test
    fun anOlderSaveLoadsWithZeroStatsAndStartsTheClockAtTheTimeItWasLoaded() {
        val gm2 = "GM2;" + sample.split(";").subList(1, 4).joinToString(";")
        val gm1 = "GM1;" + Game.new(8L).let { "${it.score};${it.rngState};" } + gm1Digits(Game.new(8L))

        for (old in listOf(gm2, gm1)) {
            val game = assertNotNull(Game.decode(old, nowMs = LOADED), old.take(4))

            assertEquals(RunStats.EMPTY, game.stats, old.take(4))
            assertEquals(RunTime(LOADED, 0L), game.time, old.take(4))
        }
    }

    @Test
    fun theWorstCaseGm3SaveStaysUnderItsReadLimitAndIsAccepted() {
        val keys =
            listOf("moves", "cascade", "moveScore", "gems", "match") +
                "BRCXSFW".flatMap { listOf("c$it", "t$it") }
        val stats = keys.joinToString(",") { "$it=${Int.MAX_VALUE}" }
        val worst = withStats(sample, "$stats,started=${Long.MAX_VALUE},active=${Long.MAX_VALUE},end=1")

        assertNotNull(Game.decode(worst))
        assertTrue(worst.length < GM3_LIMIT, "was ${worst.length}")
    }

    @Test
    fun aGm3SaveIsAcceptedUpToItsLimitAndRejectedOneCharacterOver() {
        assertNotNull(Game.decode(sample.padEnd(GM3_LIMIT)))
        assertNull(Game.decode(sample.padEnd(GM3_LIMIT + 1)))
    }

    private fun gm1Digits(game: Game): String =
        (0 until BOARD_SIZE)
            .flatMap { row -> (0 until BOARD_SIZE).map { col -> checkNotNull(game.board[Pos(row, col)]?.gem).ordinal } }
            .joinToString("")

    private companion object {
        const val STARTED = 1_700_000_000_000L
        const val ACTIVE = 654_321L
        const val LOADED = 1_800_000_000_000L
        const val GM3_LIMIT = 1024
    }
}
