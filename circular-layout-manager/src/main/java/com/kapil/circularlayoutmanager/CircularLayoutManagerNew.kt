package com.kapil.circularlayoutmanager

import android.graphics.PointF
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
import kotlin.math.abs
import kotlin.math.sqrt


class CircularLayoutManagerNew : RecyclerView.LayoutManager, ScrollVectorProvider {

    private var xRadius: Float
    private var yRadius: Float
    private var centerX: Float

    private var fillStartPosition = 0
    private var firstChildTopOffset = 0

    private var scrollToPositionCalled = false

    /**
     * Creates a circular layout manager.
     *
     * @param radius  Radius of the imaginary circle in dp.
     * @param centerX X-coordinate of center of the imaginary circle in dp.
     */
    constructor(radius: Float, centerX: Float) : this(radius, radius, centerX)

    /**
     * Creates an elliptical layout manager.
     *
     * @param xRadius Radius of the imaginary ellipse along X-axis in dp.
     * @param yRadius Radius of the imaginary ellipse along Y-axis in dp.
     * @param centerX X-coordinate of center of the imaginary ellipse in dp.
     */
    constructor(xRadius: Float, yRadius: Float, centerX: Float) {
        this.xRadius = xRadius
        this.yRadius = yRadius
        this.centerX = centerX
        logIt("init xR $xRadius yR $yRadius cX $centerX")
    }

    /**
     * Header and Footer item margins are by default taken into consideration while calculating the
     * left offset. Setting this value to true would ignore the top decoration margin for first
     * adapter item and bottom decoration margin for last adapter item.
     */
    var shouldIgnoreHeaderAndFooterMargins = false

    /**
     * Scaling factor determines the amount by which an item would be shrunk when it moves away from
     * the center.
     *
     * It is supposed to take float values between 0 to infinity.
     *
     * Set this value to 0.0 if no scaling is desired.
     */
    var scalingFactor = 0f

    /**
     * This value determines whether the call to [RecyclerView.scrollToPosition] and in turn the
     * call to [CircularLayoutManagerNew.scrollToPosition] or the call to
     * [RecyclerView.smoothScrollToPosition] and in turn the call to
     * [CircularLayoutManagerNew.smoothScrollToPosition] makes the view, associated with position
     * passed, to appear in the center or not.
     *
     * This field has NO relevance when user action is involved in scrolling.
     *
     * When set to false, the view, associated with the position passed to the method, appears at
     * the top.
     */
    var shouldCenterAfterScrollToPosition = true

