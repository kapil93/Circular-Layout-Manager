package com.kapil.circularlayoutmanager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import kotlin.math.min

/**
 * This view implements scroll wheel functionality.
 *
 * It converts circular touch motion into input for the scrolling of a recycler view.
 *
 * Touch Area of the scroll wheel is defined as the region between the circles formed by the outer
 * and inner radii. Circular scrolling could be initiated only from this region.
 */
class ScrollWheel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), GestureDetector.OnGestureListener {

    /**
     * Function to show or hide path of action of the scroll wheel. Enabling it will highlight an
     * area on the screen as a cue for the user to use the scroll wheel.
     *
     * By default set to true.
     */
    var isHighlightTouchAreaEnabled = true
        set(value) {
            field = value
            invalidate()
        }

    /**
     * Toggle to enable or disable detection of clicks and long clicks.
     *
     * It could be set to true if the touch area lies over the list partially or fully, and to false
     * otherwise.
     *
     * By default set to true.
     */
    var isHandleClicksEnabled: Boolean = true

    @Px
    var touchAreaThickness =
        context.resources.getDimension(R.dimen.default_touch_area_thickness)
        set(value) {
            field = value
            invalidate()
        }

    var onItemClickListener: ((Float, Float) -> Unit)? = null
    var onItemLongClickListener: ((Float, Float) -> Unit)? = null

    /**
     * The scroll and fling listeners return the distance and velocity respectively by which the
     * user has scrolled or flung. A positive value indicates movement in anticlockwise direction
     * and a negative value indicates a movement in clockwise direction.
     */
    var onScrollListener: ((Float) -> Unit)? = null
    var onFlingListener: ((Float) -> Unit)? = null

    var onTouchReleasedListener: (() -> Unit)? = null

    private var xCenter = 0f
    private var yCenter = 0f
    private var innerRadius = 0f
    private var outerRadius = 0f

    private var touchInitiatedBetweenCircles = false

    private var touchAreaPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.default_touch_area_color)
    }

    private var gestureDetector: GestureDetector = GestureDetector(context, this)

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ScrollWheel, 0, 0).apply {
            isHandleClicksEnabled = getBoolean(R.styleable.ScrollWheel_isHandleClicksEnabled, true)
            isHighlightTouchAreaEnabled =
                getBoolean(R.styleable.ScrollWheel_isHighlightTouchAreaEnabled, true)
            touchAreaThickness = getDimension(
                R.styleable.ScrollWheel_touchAreaThickness,
                context.resources.getDimension(R.dimen.default_touch_area_thickness)
            )
            setTouchAreaColor(
                getColor(
                    R.styleable.ScrollWheel_touchAreaColor,
                    ContextCompat.getColor(context, R.color.default_touch_area_color)
                )
            )
            recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        xCenter = measuredWidth / 2f
        yCenter = measuredHeight / 2f
        outerRadius = min(measuredWidth, measuredHeight) / 2f
        innerRadius = outerRadius - touchAreaThickness
        touchAreaPaint.strokeWidth = outerRadius - innerRadius
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isHighlightTouchAreaEnabled)
            canvas.drawCircle(xCenter, yCenter, (innerRadius + outerRadius) / 2f, touchAreaPaint)
    }

    // As the functionality of the view revolves around just providing a different experience for
    // scrolling and not enabling scrolling itself, implementing accessibility features would
    // provide no added value
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return super.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP) onTouchReleasedListener?.invoke()
        return gestureDetector.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent): Boolean {
        val x = e.x
        val y = e.y
        touchInitiatedBetweenCircles =
            ((x - xCenter) * (x - xCenter) + (y - yCenter) * (y - yCenter) > innerRadius * innerRadius
                    && (x - xCenter) * (x - xCenter) + (y - yCenter) * (y - yCenter) < outerRadius * outerRadius)
        return isHandleClicksEnabled || touchInitiatedBetweenCircles
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (touchInitiatedBetweenCircles)
            onScrollListener?.invoke(calculateDelta(e2.x, e2.y, distanceX, distanceY))
        return touchInitiatedBetweenCircles
    }

    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (touchInitiatedBetweenCircles)
            onFlingListener?.invoke(-calculateDelta(e2.x, e2.y, velocityX, velocityY))
        return touchInitiatedBetweenCircles
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        if (isHandleClicksEnabled) onItemClickListener?.invoke(e.x, e.y)
        return isHandleClicksEnabled
    }

    override fun onLongPress(e: MotionEvent) {
        if (isHandleClicksEnabled) onItemLongClickListener?.invoke(e.x, e.y)
    }

    override fun onShowPress(e: MotionEvent) {}

    private fun calculateDelta(x: Float, y: Float, dx: Float, dy: Float) = when {
        (x <= xCenter && y < yCenter) -> (dx - dy)
        (x > xCenter && y <= yCenter) -> (dx + dy)
        (x >= xCenter && y > yCenter) -> (-dx + dy)
        (x < xCenter && y >= yCenter) -> (-dx - dy)
        else -> 0f
    }

    @ColorInt
    fun getTouchAreaColor() = touchAreaPaint.color

    fun setTouchAreaColor(@ColorInt touchAreaColor: Int) {
        touchAreaPaint.color = touchAreaColor
        invalidate()
    }
}