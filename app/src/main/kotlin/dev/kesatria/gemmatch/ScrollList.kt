package dev.kesatria.gemmatch

/** How far a list of rows is scrolled, in pixels, kept between the top and the point where its last row shows. */
class ScrollList {
    var offset = 0f
        private set

    private var maxOffset = 0f

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
}
