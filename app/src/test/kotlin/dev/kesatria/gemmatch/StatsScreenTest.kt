package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BoardLayout
import dev.kesatria.gemmatch.core.FinishedRun
import dev.kesatria.gemmatch.core.Lifetime
import dev.kesatria.gemmatch.core.RunStats
import dev.kesatria.gemmatch.core.Special
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatsScreenTest {
    private fun run(
        score: Int,
        started: Long = score.toLong(),
        moves: Int = 40,
        active: Long = 605_000,
    ) = FinishedRun(
        score,
        RunStats(moves, 3, 200, 100, 4, mapOf(Special.BLAST to 1), mapOf(Special.BLAST to 2)),
        started,
        started + 700_000,
        700_000,
        active,
        newBest = false,
    )

    private fun lifetimeOf(vararg runs: FinishedRun) = runs.fold(Lifetime.EMPTY) { total, run -> total + run }

    private fun lines(items: List<StatsItem>): Map<String, String> {
        val all = items.filterIsInstance<StatsItem.Line>()
        return all.associate { it.label to it.value }
    }

    private fun headings(items: List<StatsItem>) = items.filterIsInstance<StatsItem.Heading>().map { it.text }

    @Test
    fun theLifetimeBlockShowsRecordsTotalsAndAverages() {
        val runs = listOf(run(3_000, moves = 30), run(1_000, moves = 10))
        val items = statsItems(lifetimeOf(runs[1], runs[0]), runs)

        val lines = lines(items)

        assertEquals(listOf("Records", "Totals", "Averages", "Last runs"), headings(items))
        assertEquals("3,000", lines["Best score"])
        assertEquals("3", lines["Longest cascade"])
        assertEquals("200", lines["Biggest move score"])
        assertEquals("4", lines["Biggest match"])
        assertEquals("11:40", lines["Longest run (total time)"])
        assertEquals("10:05", lines["Longest run (active time)"])
        assertEquals("2", lines["Runs played"])
        assertEquals("40", lines["Moves"])
        assertEquals("200", lines["Gems cleared"])
        assertEquals("2", lines["Special gems made"])
        assertEquals("4", lines["Special gems fired"])
        assertEquals("20:10", lines["Active time"])
        assertEquals("23:20", lines["Total time"])
        assertEquals("2,000", lines["Score per run"])
        assertEquals("100", lines["Score per move"])
        assertEquals("20", lines["Moves per run"])
    }

    @Test
    fun theLastRunsFollowNewestFirstWithScoreMovesAndActiveTime() {
        val runs = listOf(run(3_000, moves = 30), run(1_000, moves = 10))

        val rows = statsItems(lifetimeOf(runs[1], runs[0]), runs).filterIsInstance<StatsItem.Line>().takeLast(2)

        assertEquals(StatsItem.Line("3,000", "30 moves · 10:05"), rows[0])
        assertEquals(StatsItem.Line("1,000", "10 moves · 10:05"), rows[1])
    }

    @Test
    fun aHundredRunsAreAHundredRowsAndTheLifetimeStillCountsEveryRun() {
        val list = (101 downTo 2).map { run(it) }
        val lifetime = lifetimeOf(run(9_999, started = 1L), *list.reversed().toTypedArray())

        val items = statsItems(lifetime, list)

        assertEquals(100, items.takeLastWhile { it is StatsItem.Line }.size)
        assertEquals("101", lines(items)["Runs played"])
        assertEquals("9,999", lines(items)["Best score"])
    }

    @Test
    fun withNothingRecordedTheScreenSaysSoPlainly() {
        val items = statsItems(Lifetime.EMPTY, emptyList())

        assertTrue(items.any { it == StatsItem.Message("No finished runs yet") })
        assertFalse(headings(items).contains("Last runs"))
    }

    @Test
    fun anOldBestScoreStillShowsBeforeTheFirstRunIsFinished() {
        val items = statsItems(Lifetime.EMPTY.copy(bestScore = 5_000), emptyList())

        assertEquals("5,000", lines(items)["Best score"])
        assertTrue(items.any { it is StatsItem.Message })
    }

    @Test
    fun aLifetimeWithAnEmptyListSaysTheListIsEmptyButKeepsTheTotals() {
        val items = statsItems(lifetimeOf(run(500)), emptyList())

        assertEquals("1", lines(items)["Runs played"])
        assertTrue(items.any { it == StatsItem.Message("No runs in the list") })
    }

    @Test
    fun averagesAreSafeWithNoRunsOrNoMoves() {
        val lines = lines(statsItems(Lifetime.EMPTY.copy(runs = 1), emptyList()))

        assertEquals("0", lines["Score per run"])
        assertEquals("0", lines["Score per move"])
        assertEquals("0", lines["Moves per run"])
    }

    @Test
    fun theLayoutKeepsTheCloseButtonPinnedUnderTheList() {
        val layout = BoardLayout(40f, 400f, 100f)
        val geometry = StatsLayout(layout)

        assertTrue(geometry.close.top >= geometry.list.bottom)
        assertTrue(geometry.close.bottom <= geometry.panel.bottom)
        assertTrue(geometry.list.top >= geometry.panel.top)
        assertTrue(geometry.list.height >= geometry.rowHeight * 8, "at least eight rows show at once")
        assertTrue(geometry.panel.width <= layout.cellSize * 8)
        assertTrue(geometry.isOnClose(geometry.close.centerX, geometry.close.centerY))
        assertFalse(geometry.isOnClose(geometry.list.centerX, geometry.list.centerY))
    }

    @Test
    fun aClosedScreenTakesNoTouchesAndAnOpenOneClosesOnTheCloseButton() {
        val layout = BoardLayout(40f, 400f, 100f)
        val geometry = StatsLayout(layout)
        val screen = StatsScreen()
        assertFalse(screen.isOpen)

        screen.open(emptyHistory())
        assertTrue(screen.isOpen)
        assertNull(screen.onTouch(GameController.ACTION_DOWN, geometry.close.centerX, geometry.close.centerY, layout))
        assertEquals(
            StatsAction.CLOSE,
            screen.onTouch(GameController.ACTION_UP, geometry.close.centerX, geometry.close.centerY, layout),
        )
    }

    @Test
    fun draggingTheListNeverClosesTheScreen() {
        val layout = BoardLayout(40f, 400f, 100f)
        val geometry = StatsLayout(layout)
        val screen = StatsScreen()
        screen.open(emptyHistory())

        screen.onTouch(GameController.ACTION_DOWN, geometry.close.centerX, geometry.close.centerY, layout)
        screen.onTouch(GameController.ACTION_MOVE, geometry.close.centerX, geometry.close.centerY - 300f, layout)

        val liftedAt = geometry.close.centerY - 300f
        assertNull(screen.onTouch(GameController.ACTION_UP, geometry.close.centerX, liftedAt, layout))
    }

    private fun emptyHistory() =
        object : RunHistory {
            override fun lifetime() = Lifetime.EMPTY

            override fun runs() = emptyList<FinishedRun>()

            override fun record(run: FinishedRun) = false
        }
}
