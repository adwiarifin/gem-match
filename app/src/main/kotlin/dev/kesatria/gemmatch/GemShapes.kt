package dev.kesatria.gemmatch

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import dev.kesatria.gemmatch.core.Gem
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Each gem has its own colour and its own silhouette, so the seven are told apart without colour.
 * Shapes are vector paths in a unit space (centre 0,0, radius about 1) scaled at draw time: no assets.
 */
object GemShapes {
    private val paths: Array<Path> = Array(Gem.entries.size) { buildPath(Gem.entries[it]) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outline =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = OUTLINE_WIDTH
            strokeJoin = Paint.Join.ROUND
            color = Color.BLACK
        }

    /** Draws [sprite]'s gem centred at ([cx], [cy]) fitting a circle of [radius] pixels, at the sprite's alpha. */
    fun draw(
        canvas: Canvas,
        sprite: Sprite,
        cx: Float,
        cy: Float,
        radius: Float,
    ) {
        val gem = sprite.gem
        fill.color = Palette.of(gem)
        fill.alpha = (sprite.alpha * OPAQUE).toInt()
        outline.alpha = (sprite.alpha * OUTLINE_ALPHA).toInt()
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(radius, radius)
        canvas.drawPath(paths[gem.ordinal], fill)
        canvas.drawPath(paths[gem.ordinal], outline)
        canvas.restore()
    }

    private fun buildPath(gem: Gem): Path =
        when (gem) {
            Gem.RED -> Path().apply { addCircle(0f, 0f, CIRCLE_RADIUS, Path.Direction.CW) }
            Gem.ORANGE -> polygon(sides = 4, radius = DIAMOND_RADIUS, startAngle = -HALF_TURN / 2)
            Gem.YELLOW -> roundedSquare()
            Gem.GREEN -> polygon(sides = 6, radius = HEXAGON_RADIUS, startAngle = 0.0)
            Gem.BLUE ->
                polygon(sides = 3, radius = TRIANGLE_RADIUS, startAngle = -HALF_TURN / 2, centreY = TRIANGLE_DROP)
            Gem.PURPLE -> star()
            Gem.WHITE -> cross()
        }

    private fun roundedSquare(): Path =
        Path().apply {
            val half = SQUARE_HALF
            addRoundRect(-half, -half, half, half, SQUARE_CORNER, SQUARE_CORNER, Path.Direction.CW)
        }

    private fun polygon(
        sides: Int,
        radius: Float,
        startAngle: Double,
        centreY: Float = 0f,
    ): Path =
        Path().apply {
            for (i in 0 until sides) {
                val angle = startAngle + i * 2 * HALF_TURN / sides
                val x = (radius * cos(angle)).toFloat()
                val y = (radius * sin(angle)).toFloat() + centreY
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

    private fun star(): Path =
        Path().apply {
            val points = STAR_POINTS * 2
            for (i in 0 until points) {
                val radius = if (i % 2 == 0) STAR_OUTER else STAR_INNER
                val angle = -HALF_TURN / 2 + i * HALF_TURN / STAR_POINTS
                val x = (radius * cos(angle)).toFloat()
                val y = (radius * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

    private fun cross(): Path =
        Path().apply {
            val arm = CROSS_ARM
            val reach = CROSS_REACH
            moveTo(-arm, -reach)
            lineTo(arm, -reach)
            lineTo(arm, -arm)
            lineTo(reach, -arm)
            lineTo(reach, arm)
            lineTo(arm, arm)
            lineTo(arm, reach)
            lineTo(-arm, reach)
            lineTo(-arm, arm)
            lineTo(-reach, arm)
            lineTo(-reach, -arm)
            lineTo(-arm, -arm)
            close()
        }

    private const val HALF_TURN = PI
    private const val CIRCLE_RADIUS = 0.9f
    private const val DIAMOND_RADIUS = 1.0f
    private const val SQUARE_HALF = 0.78f
    private const val SQUARE_CORNER = 0.2f
    private const val HEXAGON_RADIUS = 0.95f
    private const val TRIANGLE_RADIUS = 1.05f
    private const val TRIANGLE_DROP = 0.18f
    private const val STAR_POINTS = 5
    private const val STAR_OUTER = 1.05f
    private const val STAR_INNER = 0.5f
    private const val CROSS_ARM = 0.34f
    private const val CROSS_REACH = 0.95f
    private const val OUTLINE_WIDTH = 0.07f
    private const val OUTLINE_ALPHA = 90
    private const val OPAQUE = 255
}
