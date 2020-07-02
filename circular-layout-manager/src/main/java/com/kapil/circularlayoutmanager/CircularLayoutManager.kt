package com.kapil.circularlayoutmanager

import android.content.Context
import android.content.res.TypedArray
import android.graphics.PointF
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * This is a custom layout manager for recycler view which displays list items in a circular or
 * elliptical fashion.
 *
 * @see <a href="https://github.com/kapil93/Circular-Layout-Manager">Github Page</a>
 */
open class CircularLayoutManager : RecyclerView.LayoutManager, ScrollVectorProvider {

    companion object {
        private val TAG = CircularLayoutManager::class.simpleName

        private const val FILL_START_POSITION = "FILL_START_POSITION"
        private const val FIRST_CHILD_TOP_OFFSET = "FIRST_CHILD_TOP_OFFSET"

        private const val MILLIS_PER_INCH_FAST = 25f
        private const val MILLIS_PER_INCH_SLOW = 200f
    }

    private val xRadius: Float
    private val yRadius: Float
    private val xCenter: Float

    // The two fields below are the only parameters needed to define the scroll state.
    // They together define the placement of the first child view in the layout.
    private var fillStartPosition = 0
    private var firstChildTopOffset = 0

    private var isFirstChildParametersProgrammaticallyUpdated = false

