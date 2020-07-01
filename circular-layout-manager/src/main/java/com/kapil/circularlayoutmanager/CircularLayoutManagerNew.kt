package com.kapil.circularlayoutmanager

import android.content.Context
import android.content.res.TypedArray
import android.graphics.PointF
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * This is a custom layout manager for recycler view which displays list items in a circular or
 * elliptical fashion.
 *
 * @see <a href="https://github.com/kapil93/Circular-Layout-Manager">Github Page</a>
 */
class CircularLayoutManagerNew : RecyclerView.LayoutManager, ScrollVectorProvider {

    companion object {
        private val TAG = CircularLayoutManagerNew::class.simpleName

        private const val FILL_START_POSITION = "FILL_START_POSITION"
        private const val FIRST_CHILD_TOP_OFFSET = "FIRST_CHILD_TOP_OFFSET"
    }

    private var xRadius: Float
    private var yRadius: Float
    private var xCenter: Float

    // The two fields below are the only parameters needed to define the scroll state.
    private var fillStartPosition = 0
    private var firstChildTopOffset = 0

    private var isFirstChildParametersProgrammaticallyUpdated = false

    /**
     * This constructor is called by the [RecyclerView] when the name of this layout manager is
     * passed as an XML attribute to the recycler view.
     *
     * For the purpose of instantiating this layout manager, radius and xCenter OR xRadius, yRadius
     * and xCenter should also be passed as XML attributes to the same recycler view depending on
     * whether a circular or an elliptical layout manager is desired respectively.
     */
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
    ) {
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.RecyclerView, defStyleAttr, defStyleRes
        )
        when {
            areAttrsForCircleAvailable(a) -> {
                xRadius = a.getDimension(R.styleable.RecyclerView_radius, 0f)
                yRadius = a.getDimension(R.styleable.RecyclerView_radius, 0f)
                xCenter = a.getDimension(R.styleable.RecyclerView_xCenter, 0f)
            }
            areAttrsForEllipseAvailable(a) -> {
                xRadius = a.getDimension(R.styleable.RecyclerView_xRadius, 0f)
                yRadius = a.getDimension(R.styleable.RecyclerView_yRadius, 0f)
                xCenter = a.getDimension(R.styleable.RecyclerView_xCenter, 0f)
            }
            else -> {
                throw InstantiationException(
                    "All the necessary attributes need to be supplied. " +
                            "For circle: radius and xCenter OR For ellipse: xRadius, yRadius and xCenter"
                )
            }
        }
        scalingFactor = a.getFloat(R.styleable.RecyclerView_scalingFactor, 0f)
        shouldIgnoreHeaderAndFooterMargins = a.getBoolean(
            R.styleable.RecyclerView_shouldIgnoreHeaderAndFooterMargins,
            false
        )
        shouldCenterIfProgrammaticallyScrolled = a.getBoolean(
            R.styleable.RecyclerView_shouldCenterIfProgrammaticallyScrolled,
            true
        )
        a.recycle()
    }

    /**
     * Creates a circular layout manager.
     *
     * Calls the constructor for ellipse as circle is also an ellipse with eccentricity = 1.
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
     * @param xCenter X-coordinate of center of the imaginary ellipse in dp.
     */
    constructor(xRadius: Float, yRadius: Float, xCenter: Float) {
        this.xRadius = xRadius
        this.yRadius = yRadius
        this.xCenter = xCenter
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
     * When set to false, the view associated with the position passed to the method appears at
     * the top.
     */
    var shouldCenterIfProgrammaticallyScrolled = true

    override fun generateDefaultLayoutParams() = RecyclerView.LayoutParams(
        RecyclerView.LayoutParams.WRAP_CONTENT,
        RecyclerView.LayoutParams.WRAP_CONTENT
    )

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        logIt("onLayoutChildren")
        adjustGapsIfProgrammaticallyScrolled(recycler)
        fill(recycler)
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
        calculateFirstChildPlacement(recycler)
        fill(recycler)
        return scrolled
    }

    override fun scrollToPosition(position: Int) {
        if (position in 0 until itemCount) {
            fillStartPosition = position
            firstChildTopOffset = 0
            isFirstChildParametersProgrammaticallyUpdated = true
            requestLayout()
        } else {
            Log.e(TAG, "scrollToPosition: Index: $position, Size: $itemCount")
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
                    return if (shouldCenterIfProgrammaticallyScrolled) {
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
            Log.e(TAG, "smoothScrollToPosition: Index: $position, Size: $itemCount")
        }
    }

    override fun computeScrollVectorForPosition(targetPosition: Int) =
        PointF(0f, (targetPosition - fillStartPosition).toFloat())

    override fun onAdapterChanged(
        oldAdapter: RecyclerView.Adapter<*>?,
        newAdapter: RecyclerView.Adapter<*>?
    ) {
        super.onAdapterChanged(oldAdapter, newAdapter)
        removeAllViews()
        clearScrollState()
    }

    override fun onSaveInstanceState(): Parcelable = Bundle().apply {
        putInt(FILL_START_POSITION, fillStartPosition)
        putInt(FIRST_CHILD_TOP_OFFSET, firstChildTopOffset)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        (state as? Bundle)?.let {
            fillStartPosition = it.getInt(FILL_START_POSITION)
            firstChildTopOffset = it.getInt(FIRST_CHILD_TOP_OFFSET)
        }
    }

    /**
     * This method is responsible to actually layout the child views based on the data obtained from
     * the recycler and the layout manager.
     *
     * Steps:
     * 1. Detaches and scraps all the attached views
     * 2. Adds, measures, scales (if needed) and lays out all the relevant child views. It starts
     *   based on the values [fillStartPosition] and [firstChildTopOffset] and stops when there is
     *   no more space left to fill
     * 3. Recycles the leftover views (if any) in the scrap list[RecyclerView.Recycler.getScrapList]
     */
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

    /**
     * Calculates [fillStartPosition] and [firstChildTopOffset] to prepare for the fill usually
     * after the child views have moved.
     *
     * @see scrollVerticallyBy
     */
    private fun calculateFirstChildPlacement(recycler: RecyclerView.Recycler) {
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
                clearScrollState()
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

    /**
     * @see shouldIgnoreHeaderAndFooterMargins
     */
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

    /**
     * This method calculates the x-coordinate of an ellipse based on the y-coordinate provided.
     *
     * This method is only relevant in the context of the specific use case of this class.
     *
     * Note: y-coordinate supplied is allowed to be an illegal value which may out of bounds with
     * respect to the imaginary ellipse.
     */
    private fun calculateEllipseXFromY(y: Int): Int {
        val yCenter = height / 2f
        val amount =
            (1 - (y - yCenter) * (y - yCenter) / (yRadius * yRadius)) * (xRadius * xRadius).toDouble()
        return if (amount >= 0) (sqrt(amount) + xCenter).toInt() else (-sqrt(-amount) + xCenter).toInt()
    }

    /**
     * This method is responsible to determine the amount by which the child views should be offset
     * as a response to user scroll. It introduces boundary conditions and prevents the child views
     * from being over-scrolled.
     */
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

    /**
     * The fields [fillStartPosition] and [firstChildTopOffset] are never set arbitrarily. They are
     * always updated based on some user action like in [scrollVerticallyBy]. There is no scope for
     * any gaps in the layout if these values are only updated as a response to scrollVerticallyBy.
     *
     * @see determineActualScroll
     *
     * But in [scrollToPosition] these values can be indirectly set programmatically which may
     * result in some gaps specially when views with first and last adapter positions are involved.
     *
     * This method adjusts fillStartPosition and firstChildTopOffset to eliminate potential gaps.
     */
    private fun adjustGapsIfProgrammaticallyScrolled(recycler: RecyclerView.Recycler) {
        if (isFirstChildParametersProgrammaticallyUpdated) {
            if (shouldCenterIfProgrammaticallyScrolled) {
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
                if (topGap > 0) clearScrollState()
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
                if (bottomGap > 0) clearScrollState()
            }

            isFirstChildParametersProgrammaticallyUpdated = false
        }
    }

    /**
     * Clears the scroll state.
     *
     * In other words, the next call to [fill] will result in the layout starting from the adapter
     * position 0.
     */
    private fun clearScrollState() {
        fillStartPosition = 0
        firstChildTopOffset = 0
    }

    private fun areAttrsForCircleAvailable(a: TypedArray) =
        a.hasValue(R.styleable.RecyclerView_radius) && a.hasValue(R.styleable.RecyclerView_xCenter)

    private fun areAttrsForEllipseAvailable(a: TypedArray) =
        a.hasValue(R.styleable.RecyclerView_xRadius) && a.hasValue(R.styleable.RecyclerView_yRadius)
                && a.hasValue(R.styleable.RecyclerView_xCenter)

    private fun logIt(msg: String) = Log.e("BCBCBCBCBC", msg)
}