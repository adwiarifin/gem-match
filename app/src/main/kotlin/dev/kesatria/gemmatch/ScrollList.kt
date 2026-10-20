package dev.kesatria.gemmatch

/** How far a list of rows is scrolled, in pixels, kept between the top and the point where its last row shows. */
class ScrollList {
    var offset = 0f
        private set

    private var maxOffset = 0f
    private var startY = 0f
    private var lastY = 0f

    /** True once the finger that is down has moved further than the slop, so lifting it is not a tap. */
    var dragged = false
        private set

    val canScroll: Boolean get() = maxOffset > 0f

    /** Tells the list how tall its content and its window are; pulls the offset back in range if they shrank. */
    fun fit(
        contentHeight: Float,
        viewHeight: Float,
    ) {
        maxOffset = maxOf(0f, contentHeight - viewHeight)
        offset = offset.coerceIn(0f, maxOffset)
    }

    /** Moves the content up by [dy] (a finger dragging up scrolls down), stopping at both ends. */
    fun scrollBy(dy: Float) {
        offset = (offset + dy).coerceIn(0f, maxOffset)
    }

    fun reset() {
        offset = 0f
    }

    /** Follows one touch event: a finger moving up scrolls down. [slop] is how far it may wander and still be a tap. */
    fun touch(
        action: Int,
        y: Float,
        slop: Float,
    ) {
        when (action) {
            GameController.ACTION_DOWN -> {
                startY = y
                lastY = y
                dragged = false
            }

            GameController.ACTION_MOVE -> {
                if (kotlin.math.abs(y - startY) > slop) dragged = true
                scrollBy(lastY - y)
                lastY = y
            }
        }
    }
}
