package dev.kesatria.gemmatch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScrollListTest {
    @Test
    fun aListThatFitsDoesNotScroll() {
        val list = ScrollList()
        list.fit(contentHeight = 300f, viewHeight = 400f)

        list.scrollBy(50f)

        assertEquals(0f, list.offset)
        assertFalse(list.canScroll)
    }

    @Test
    fun aLongListScrollsDownToItsEndAndNoFurther() {
        val list = ScrollList()
        list.fit(contentHeight = 1_000f, viewHeight = 400f)
        assertTrue(list.canScroll)

        list.scrollBy(250f)
        assertEquals(250f, list.offset)
        list.scrollBy(10_000f)
        assertEquals(600f, list.offset)
    }

    @Test
    fun scrollingBackUpStopsAtTheTop() {
        val list = ScrollList()
        list.fit(1_000f, 400f)
        list.scrollBy(300f)

        list.scrollBy(-100f)
        assertEquals(200f, list.offset)
        list.scrollBy(-10_000f)
        assertEquals(0f, list.offset)
    }

    @Test
    fun shrinkingTheContentPullsTheOffsetBackInRange() {
        val list = ScrollList()
        list.fit(1_000f, 400f)
        list.scrollBy(600f)

        list.fit(contentHeight = 700f, viewHeight = 400f)

        assertEquals(300f, list.offset)
        list.fit(contentHeight = 200f, viewHeight = 400f)
        assertEquals(0f, list.offset)
    }

    @Test
    fun resetGoesBackToTheTop() {
        val list = ScrollList()
        list.fit(1_000f, 400f)
        list.scrollBy(500f)

        list.reset()

        assertEquals(0f, list.offset)
    }

    @Test
    fun aFingerMovingUpScrollsTheListDownAndCountsAsADrag() {
        val list = ScrollList()
        list.fit(1_000f, 400f)

        list.touch(GameController.ACTION_DOWN, y = 500f, slop = 10f)
        list.touch(GameController.ACTION_MOVE, y = 450f, slop = 10f)
        list.touch(GameController.ACTION_MOVE, y = 400f, slop = 10f)

        assertEquals(100f, list.offset)
        assertTrue(list.dragged)
    }

    @Test
    fun aFingerThatStaysWithinTheSlopIsStillATap() {
        val list = ScrollList()
        list.fit(1_000f, 400f)

        list.touch(GameController.ACTION_DOWN, y = 500f, slop = 10f)
        list.touch(GameController.ACTION_MOVE, y = 496f, slop = 10f)

        assertFalse(list.dragged)
        list.touch(GameController.ACTION_DOWN, y = 300f, slop = 10f)
        assertFalse(list.dragged, "a new touch starts clean")
    }
}
