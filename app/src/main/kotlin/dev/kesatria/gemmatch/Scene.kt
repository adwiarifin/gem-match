package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.BOARD_SIZE
import dev.kesatria.gemmatch.core.Board
import dev.kesatria.gemmatch.core.Gem
import dev.kesatria.gemmatch.core.Pos

/** One gem as drawn: where it is (in cell units, fractional while moving), and how it looks. */
class Sprite(
    val gem: Gem,
    var row: Float,
    var col: Float,
) {
    var scale = 1f
    var alpha = 1f
}

/**
 * What is on screen right now: one sprite per board slot. Animations move and fade sprites here
 * while the real game state is already resolved. Only the game-loop thread touches it.
 */
class Scene {
    private val slots = arrayOfNulls<Sprite>(BOARD_SIZE * BOARD_SIZE)

    /** Resets every sprite to rest on its cell of [board]. */
    fun load(board: Board) {
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                slots[indexOf(Pos(row, col))] = board[Pos(row, col)]?.let { Sprite(it, row.toFloat(), col.toFloat()) }
            }
        }
    }

    operator fun get(pos: Pos): Sprite? = slots[indexOf(pos)]

    fun remove(pos: Pos): Sprite? {
        val sprite = slots[indexOf(pos)]
        slots[indexOf(pos)] = null
        return sprite
    }

    fun place(
        pos: Pos,
        sprite: Sprite,
    ) {
        slots[indexOf(pos)] = sprite
    }

    fun swapSlots(
        a: Pos,
        b: Pos,
    ) {
        val held = slots[indexOf(a)]
        slots[indexOf(a)] = slots[indexOf(b)]
        slots[indexOf(b)] = held
    }

    fun forEach(action: (Sprite) -> Unit) {
        for (sprite in slots) if (sprite != null) action(sprite)
    }

    private fun indexOf(pos: Pos): Int = pos.row * BOARD_SIZE + pos.col
}
