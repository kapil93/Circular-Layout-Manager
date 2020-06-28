package com.kapil.circularlayoutmanager

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * This is a custom layout manager for recycler view which displays list items in a circular or
 * elliptical fashion.
 */
class CircularLayoutManager : RecyclerView.LayoutManager {

    enum class Path {
        CIRCLE,
        ELLIPSE
    }

    private var recyclerView: RecyclerView? = null
    private var recyclerBounds: Rect? = null
    private var topOfFirstChild = 0
    private var childDecoratedBoundsWithMargin: Rect? = null
    private var verticalCenter = 0
    private var scrolled = false
    private var radius = 0f
    private var majorRadius = 0f
    private var minorRadius = 0f
    private var centerX: Float

    private var layoutPath: Path

    /**
     * Creates a circular layout manager.
     *
     * @param radius  Radius of the imaginary circle in dp.
     * @param centerX X-coordinate of center of the imaginary circle in dp.
     */
    constructor(radius: Float, centerX: Float) {
        this.radius = radius
        this.centerX = centerX
        layoutPath = Path.CIRCLE
    }

    /**
     * Creates an elliptical layout manager.
     *
     * @param majorRadius Major radius of the imaginary ellipse in dp.
     * @param minorRadius Minor radius of the imaginary ellipse in dp.
     * @param centerX     X-coordinate of center of the imaginary ellipse in dp.
     */
    constructor(majorRadius: Float, minorRadius: Float, centerX: Float) {
        this.majorRadius = majorRadius
        this.minorRadius = minorRadius
        this.centerX = centerX
        layoutPath = Path.ELLIPSE
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onAttachedToWindow(view: RecyclerView) {
        super.onAttachedToWindow(view)
        recyclerView = view
        topOfFirstChild = 0
        childDecoratedBoundsWithMargin = Rect()
        scrolled = false
    }

    override fun onDetachedFromWindow(view: RecyclerView, recycler: RecyclerView.Recycler) {
        super.onDetachedFromWindow(view, recycler)
        removeAndRecycleAllViews(recycler)
        recycler.clear()
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (itemCount == 0) {
            detachAndScrapAttachedViews(recycler)
            return
        }
        if (recyclerBounds == null) {
            recyclerBounds = Rect()
            recyclerView!!.getHitRect(recyclerBounds)
            verticalCenter = recyclerBounds!!.height() / 2
        }
        if (childCount == 0) {
            fill(0, recycler)
        }
    }

    /**
     * This function lays out child views into appropriate position with respect to an anchor,
     * (topOfFirstChild).
     *
     * @param indexToStartFill Index of child to start layout operation.
     * @param recycler         Recycler, for detaching, scraping and recycling of child views.
     */
    private fun fill(indexToStartFill: Int, recycler: RecyclerView.Recycler) {
        var indexToStartFill = indexToStartFill
        if (indexToStartFill < 0) {
            indexToStartFill = 0
        }
        var childTop = topOfFirstChild
        detachAndScrapAttachedViews(recycler)
        for (i in indexToStartFill until itemCount) {
            val child = recycler.getViewForPosition(i)
            measureChildWithMargins(child, 0, 0)
            val sumOfHorizontalMargins =
                ((child.layoutParams as RecyclerView.LayoutParams).leftMargin
                        + (child.layoutParams as RecyclerView.LayoutParams).rightMargin)
            val sumOfVerticalMargins =
                ((child.layoutParams as RecyclerView.LayoutParams).topMargin
                        + (child.layoutParams as RecyclerView.LayoutParams).bottomMargin)
            val childLeft = when (layoutPath) {
                Path.CIRCLE -> calculateCircleXFromY(
                    childTop + (getDecoratedMeasuredHeight(child) +
                            getTopDecorationHeight(child) - getBottomDecorationHeight(child) + sumOfVerticalMargins) / 2
                )
                Path.ELLIPSE -> calculateEllipseXFromY(
                    childTop + (getDecoratedMeasuredHeight(child) +
                            getTopDecorationHeight(child) - getBottomDecorationHeight(child) + sumOfVerticalMargins) / 2
                )
            }
            if (!(recyclerBounds!!.intersects(
                    recyclerBounds!!.left + childLeft, recyclerBounds!!.top + childTop,
                    recyclerBounds!!.left + childLeft + getDecoratedMeasuredWidth(child) + sumOfHorizontalMargins,
                    recyclerBounds!!.top + childTop + getDecoratedMeasuredHeight(child) + sumOfVerticalMargins
                )
                        || recyclerBounds!!.contains(
                    recyclerBounds!!.left + childLeft, recyclerBounds!!.top + childTop,
                    recyclerBounds!!.left + childLeft + getDecoratedMeasuredWidth(child) + sumOfHorizontalMargins,
                    recyclerBounds!!.top + childTop + getDecoratedMeasuredHeight(child) + sumOfVerticalMargins
                ))
            ) {
                break
            }
            addView(child)
            layoutDecoratedWithMargins(
                child,
                childLeft,
                childTop,
                childLeft + getDecoratedMeasuredWidth(child)
                        + sumOfHorizontalMargins,
                childTop + getDecoratedMeasuredHeight(child) + sumOfVerticalMargins
            )
            getDecoratedBoundsWithMargins(child, childDecoratedBoundsWithMargin!!)
            scaleChild(child)
            childTop += childDecoratedBoundsWithMargin!!.height()
        }
//        recycler.scrapList.forEach { recycler.recycleView(it.itemView) }
        recycler.clear()

        if (!scrolled) {
            stabilize()
        }
    }

    override fun canScrollVertically(): Boolean {
        return true
    }

    override fun onScrollStateChanged(state: Int) {
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            stabilize()
        }
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        if (!scrolled) {
            scrolled = true
        }
        var delta = dy
        if (delta > 150) {
            delta = 150
        }
        if (delta < -150) {
            delta = -150
        }
        if (childCount == 0) {
            return dy
        }
        if (getPosition(getChildAt(childCount - 1)!!) == itemCount - 1) {
            val child = getChildAt(childCount - 1)
            getDecoratedBoundsWithMargins(child!!, childDecoratedBoundsWithMargin!!)
            if (childDecoratedBoundsWithMargin!!.bottom - delta < recyclerBounds!!.height()) {
                var position = recyclerBounds!!.height()
                var indexToStartFill = getPosition(getChildAt(0)!!)
                for (i in childCount - 1 downTo 0) {
                    getDecoratedBoundsWithMargins(getChildAt(i)!!, childDecoratedBoundsWithMargin!!)
                    position -= childDecoratedBoundsWithMargin!!.height()
                    if (position <= 0) {
                        topOfFirstChild = position
                        if (topOfFirstChild <= -childDecoratedBoundsWithMargin!!.height()) {
                            topOfFirstChild += childDecoratedBoundsWithMargin!!.height()
                        }
                        indexToStartFill = getPosition(getChildAt(i)!!)
                        if (indexToStartFill >= itemCount) {
                            indexToStartFill = itemCount - 1
                        }
                        break
                    }
                }
                fill(indexToStartFill, recycler)
                return 0
            }
        }
        topOfFirstChild -= delta
        getDecoratedBoundsWithMargins(getChildAt(0)!!, childDecoratedBoundsWithMargin!!)
        var indexToStartFill = getPosition(getChildAt(0)!!)
        if (topOfFirstChild > 0) {
            topOfFirstChild -= childDecoratedBoundsWithMargin!!.height()
            indexToStartFill--
            if (indexToStartFill == -1) {
                topOfFirstChild = 0
                fill(0, recycler)
                return 0
            }
        } else if (topOfFirstChild <= -childDecoratedBoundsWithMargin!!.height()) {
            topOfFirstChild += childDecoratedBoundsWithMargin!!.height()
            indexToStartFill++
        }
        fill(indexToStartFill, recycler)
        return dy
    }

