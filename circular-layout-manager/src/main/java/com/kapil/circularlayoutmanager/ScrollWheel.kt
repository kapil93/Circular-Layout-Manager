package com.kapil.circularlayoutmanager

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min

/**
 * This view implements scroll wheel functionality.
 *
 * It converts circular touch motion into input for the scrolling of a recycler view.
 */
class ScrollWheel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), GestureDetector.OnGestureListener {

    /**
     * recyclerView Instance of recycler view that will be scrolled using scroll wheel.
     */
    var recyclerView: RecyclerView? = null

    /**
     * Toggle to enable or disable scroll wheel functionality.
     *
     * By default set to true.
     *
     * scrollWheelEnabled true or false.
     */
    var isScrollWheelEnabled = true

    /**
     * Toggle for enabling or disabling consumption of touch input outside the touch area.
     *
     * It can be set to true if only item click or item long click callback is needed, or can be set
     * to false if the touch events are required.
     *
     * The touch input would be simply passed to the next
     * view in case of touch down action outside touch area if set to false.
     *
     * By default set to true.
     *
     * consumeTouchOutsideTouchAreaEnabled true or false.
     */
    var isConsumeTouchOutsideTouchAreaEnabled = true

    /**
     * Function to show or hide path of action of the scroll wheel. Enabling it will highlight an
     * area on the screen as a cue for the user to use the scroll wheel.
     *
     * By default set to true.
     *
     * isHighlightTouchAreaEnabled true or false.
     */
    var isHighlightTouchAreaEnabled = true
        set(value) {
            field = value
            invalidate()
        }

    private var xCenter = 0
    private var yCenter = 0
    private var innerRadius = 0
    private var outerRadius = 0

    var touchAreaThickness =
        context.resources.getDimension(R.dimen.default_touch_area_thickness).toInt()
        set(value) {
            field = value
            invalidate()
        }

    private var touchInitiatedBetweenCircles = false

    private var touchAreaPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.default_touch_area_color)
    }

    private var gestureDetector: GestureDetector = GestureDetector(context, this)

    private var onItemClickListener: OnItemClickListener? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        xCenter = measuredWidth / 2
        yCenter = measuredHeight / 2
        val minDimension = min(measuredWidth, measuredHeight)
        outerRadius = minDimension / 2
        innerRadius = outerRadius - touchAreaThickness
        touchAreaPaint.strokeWidth = outerRadius - innerRadius.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isHighlightTouchAreaEnabled) {
            canvas.drawCircle(
                xCenter.toFloat(),
                yCenter.toFloat(),
                (innerRadius + outerRadius) / 2f,
                touchAreaPaint
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (recyclerView == null || !isScrollWheelEnabled) {
            return super.onTouchEvent(event)
        }
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            (recyclerView!!.layoutManager as CircularLayoutManager).stabilize()
        }
        return gestureDetector.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent): Boolean {
        val x = e.x
        val y = e.y
        touchInitiatedBetweenCircles =
            ((x - xCenter) * (x - xCenter) + (y - yCenter) * (y - yCenter) > innerRadius * innerRadius
                    && (x - xCenter) * (x - xCenter) + (y - yCenter) * (y - yCenter) < outerRadius * outerRadius)
        return isConsumeTouchOutsideTouchAreaEnabled || touchInitiatedBetweenCircles
    }

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        if (onItemClickListener != null && isConsumeTouchOutsideTouchAreaEnabled) {
            val childIndex = getChildIndexUnder(e.x, e.y)
            if (childIndex != -1) {
                onItemClickListener!!.onItemClick(this@ScrollWheel, childIndex)
            }
        }
        return true
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (!touchInitiatedBetweenCircles) {
            return false
        }
        var delta = 0
        val x = e2.x
        val y = e2.y
        if (x <= xCenter && y < yCenter) {
            delta = (distanceX - distanceY).toInt()
        } else if (x > xCenter && y <= yCenter) {
            delta = (distanceX + distanceY).toInt()
        } else if (x >= xCenter && y > yCenter) {
            delta = (-distanceX + distanceY).toInt()
        } else if (x < xCenter && y >= yCenter) {
            delta = (-distanceX - distanceY).toInt()
        }
        recyclerView!!.scrollBy(0, delta)
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        if (onItemClickListener != null && isConsumeTouchOutsideTouchAreaEnabled) {
            val childIndex = getChildIndexUnder(e.x, e.y)
            if (childIndex != -1) {
                onItemClickListener!!.onItemLongClick(this@ScrollWheel, childIndex)
            }
        }
    }

    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (!touchInitiatedBetweenCircles) {
            return false
        }
        var delta = 0
        val x = e2.x
        val y = e2.y
        if (x <= xCenter && y < yCenter) {
            delta = (velocityX - velocityY).toInt()
        } else if (x > xCenter && y <= yCenter) {
            delta = (velocityX + velocityY).toInt()
        } else if (x >= xCenter && y > yCenter) {
            delta = (-velocityX + velocityY).toInt()
        } else if (x < xCenter && y >= yCenter) {
            delta = (-velocityX - velocityY).toInt()
        }
        recyclerView!!.fling(0, -delta)
        return true
    }

    /**
     * Detects the child view under a particular point.
     *
     * @param x X-coordinate of point.
     * @param y Y-coordinate of point.
     * @return  Index of child view if it is found under the point, -1 if there is no child view
     * found under the point.
     */
    private fun getChildIndexUnder(x: Float, y: Float): Int {
        val child = recyclerView!!.findChildViewUnder(x, y)
        return if (child != null) {
            recyclerView!!.indexOfChild(child)
        } else -1
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
    }

    @ColorInt
    fun getTouchAreaColor() = touchAreaPaint.color

    fun setTouchAreaColor(@ColorInt touchAreaColor: Int) {
        touchAreaPaint.color = touchAreaColor
        invalidate()
    }
}