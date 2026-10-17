package dev.kesatria.gemmatch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveCodecTest {
    private fun played(
        seed: Long,
        moves: Int,
    ): Game {
        var game = Game.new(seed)
        repeat(moves) {
            val move = findValidMoves(game.board).first()
            game = (game.trySwap(move.from, move.to) as SwapOutcome.Accepted).game
        }
        return game
    }

    private val saved = played(seed = 8L, moves = 5).encode()

    private fun boardDigits(game: Game): String =
        (0 until BOARD_SIZE)
            .flatMap { row -> (0 until BOARD_SIZE).map { col -> checkNotNull(game.board[Pos(row, col)]?.gem).ordinal } }
            .joinToString("")

    @Test
    fun encodedGameStartsWithTheVersionMarker() {
        assertTrue(saved.startsWith("GM1;"))
    }

    @Test
    fun encodingIsDeterministic() {
        assertEquals(played(8L, 5).encode(), played(8L, 5).encode())
    }

    @Test
    fun roundTripKeepsBoardScoreAndRandomState() {
        for (seed in 1L..50L) {
            val game = played(seed, moves = 8)

            val restored = Game.decode(game.encode())

            assertNotNull(restored, "seed $seed")
            assertEquals(game.board, restored.board, "seed $seed")
            assertEquals(game.score, restored.score, "seed $seed")
            assertEquals(game.rngState, restored.rngState, "seed $seed")
        }
    }

    @Test
    fun restoredGameContinuesWithTheSameFuture() {
        val original = played(seed = 12L, moves = 6)
        val restored = checkNotNull(Game.decode(original.encode()))
        val move = findValidMoves(original.board).first()

        val a = original.trySwap(move.from, move.to)
        val b = restored.trySwap(move.from, move.to)

        assertIs<SwapOutcome.Accepted>(a)
        assertIs<SwapOutcome.Accepted>(b)
        assertEquals(a.steps, b.steps)
        assertEquals(a.game.board, b.game.board)
        assertEquals(a.game.score, b.game.score)
    }

    @Test
    fun trailingNewlineIsTolerated() {
        assertNotNull(Game.decode(saved + "\n"))
    }

    @Test
    fun emptyAndBlankTextReturnNull() {
        assertNull(Game.decode(""))
        assertNull(Game.decode("   "))
    }

    @Test
    fun everyTruncationOfAValidSaveReturnsNull() {
        for (length in saved.indices) {
            assertNull(Game.decode(saved.substring(0, length)), "prefix length $length")
        }
    }

    @Test
    fun wrongVersionReturnsNull() {
        assertNull(Game.decode(saved.replaceFirst("GM1", "GM2")))
    }

    @Test
    fun garbageReturnsNull() {
        assertNull(Game.decode("hello"))
        assertNull(Game.decode("GM1;;;"))
        assertNull(Game.decode("\u0000\u0001\u0002"))
    }

    @Test
    fun malformedFieldsReturnNull() {
        val parts = saved.split(";")
        val (version, score, rng) = parts
        val board = parts[3]

        assertNull(Game.decode("$version;abc;$rng;$board"))
        assertNull(Game.decode("$version;-5;$rng;$board"))
        assertNull(Game.decode("$version;99999999999;$rng;$board"))
        assertNull(Game.decode("$version;$score;notanumber;$board"))
        assertNull(Game.decode("$version;$score;$rng;${board.replaceRange(0, 1, "x")}"))
        assertNull(Game.decode("$version;$score;$rng;${board.replaceRange(0, 1, "7")}"))
        assertNull(Game.decode("$version;$score;$rng;${board}0"))
        assertNull(Game.decode("$saved;extra"))
    }

    @Test
    fun boardWithARunReturnsNull() {
        val (version, score, rng) = saved.split(";")

        assertNull(Game.decode("$version;$score;$rng;${"0".repeat(BOARD_SIZE * BOARD_SIZE)}"))
    }

    @Test
    fun boardWithNoValidMoveReturnsNull() {
        val (version, score, rng) = saved.split(";")
        val deadDiagonal =
            (0 until BOARD_SIZE).joinToString("") { row ->
                (0 until BOARD_SIZE).joinToString("") { col -> ((row + col) % 3).toString() }
            }

        assertNull(Game.decode("$version;$score;$rng;$deadDiagonal"))
    }

    @Test
    fun anOversizedSaveIsRejectedWithoutBuildingAHugeBoard() {
        val valid = Game.new(3L).encode()
        val hugeBoard = valid.substringBeforeLast(';') + ";" + "1".repeat(5_000_000)

        assertNull(Game.decode(hugeBoard))
        assertNull(Game.decode(valid + " ".repeat(10_000)))
    }

    @Test
    fun aSaveAtTheLargestRealisticSizeStillLoads() {
        val game = Game.new(3L)
        val biggest = game.encode().replace(Regex(";\\d+;-?\\d+;"), ";2147483647;-9223372036854775807;")

        val loaded = assertNotNull(Game.decode(biggest))

        assertEquals(Int.MAX_VALUE, loaded.score)
        assertTrue(biggest.length < 128, "a real save is about 100 bytes, was ${biggest.length}")
    }

    @Test
    fun boardDigitsMatchTheGameForReadability() {
        val game = played(8L, 5)

        assertTrue(game.encode().endsWith(boardDigits(game)))
    }
}
