package dev.kesatria.gemmatch

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import dev.kesatria.gemmatch.core.Gem
import dev.kesatria.gemmatch.core.Special
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * What marks a gem as special, in the same unit space as [GemShapes]: a white vector overlay with a dark outline,
 * and a slow shimmer. Blast is an eight-point burst, a line gem a stripe along its row or column, a cross gem a
 * plus, and the colour gem a wheel of the seven gem colours with a white edge.
 */
object SpecialOverlays {
    private val burst = spikes()
    private val stripeRow = stripe(horizontal = true)
    private val stripeColumn = stripe(horizontal = false)
    private val plus = plus(PLUS_ARM, PLUS_REACH, 0f)
    private val xShort = plus(X_SHORT_ARM, X_SHORT_REACH, DIAGONAL_DEGREES)
    private val xFull = plus(PLUS_ARM, X_FULL_REACH, DIAGONAL_DEGREES)
    private val wheel = Array(Gem.entries.size) { wedge(it) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val sheen = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val outline = strokePaint(OUTLINE_WIDTH, Color.BLACK)
    private val rim = strokePaint(RIM_WIDTH, Color.WHITE)

    /** Draws the overlay of [sprite]'s special, if it has one. [clock] is the scene's running time in seconds. */
    fun draw(
        canvas: Canvas,
        sprite: Sprite,
        clock: Float,
    ) {
        when (val special = sprite.piece.special) {
            null -> Unit
            Special.COLOUR -> drawWheel(canvas, sprite.alpha)
            else -> drawMark(canvas, sprite, special, shimmer(clock))
        }
    }

    /** 0 to 1 and back, once every [SHIMMER_SECONDS]. */
    private fun shimmer(clock: Float): Float = (MIDDLE + MIDDLE * sin(2 * PI * clock / SHIMMER_SECONDS)).toFloat()

    private fun drawMark(
        canvas: Canvas,
        sprite: Sprite,
        special: Special,
        glow: Float,
    ) {
        val alpha = sprite.alpha
        val mark = if (sprite.piece.gem == Gem.WHITE) DARK_MARK else Color.WHITE
        sprite.piece.gem?.let {
            sheen.color = mark
            sheen.alpha = (alpha * glow * SHEEN_ALPHA).toInt()
            canvas.drawPath(GemShapes.pathOf(it), sheen)
        }
        val path =
            when (special) {
                Special.BLAST -> burst
                Special.LINE_ROW -> stripeRow
                Special.LINE_COL -> stripeColumn
                Special.CROSS -> plus
                Special.X_SHORT -> xShort
                Special.X_FULL -> xFull
                Special.COLOUR -> return
            }
        overlayFill.color = mark
        overlayFill.alpha = (alpha * (OVERLAY_BASE + OVERLAY_GLOW * glow) * OPAQUE).toInt()
        outline.alpha = (alpha * OUTLINE_ALPHA).toInt()
        canvas.drawPath(path, overlayFill)
        canvas.drawPath(path, outline)
    }

    private fun drawWheel(
        canvas: Canvas,
        alpha: Float,
    ) {
        fill.color = Color.WHITE
        fill.alpha = (alpha * OPAQUE).toInt()
        canvas.drawCircle(0f, 0f, WHEEL_RADIUS, fill)
        for (gem in Gem.entries) {
            fill.color = Palette.of(gem)
            fill.alpha = (alpha * OPAQUE).toInt()
            canvas.drawPath(wheel[gem.ordinal], fill)
        }
        rim.alpha = (alpha * OPAQUE).toInt()
        outline.alpha = (alpha * OUTLINE_ALPHA).toInt()
        canvas.drawCircle(0f, 0f, WHEEL_RADIUS, rim)
        canvas.drawCircle(0f, 0f, WHEEL_RADIUS + RIM_WIDTH / 2, outline)
    }

    private fun strokePaint(
        width: Float,
        colour: Int,
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeJoin = Paint.Join.ROUND
        color = colour
    }

    private fun spikes(): Path =
        Path().apply {
            for (i in 0 until BURST_POINTS * 2) {
                val radius = if (i % 2 == 0) BURST_OUTER else BURST_INNER
                val angle = -PI / 2 + i * PI / BURST_POINTS
                val x = (radius * cos(angle)).toFloat()
                val y = (radius * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

    private fun stripe(horizontal: Boolean): Path =
        Path().apply {
            val reach = STRIPE_REACH
            val half = STRIPE_HALF
            if (horizontal) {
                addRoundRect(-reach, -half, reach, half, STRIPE_CORNER, STRIPE_CORNER, Path.Direction.CW)
            } else {
                addRoundRect(-half, -reach, half, reach, STRIPE_CORNER, STRIPE_CORNER, Path.Direction.CW)
            }
        }

    /** A plus with arms [arm] wide and [reach] long, turned by [degrees]. Turned 45 degrees it is an X. */
    private fun plus(
        arm: Float,
        reach: Float,
        degrees: Float,
    ): Path =
        Path().apply {
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
            transform(Matrix().apply { setRotate(degrees) })
        }

    private fun wedge(index: Int): Path =
        Path().apply {
            val sweep = FULL_TURN_DEGREES / Gem.entries.size
            val bounds = RectF(-WHEEL_RADIUS, -WHEEL_RADIUS, WHEEL_RADIUS, WHEEL_RADIUS)
            moveTo(0f, 0f)
            arcTo(bounds, -QUARTER_TURN_DEGREES + index * sweep, sweep)
            close()
        }

    private const val MIDDLE = 0.5
    private const val DARK_MARK = 0xFF37474F.toInt()
    private const val BURST_POINTS = 8
    private const val BURST_OUTER = 0.8f
    private const val BURST_INNER = 0.4f
    private const val STRIPE_REACH = 0.85f
    private const val STRIPE_HALF = 0.17f
    private const val STRIPE_CORNER = 0.1f
    private const val PLUS_ARM = 0.17f
    private const val PLUS_REACH = 0.8f
    private const val X_SHORT_ARM = 0.13f
    private const val X_SHORT_REACH = 0.55f
    private const val X_FULL_REACH = 0.88f
    private const val DIAGONAL_DEGREES = 45f
    private const val WHEEL_RADIUS = 0.85f
    private const val RIM_WIDTH = 0.12f
    private const val OUTLINE_WIDTH = 0.07f
    private const val FULL_TURN_DEGREES = 360f
    private const val QUARTER_TURN_DEGREES = 90f
    private const val SHIMMER_SECONDS = 2.4f
    private const val SHEEN_ALPHA = 90
    private const val OVERLAY_BASE = 0.75f
    private const val OVERLAY_GLOW = 0.25f
    private const val OUTLINE_ALPHA = 200
    private const val OPAQUE = 255
}