    /**
     * Needed only for the stabilization feature. Value assigned and updated on every call to [fill].
     *
     * Not sure whether holding a reference of [RecyclerView.Recycler] will create any problems.
     *
     * @see stabilize
     * @see couldChildBeBroughtDownToCenter
     * @see couldChildBeBroughtUpToCenter
     */
    private lateinit var mRecycler: RecyclerView.Recycler

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
        isAutoStabilizationEnabled = a.getBoolean(
            R.styleable.RecyclerView_isAutoStabilizationEnabled,
            true
        )
        a.recycle()
    }

    /**
     * Creates a circular layout manager.
     *
     * Calls the constructor for ellipse as circle is also an ellipse.
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
     * call to [CircularLayoutManager.scrollToPosition] or the call to
     * [RecyclerView.smoothScrollToPosition] and in turn the call to
     * [CircularLayoutManager.smoothScrollToPosition] makes the view, associated with position
     * passed, to appear in the center or not.
     *
     * This field has NO relevance when user action is involved in scrolling.
     *
     * When set to false, the view associated with the position passed to the method appears at
     * the top.
     */
    var shouldCenterIfProgrammaticallyScrolled = true

    /**
     * Enables or disables auto stabilization feature. Manual call to [stabilize] is always possible.
     *
     * Some tuning of the top and bottom item offsets (decorations) of the first and last adapter
     * items might be required to get the desired over scroll effect when auto stabilization is
     * enabled.
     *
     * @see stabilize
     */
    var isAutoStabilizationEnabled = true

    override fun generateDefaultLayoutParams() = RecyclerView.LayoutParams(
        RecyclerView.LayoutParams.WRAP_CONTENT,
        RecyclerView.LayoutParams.WRAP_CONTENT
    )

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        adjustGapsIfProgrammaticallyScrolled(recycler)
        fill(recycler)
    }

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        if (isAutoStabilizationEnabled) stabilize()
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
            startSmoothScroll(
                recyclerView.context,
                position,
                MILLIS_PER_INCH_FAST,
                shouldCenterIfProgrammaticallyScrolled
            )
        } else {
            Log.e(TAG, "smoothScrollToPosition: Index: $position, Size: $itemCount")
        }
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (isAutoStabilizationEnabled && state == SCROLL_STATE_IDLE) stabilize()
    }

    override fun computeScrollVectorForPosition(targetPosition: Int) =
        PointF(0f, (targetPosition - fillStartPosition).toFloat())

    // The three methods below are mainly overridden for accessibility. More specifically, to enable
    // scrolling while using TalkBack.
    override fun computeVerticalScrollOffset(state: RecyclerView.State) =
        getPosition(getChildAt(0)!!)

    override fun computeVerticalScrollExtent(state: RecyclerView.State) =
        getPosition(getChildAt(childCount - 1)!!) - getPosition(getChildAt(0)!!) + 1

    override fun computeVerticalScrollRange(state: RecyclerView.State) = itemCount

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

        mRecycler = recycler

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

            isFirstChildParametersProgrammaticallyUpdated = false
        }
    }

    private fun startSmoothScroll(
        context: Context,
        targetPosition: Int,
        millisPerInch: Float,
        shouldCenter: Boolean
    ) {
        object : LinearSmoothScroller(context) {

            override fun calculateDtToFit(
                viewStart: Int,
                viewEnd: Int,
                boxStart: Int,
                boxEnd: Int,
                snapPreference: Int
            ): Int {
                return if (shouldCenter) {
                    ((boxStart + boxEnd) / 2) - ((viewStart + viewEnd) / 2)
                } else {
                    super.calculateDtToFit(viewStart, viewEnd, boxStart, boxEnd, snapPreference)
                }
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics) =
                millisPerInch / displayMetrics.densityDpi

        }.apply {
            this.targetPosition = targetPosition
            startSmoothScroll(this)
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

    private fun couldChildBeBroughtDownToCenter(nearestChildIndex: Int): Boolean {
        val nearestChildPosition = getPosition(getChildAt(nearestChildIndex)!!)
        var topGap = height / 2
        for (i in 0 until nearestChildPosition) {
            val child = mRecycler.getViewForPosition(i)
            measureChildWithMargins(child, 0, 0)
            topGap -= getDecoratedMeasuredHeight(child)
            if (topGap <= 0) return true
        }
        val nearestChild = mRecycler.getViewForPosition(nearestChildPosition)
        measureChildWithMargins(nearestChild, 0, 0)
        topGap -= getDecoratedMeasuredHeight(nearestChild) / 2
        return topGap <= 0
    }

    private fun couldChildBeBroughtUpToCenter(nearestChildIndex: Int): Boolean {
        val nearestChildPosition = getPosition(getChildAt(nearestChildIndex)!!)
        var bottomGap = height / 2
        for (i in (itemCount - 1) downTo (nearestChildPosition + 1)) {
            val child = mRecycler.getViewForPosition(i)
            measureChildWithMargins(child, 0, 0)
            bottomGap -= getDecoratedMeasuredHeight(child)
            if (bottomGap <= 0) return true
        }
        val nearestChild = mRecycler.getViewForPosition(nearestChildPosition)
        measureChildWithMargins(nearestChild, 0, 0)
        bottomGap -= getDecoratedMeasuredHeight(nearestChild) / 2
        return bottomGap <= 0
    }

    /**
     * This method is responsible for detecting the view nearest to the vertical center of the
     * layout and centering it.
     *
     * This method is called only after a user scroll or fling ends and after layout completion.
     * These automatic calls could be enabled or disabled by [isAutoStabilizationEnabled].
     *
     * This For programmatic scrolls, [scrollToPosition] and [smoothScrollToPosition] is supported.
     * The centering behaviour can be controlled by the field [shouldCenterIfProgrammaticallyScrolled].
     *
     * When scrolling is externally and programmatically triggered through methods like
     * [RecyclerView.scrollBy], there is no clean way of knowing whether the scrolling has stopped
     * or not with certainty. Therefore, this method should be called in such cases whenever
     * stabilization is necessary even when isAutoStabilizationEnabled is set to true. Ideally it
     * should be called when the scroll state is IDLE.
     *
     * The distance between the center of the layout and the center of the view would keep on
     * decreasing as we traverse downwards from the top child towards the center of the layout. If
     * at any moment the distance is greater than the previous distance, it means that we are now
     * moving away from center, so we ignore this greater distance and break out of the loop as the
     * child with the least distance from the center of the layout should be the previous child.
     *
     * The above logic is used to find the nearest child from vertical center and the current
     * distance which separates them. After that, it is checked whether movement in either direction
     * is possible or not.
     */
    fun stabilize() {
        if (childCount == 0 || isSmoothScrolling) return

        var minDistance = Int.MAX_VALUE
        var nearestChildIndex = 0
        val yCenter = height / 2
        for (i in 0 until childCount) {
            val child = getChildAt(i)!!
            val y = (getDecoratedTop(child) + getDecoratedBottom(child)) / 2
            if (abs(y - yCenter) < abs(minDistance)) {
                nearestChildIndex = i
                minDistance = y - yCenter
            } else break
        }

        var isStabilizationPossible = false

        if (minDistance < 0) {
            // Child is above the center
            if (couldChildBeBroughtDownToCenter(nearestChildIndex)) {
                isStabilizationPossible = true
            } else {
                if (nearestChildIndex + 1 < childCount) {
                    // Here an assumption is made that the child views would approximately be of the
                    // same height and the next nearest child to center would be below the center.
                    if (couldChildBeBroughtUpToCenter(nearestChildIndex + 1)) {
                        nearestChildIndex++
                        isStabilizationPossible = true
                    }
                }
            }
        } else if (minDistance > 0) {
            // Child is below the center
            if (couldChildBeBroughtUpToCenter(nearestChildIndex)) {
                isStabilizationPossible = true
            } else {
                // This condition should always be true. But is checked just for safety.
                if (nearestChildIndex - 1 >= 0) {
                    // Here an assumption is made that the child views would approximately be of the
                    // same height and the next nearest child to center would be above the center.
                    if (couldChildBeBroughtDownToCenter(nearestChildIndex - 1)) {
                        nearestChildIndex--
                        isStabilizationPossible = true
                    }
                }
            }
        }

        if (isStabilizationPossible)
            startSmoothScroll(
                getChildAt(nearestChildIndex)!!.context,
                getPosition(getChildAt(nearestChildIndex)!!),
                MILLIS_PER_INCH_SLOW,
                true
            )
    }
}