    /**
     * Scales the width and height of a child view depending on it's vertical positioning.
     *
     * @param child Child View to be scaled.
     */
    private fun scaleChild(child: View) {
        val y = (child.top + child.bottom) / 2
        val scale =
            1 - abs(verticalCenter - y) / (recyclerBounds!!.height() - child.height).toFloat()
        child.pivotX = 0f
        child.scaleX = scale
        child.scaleY = scale
    }

    /**
     * This function calculates horizontal position of child view depending on it's vertical position
     * using the circle equation.
     *
     * @param y Vertical positioning of the child view.
     * @return  Horizontal positioning of the child view.
     */
    private fun calculateCircleXFromY(y: Int): Int {
        val centerY = verticalCenter
        return (sqrt(radius * radius - ((y - centerY) * (y - centerY)).toDouble()) + centerX).toInt()
    }

    /**
     * This function calculates horizontal position of child view depending on it's vertical position
     * using the circle equation.
     *
     * @param y Vertical positioning of the child view.
     * @return  Horizontal positioning of the child view.
     */
    private fun calculateEllipseXFromY(y: Int): Int {
        val centerY = verticalCenter
        return (sqrt((1 - (y - centerY) * (y - centerY) / (minorRadius * minorRadius)) * (majorRadius * majorRadius).toDouble()) + centerX).toInt()
    }

    /**
     * This function is responsible for centering of the list items on idle scroll state with
     * reference to a vertical center.
     */
    fun stabilize() {
        var minDistance = Int.MAX_VALUE
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val y = (child!!.top + child.bottom) / 2
            minDistance = if (abs(y - verticalCenter) < abs(minDistance)) {
                y - verticalCenter
            } else {
                break
            }
        }
        recyclerView!!.smoothScrollBy(0, minDistance)
    }
}