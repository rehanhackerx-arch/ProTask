package com.example.myempty.protask

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class SpinWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(168, 85, 247)
        style = Paint.Style.STROKE
        strokeWidth = 10f
        setShadowLayer(22f, 0f, 0f, Color.rgb(168, 85, 247))
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 32f
        isFakeBoldText = true
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(168, 85, 247)
        style = Paint.Style.FILL
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(45, 212, 191)
        style = Paint.Style.FILL
    }
    private val wheelBounds = RectF()
    private var rotationAngle = 0f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height).toFloat()
        val radius = size / 2f - 30f
        val cx = width / 2f
        val cy = height / 2f
        wheelBounds.set(cx - radius, cy - radius, cx + radius, cy + radius)

        val sweep = 360f / POINTS.size
        POINTS.forEachIndexed { index, points ->
            slicePaint.shader = LinearGradient(
                wheelBounds.left,
                wheelBounds.top,
                wheelBounds.right,
                wheelBounds.bottom,
                SLICE_COLORS[index % SLICE_COLORS.size],
                SLICE_COLORS_ALT[index % SLICE_COLORS_ALT.size],
                Shader.TileMode.CLAMP
            )
            val startAngle = -90f + rotationAngle + index * sweep
            canvas.drawArc(wheelBounds, startAngle, sweep, true, slicePaint)
            slicePaint.shader = null
            drawSliceText(canvas, "$points", startAngle + sweep / 2f, cx, cy, radius)
        }

        canvas.drawCircle(cx, cy, radius - 4f, rimPaint)
        canvas.drawCircle(cx, cy, 28f, centerPaint)
        drawTopIndicator(canvas, cx, cy - radius)
    }

    fun spinToSlice(targetIndex: Int, onFinished: () -> Unit) {
        val sweep = 360f / POINTS.size
        val targetCenterOffset = targetIndex * sweep + sweep / 2f
        var targetRotation = -targetCenterOffset

        while (targetRotation <= rotationAngle + 1440f) {
            targetRotation += 360f
        }

        ValueAnimator.ofFloat(rotationAngle, targetRotation).apply {
            duration = 3600L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                rotationAngle = it.animatedValue as Float
                invalidate()
            }
            doOnEndCompat { onFinished() }
            start()
        }
    }

    private fun drawSliceText(
        canvas: Canvas,
        text: String,
        angleDegrees: Float,
        cx: Float,
        cy: Float,
        radius: Float
    ) {
        val angle = Math.toRadians(angleDegrees.toDouble())
        val textRadius = radius * 0.62f
        val x = cx + (cos(angle) * textRadius).toFloat()
        val y = cy + (sin(angle) * textRadius).toFloat() + textPaint.textSize / 3f
        canvas.drawText(text, x, y, textPaint)
    }

    private fun drawTopIndicator(canvas: Canvas, centerX: Float, wheelTop: Float) {
        val path = Path().apply {
            moveTo(centerX - 22f, wheelTop - 12f)
            lineTo(centerX + 22f, wheelTop - 12f)
            lineTo(centerX, wheelTop + 32f)
            close()
        }
        canvas.drawPath(path, arrowPaint)
    }

    private fun ValueAnimator.doOnEndCompat(action: () -> Unit) {
        addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) = Unit
            override fun onAnimationEnd(animation: android.animation.Animator) = action()
            override fun onAnimationCancel(animation: android.animation.Animator) = Unit
            override fun onAnimationRepeat(animation: android.animation.Animator) = Unit
        })
    }

    companion object {
        val POINTS = intArrayOf(5, 10, 20, 30, 50, 75, 100, 150)
        private val SLICE_COLORS = intArrayOf(
            Color.rgb(123, 44, 255),
            Color.rgb(91, 140, 255),
            Color.rgb(168, 85, 247),
            Color.rgb(45, 212, 191),
            Color.rgb(88, 28, 135),
            Color.rgb(72, 52, 212),
            Color.rgb(34, 211, 238),
            Color.rgb(107, 70, 193)
        )
        private val SLICE_COLORS_ALT = intArrayOf(
            Color.rgb(168, 85, 247),
            Color.rgb(45, 212, 191),
            Color.rgb(91, 140, 255),
            Color.rgb(123, 44, 255),
            Color.rgb(34, 211, 238),
            Color.rgb(168, 85, 247),
            Color.rgb(88, 28, 135),
            Color.rgb(45, 212, 191)
        )
    }
}
