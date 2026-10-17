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

    /** The same game as an old `GM1` save: one gem digit per cell. The malformed-input tests below run on it. */
    private val saved = gm1(played(seed = 8L, moves = 5))

    private fun gm1(game: Game): String = listOf("GM1", game.score, game.rngState, boardDigits(game)).joinToString(";")

    private fun boardCells(game: Game): String =
        (0 until BOARD_SIZE).joinToString("") { row ->
            (0 until BOARD_SIZE).joinToString("") { col -> "${checkNotNull(game.board[Pos(row, col)]?.gem).ordinal}-" }
        }

    private fun boardDigits(game: Game): String =
        (0 until BOARD_SIZE)
            .flatMap { row -> (0 until BOARD_SIZE).map { col -> checkNotNull(game.board[Pos(row, col)]?.gem).ordinal } }
            .joinToString("")

    @Test
    fun encodedGameStartsWithTheVersionMarker() {
        assertTrue(played(8L, 5).encode().startsWith("GM2;"))
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
        assertNull(Game.decode(saved.replaceFirst("GM1", "GM3")))
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
        val biggest = gm1(game).replace(Regex(";\\d+;-?\\d+;"), ";2147483647;-9223372036854775807;")

        val loaded = assertNotNull(Game.decode(biggest))

        assertEquals(Int.MAX_VALUE, loaded.score)
        assertTrue(biggest.length < 128, "a real save is about 100 bytes, was ${biggest.length}")
    }

    @Test
    fun boardDigitsMatchTheGameForReadability() {
        val game = played(8L, 5)

        assertTrue(game.encode().endsWith(boardCells(game)))
    }

    private val everySpecialBoard =
        randomStableBoard(5L)
            .with(Pos(0, 0), Piece(Gem.RED, Special.BLAST))
            .with(Pos(0, 1), Piece(Gem.ORANGE, Special.LINE_ROW))
            .with(Pos(0, 2), Piece(Gem.YELLOW, Special.LINE_COL))
            .with(Pos(0, 3), Piece(Gem.WHITE, Special.CROSS))
            .with(Pos(0, 4), Piece(null, Special.COLOUR))
            .with(Pos(0, 5), Piece(Gem.GREEN, Special.X_SHORT))
            .with(Pos(0, 6), Piece(Gem.BLUE, Special.X_FULL))

    private val letters =
        mapOf(
            null to '-',
            Special.BLAST to 'B',
            Special.LINE_ROW to 'R',
            Special.LINE_COL to 'C',
            Special.CROSS to 'X',
            Special.X_SHORT to 'S',
            Special.X_FULL to 'F',
            Special.COLOUR to 'W',
        )

    private fun gm2(
        board: Board,
        score: Int = 120,
        rng: Long = 77L,
    ): String {
        val cells =
            (0 until BOARD_SIZE).joinToString("") { row ->
                (0 until BOARD_SIZE).joinToString("") { col ->
                    val piece = checkNotNull(board[Pos(row, col)])
                    "${piece.gem?.ordinal ?: '-'}${letters.getValue(piece.special)}"
                }
            }
        return "GM2;$score;$rng;$cells"
    }

    @Test
    fun aBoardHoldingEverySpecialRoundTripsEqual() {
        val text = gm2(everySpecialBoard)

        val game = assertNotNull(Game.decode(text))

        assertEquals(everySpecialBoard, game.board)
        assertEquals(120, game.score)
        assertEquals(77L, game.rngState)
        assertEquals(text, game.encode())
    }

    @Test
    fun aRestoredGameWithSpecialsPlaysTheSameFuture() {
        val original = assertNotNull(Game.decode(gm2(everySpecialBoard)))
        val restored = assertNotNull(Game.decode(original.encode()))
        val move = findMatchMoves(original.board).first()

        val a = original.trySwap(move.from, move.to)
        val b = restored.trySwap(move.from, move.to)

        assertIs<SwapOutcome.Accepted>(a)
        assertIs<SwapOutcome.Accepted>(b)
        assertEquals(a.steps, b.steps)
        assertEquals(a.game.encode(), b.game.encode())
    }

    @Test
    fun aGm1SaveLoadsAndIsWrittenBackAsGm2() {
        val old = played(seed = 8L, moves = 5)

        val loaded = assertNotNull(Game.decode(gm1(old)))

        assertEquals(old.board, loaded.board)
        assertEquals(old.score, loaded.score)
        assertEquals(old.rngState, loaded.rngState)
        assertTrue(loaded.encode().startsWith("GM2;"))
        assertEquals(old.encode(), loaded.encode())
    }

    @Test
    fun malformedGm2CellsReturnNull() {
        val valid = gm2(everySpecialBoard)
        val head = valid.substringBeforeLast(';') + ";"
        val cells = valid.substringAfterLast(';')

        fun withFirstCell(cell: String) = head + cell + cells.drop(2)

        assertNotNull(Game.decode(withFirstCell("0B")))
        assertNull(Game.decode(withFirstCell("0Z")), "unknown special letter")
        assertNull(Game.decode(withFirstCell("-B")), "no gem on a non-colour special")
        assertNull(Game.decode(withFirstCell("--")), "an empty cell")
        assertNull(Game.decode(withFirstCell("0W")), "a colour gem with a gem")
        assertNull(Game.decode(withFirstCell("7-")), "gem digit out of range")
        assertNull(Game.decode(withFirstCell("x-")), "gem digit not a digit")
        assertNull(Game.decode(withFirstCell("b-")), "lower case letters are not specials")
        assertNull(Game.decode(head + cells.dropLast(1)), "one character short")
        assertNull(Game.decode(head + cells + "0-"), "one cell long")
        assertNull(Game.decode(head + cells.take(CELL_CHARS - 1)), "truncated")
    }

    @Test
    fun everyTruncationOfAGm2SaveReturnsNull() {
        val text = gm2(everySpecialBoard)
        for (length in text.indices) {
            assertNull(Game.decode(text.substring(0, length)), "prefix length $length")
        }
    }

    @Test
    fun theWorstCaseGm2SaveStaysUnderTheReadLimit() {
        val worst = gm2(everySpecialBoard, score = Int.MAX_VALUE, rng = Long.MIN_VALUE)

        assertNotNull(Game.decode(worst))
        assertTrue(worst.length < 256, "was ${worst.length}")
    }

    private companion object {
        const val CELL_CHARS = BOARD_SIZE * BOARD_SIZE * 2
    }
}
