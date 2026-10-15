package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.Gem

/** Every colour the game draws with, as ARGB ints. */
object Palette {
    const val BACKGROUND = 0xFF181830.toInt()
    const val PANEL = 0xFF242448.toInt()
    const val LABEL_TEXT = 0xFF9696BE.toInt()
    const val BODY_TEXT = 0xFFBEBEDC.toInt()
    const val NOTICE_FILL = 0xFF141428.toInt()

    fun of(gem: Gem): Int =
        when (gem) {
            Gem.RED -> 0xFFE53935.toInt()
            Gem.ORANGE -> 0xFFFB8C00.toInt()
            Gem.YELLOW -> 0xFFFDD835.toInt()
            Gem.GREEN -> 0xFF43A047.toInt()
            Gem.BLUE -> 0xFF1E88E5.toInt()
            Gem.PURPLE -> 0xFF8E24AA.toInt()
            Gem.WHITE -> 0xFFECEFF1.toInt()
        }
}