    override fun generateDefaultLayoutParams() = RecyclerView.LayoutParams(
        RecyclerView.LayoutParams.WRAP_CONTENT,
        RecyclerView.LayoutParams.WRAP_CONTENT
    )

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        adjustGapsIfProgrammaticallyScrolled(recycler)
        fill(recycler)
    }

    private fun fill(recycler: RecyclerView.Recycler) {
        if (itemCount == 0) {
            removeAndRecycleAllViews(recycler)
            return
        }

        var tmpOffset = firstChildTopOffset

        detachAndScrapAttachedViews(recycler)

        for (position in fillStartPosition until itemCount) {
            val child = recycler.getViewForPosition(position)

            addView(child)

            measureChildWithMargins(child, 0, 0)

            val childWidth = getDecoratedMeasuredWidth(child)
            val childHeight = getDecoratedMeasuredHeight(child)

            val left = calculateLeftOffset(position, child, childHeight, tmpOffset)
            val top = tmpOffset

            layoutDecoratedWithMargins(child, left, top, left + childWidth, top + childHeight)

            scaleChild(child)

            tmpOffset += childHeight

            if (tmpOffset > height) break
        }

        recycler.scrapList.toList().forEach { recycler.recycleView(it.itemView) }
    }

    override fun canScrollVertically() = true

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        if (childCount == 0) return 0
        val scrolled = determineActualScroll(dy)
        offsetChildrenVertical(-scrolled)
        calculateFirstChildPlacementProperties(recycler)
        fill(recycler)
        return scrolled
    }

    override fun scrollToPosition(position: Int) {
        if (position in 0 until itemCount) {
            fillStartPosition = position
            firstChildTopOffset = 0
            scrollToPositionCalled = true
            requestLayout()
        } else {
            throw IndexOutOfBoundsException("Index: $position, Size: $itemCount")
        }
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State,
        position: Int
    ) {
        if (position in 0 until itemCount) {
            object : LinearSmoothScroller(recyclerView.context) {
                override fun calculateDtToFit(
                    viewStart: Int,
                    viewEnd: Int,
                    boxStart: Int,
                    boxEnd: Int,
                    snapPreference: Int
                ): Int {
                    return if (shouldCenterAfterScrollToPosition) {
                        ((boxStart + boxEnd) / 2) - ((viewStart + viewEnd) / 2)
                    } else {
                        super.calculateDtToFit(viewStart, viewEnd, boxStart, boxEnd, snapPreference)
                    }
                }
            }.apply {
                targetPosition = position
                startSmoothScroll(this)
            }
        } else {
            throw IndexOutOfBoundsException("Index: $position, Size: $itemCount")
        }
    }

    override fun computeScrollVectorForPosition(targetPosition: Int) =
        PointF(0f, (targetPosition - fillStartPosition).toFloat())

    override fun onDetachedFromWindow(view: RecyclerView, recycler: RecyclerView.Recycler) {
        super.onDetachedFromWindow(view, recycler)
        removeAndRecycleAllViews(recycler)
        recycler.clear()
    }

    private fun calculateFirstChildPlacementProperties(recycler: RecyclerView.Recycler) {
        // Find first visible view
        for (i in 0 until childCount) {
            val tmpChild = getChildAt(i)!!
            if (getDecoratedBottom(tmpChild) > 0) {
                firstChildTopOffset = getDecoratedTop(tmpChild)
                fillStartPosition = getPosition(tmpChild)
                break
            }
        }
        // Find position from which filling of gap should be started
        while (firstChildTopOffset > 0) {
            fillStartPosition -= 1
            if (fillStartPosition < 0) {
                fillStartPosition = 0
                firstChildTopOffset = 0
                break
            } else {
                val tmpChild = recycler.getViewForPosition(fillStartPosition)
                measureChildWithMargins(tmpChild, 0, 0)
                firstChildTopOffset -= getDecoratedMeasuredHeight(tmpChild)
            }
        }
    }

    /**
     * Scales the width and height of a child view depending on it's vertical positioning relative
     * to the horizontal axis of the ellipse.
     *
     * @param child Child View to be scaled.
     *
     * @see scalingFactor
     */
    private fun scaleChild(child: View) {
        val y = (child.top + child.bottom) / 2
        val scale = 1 - (scalingFactor * (abs((height / 2f) - y) / (height - child.height)))
        child.pivotX = 0f
        child.pivotY = child.height / 2f
        child.scaleX = scale
        child.scaleY = scale
    }

    private fun calculateLeftOffset(position: Int, child: View, childHeight: Int, tmpOffset: Int) =
        if (shouldIgnoreHeaderAndFooterMargins) {
            when (position) {
                0 -> calculateEllipseXFromY(
                    tmpOffset + getTopDecorationHeight(child)
                            + ((childHeight - getTopDecorationHeight(child)) / 2)
                )
                itemCount - 1 -> calculateEllipseXFromY(
                    tmpOffset + ((childHeight - getBottomDecorationHeight(child)) / 2)
                )
                else -> calculateEllipseXFromY(tmpOffset + (childHeight / 2))
            }
        } else calculateEllipseXFromY(tmpOffset + (childHeight / 2))

    private fun calculateEllipseXFromY(y: Int): Int {
        val centerY = height / 2f
        val amount =
            (1 - (y - centerY) * (y - centerY) / (yRadius * yRadius)) * (xRadius * xRadius).toDouble()
        return if (amount >= 0) (sqrt(amount) + centerX).toInt() else (-sqrt(-amount) + centerX).toInt()
    }

    private fun determineActualScroll(dy: Int): Int {
        val firstChild = getChildAt(0)!!
        val lastChild = getChildAt(childCount - 1)!!
        return when {
            (dy < 0 && getPosition(firstChild) == 0 && getDecoratedTop(firstChild) - dy > 0) ->
                getDecoratedTop(firstChild)
            (dy > 0 && getPosition(lastChild) == itemCount - 1 && getDecoratedBottom(lastChild) - dy < height) ->
                getDecoratedBottom(lastChild) - height
            else -> dy
        }
    }

    private fun adjustGapsIfProgrammaticallyScrolled(recycler: RecyclerView.Recycler) {
        if (scrollToPositionCalled) {
            if (shouldCenterAfterScrollToPosition) {
                // If shouldCenterAfterScrollToPosition is true, readjust fillStartPosition
                // Also, ensure there is no gap at the top
                val centerChild = recycler.getViewForPosition(fillStartPosition)
                measureChildWithMargins(centerChild, 0, 0)
                var topGap = (height / 2) - (getDecoratedMeasuredHeight(centerChild) / 2)
                for (position in (fillStartPosition - 1) downTo 0) {
                    val tmpChild = recycler.getViewForPosition(position)
                    measureChildWithMargins(tmpChild, 0, 0)
                    topGap -= getDecoratedMeasuredHeight(tmpChild)
                    if (topGap <= 0) {
                        fillStartPosition = position
                        firstChildTopOffset = topGap
                        break
                    }
                }
                if (topGap > 0) {
                    fillStartPosition = 0
                    firstChildTopOffset = 0
                }
            }

            if (fillStartPosition != 0) {
                // Ensure there is no gap at the bottom
                var bottomGap = height
                for (position in (itemCount - 1) downTo 0) {
                    val tmpChild = recycler.getViewForPosition(position)
                    measureChildWithMargins(tmpChild, 0, 0)
                    bottomGap -= getDecoratedMeasuredHeight(tmpChild)
                    if (bottomGap <= 0) {
                        if ((position < fillStartPosition) || (position == fillStartPosition && bottomGap > firstChildTopOffset)) {
                            fillStartPosition = position
                            firstChildTopOffset = bottomGap
                        }
                        break
                    }
                }
                if (bottomGap > 0) {
                    fillStartPosition = 0
                    firstChildTopOffset = 0
                }
            }

            scrollToPositionCalled = false
        }
    }

    private fun logIt(msg: String) = Log.e("BCBCBCBCBC", msg)
}