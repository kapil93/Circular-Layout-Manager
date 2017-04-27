package com.kapil.circularlayoutmanager;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

/**
 * This is majorRadius custom layout manager for recycler view which displays list items in majorRadius circular or
 * elliptical fashion.
 */

public class CircularLayoutManager extends RecyclerView.LayoutManager {
    private static final int CIRCLE = 0;
    private static final int ELLIPSE = 1;

    private RecyclerView recyclerView;
    private Rect recyclerBounds;

    private int topOfFirstChild;

    private Rect childDecoratedBoundsWithMargin;
    private int verticalCenter;
    private boolean scrolled;

    private float radius;
    private float majorRadius, minorRadius;
    private float centerX;

    private @LayoutPath int layoutPath;

    @IntDef({CIRCLE, ELLIPSE})
    @interface LayoutPath {

    }

    /**
     * Creates a circular layout manager.
     *
     * @param context Current context, will be to used to access resources.
     * @param radius  Radius of the imaginary circle in dp.
     * @param centerX X-coordinate of center of the imaginary circle in dp.
     */

    public CircularLayoutManager(Context context, int radius, int centerX) {
        this.radius = Utils.dpToPx(context, radius);
        this.centerX = Utils.dpToPx(context, centerX);

        layoutPath = CIRCLE;
    }

    /**
     * Creates an elliptical layout manager.
     *
     * @param context     Current context, will be to used to access resources.
     * @param majorRadius Major radius of the imaginary ellipse in dp.
     * @param minorRadius Minor radius of the imaginary ellipse in dp.
     * @param centerX     X-coordinate of center of the imaginary ellipse in dp.
     */

    public CircularLayoutManager(Context context, int majorRadius, int minorRadius, int centerX) {
        this.majorRadius = Utils.dpToPx(context, majorRadius);
        this.minorRadius = Utils.dpToPx(context, minorRadius);
        this.centerX = Utils.dpToPx(context, centerX);

        layoutPath = ELLIPSE;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);

        recyclerView = view;
        topOfFirstChild = 0;
        childDecoratedBoundsWithMargin = new Rect();
        scrolled = false;
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);

        removeAndRecycleAllViews(recycler);
        recycler.clear();
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }

        if (recyclerBounds == null) {
            recyclerBounds = new Rect();
            recyclerView.getHitRect(recyclerBounds);
            verticalCenter = (recyclerBounds.height() / 2);
        }

        if (getChildCount() == 0) {
            fill(0, recycler);
        }
    }

    /**
     * This function lays out child views into appropriate position with respect to an anchor,
     * (topOfFirstChild).
     *
     * @param indexToStartFill Index of child to start layout operation.
     * @param recycler         Recycler, for detaching, scraping and recycling of child views.
     */

    private void fill(int indexToStartFill, RecyclerView.Recycler recycler) {
        if (indexToStartFill < 0) {
            indexToStartFill = 0;
        }

        int childTop = topOfFirstChild;

        detachAndScrapAttachedViews(recycler);

        for (int i = indexToStartFill; i < getItemCount(); i++) {
            View child = recycler.getViewForPosition(i);

            measureChildWithMargins(child, 0, 0);

            int sumOfHorizontalMargins = ((RecyclerView.LayoutParams) child.getLayoutParams()).leftMargin
                    + ((RecyclerView.LayoutParams) child.getLayoutParams()).rightMargin;
            int sumOfVerticalMargins = ((RecyclerView.LayoutParams) child.getLayoutParams()).topMargin
                    + ((RecyclerView.LayoutParams) child.getLayoutParams()).bottomMargin;

            int childLeft = 0;

            switch (layoutPath) {
                case CIRCLE:
                    childLeft = calculateEllipseXFromY(childTop + (getDecoratedMeasuredHeight(child) +
                            getTopDecorationHeight(child) - getBottomDecorationHeight(child) + sumOfVerticalMargins) / 2);
                    break;
                case ELLIPSE:
                    childLeft = calculateCircleXFromY(childTop + (getDecoratedMeasuredHeight(child) +
                            getTopDecorationHeight(child) - getBottomDecorationHeight(child) + sumOfVerticalMargins) / 2);
                    break;
            }

            if (!(recyclerBounds.intersects(recyclerBounds.left + childLeft, recyclerBounds.top + childTop,
                    recyclerBounds.left + childLeft + getDecoratedMeasuredWidth(child) + sumOfHorizontalMargins,
                    recyclerBounds.top + childTop + getDecoratedMeasuredHeight(child) + sumOfVerticalMargins)
                    || recyclerBounds.contains(recyclerBounds.left + childLeft, recyclerBounds.top + childTop,
                    recyclerBounds.left + childLeft + getDecoratedMeasuredWidth(child) + sumOfHorizontalMargins,
                    recyclerBounds.top + childTop + getDecoratedMeasuredHeight(child) + sumOfVerticalMargins))) {
                break;
            }

            addView(child);

            layoutDecoratedWithMargins(child, childLeft, childTop, childLeft + getDecoratedMeasuredWidth(child)
                    + sumOfHorizontalMargins, childTop + getDecoratedMeasuredHeight(child) + sumOfVerticalMargins);

            getDecoratedBoundsWithMargins(child, childDecoratedBoundsWithMargin);

            scaleChild(child);

            childTop += childDecoratedBoundsWithMargin.height();
        }

        List<RecyclerView.ViewHolder> scrapList = recycler.getScrapList();
        for (int i = 0; i < scrapList.size(); i++) {
            View viewRemoved = scrapList.get(i).itemView;
            recycler.recycleView(viewRemoved);
        }

        if (!scrolled) {
            stabilize();
        }
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public void onScrollStateChanged(int state) {
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            stabilize();
        }
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (!scrolled) {
            scrolled = true;
        }

        int delta = dy;

        if (delta > 150) {
            delta = 150;
        }

        if (delta < -150) {
            delta = -150;
        }

        if (getChildCount() == 0) {
            return dy;
        }

        if (getPosition(getChildAt(getChildCount() - 1)) == getItemCount() - 1) {

            View child = getChildAt(getChildCount() - 1);
            getDecoratedBoundsWithMargins(child, childDecoratedBoundsWithMargin);

            if (childDecoratedBoundsWithMargin.bottom - delta < recyclerBounds.height()) {
                int position = recyclerBounds.height();
                int indexToStartFill = getPosition(getChildAt(0));

                for (int i = getChildCount() - 1; i >= 0; i--) {
                    getDecoratedBoundsWithMargins(getChildAt(i), childDecoratedBoundsWithMargin);

                    position -= childDecoratedBoundsWithMargin.height();

                    if (position <= 0) {
                        topOfFirstChild = position;
                        if (topOfFirstChild <= -childDecoratedBoundsWithMargin.height()) {
                            topOfFirstChild += childDecoratedBoundsWithMargin.height();
                        }
                        indexToStartFill = getPosition(getChildAt(i));
                        if (indexToStartFill >= getItemCount()) {
                            indexToStartFill = getItemCount() - 1;
                        }
                        break;
                    }
                }

                fill(indexToStartFill, recycler);
                return 0;
            }
        }

        topOfFirstChild -= delta;

        getDecoratedBoundsWithMargins(getChildAt(0), childDecoratedBoundsWithMargin);

        int indexToStartFill = getPosition(getChildAt(0));

        if (topOfFirstChild > 0) {
            topOfFirstChild -= childDecoratedBoundsWithMargin.height();
            indexToStartFill--;
            if (indexToStartFill == -1) {
                topOfFirstChild = 0;
                fill(0, recycler);
                return 0;
            }
        } else if (topOfFirstChild <= -childDecoratedBoundsWithMargin.height()) {
            topOfFirstChild += childDecoratedBoundsWithMargin.height();
            indexToStartFill++;
        }

        fill(indexToStartFill, recycler);

        return dy;
    }

    /**
     * Scales the width and height of a child view depending on it's vertical positioning.
     *
     * @param child Child View to be scaled.
     */

    private void scaleChild(View child) {
        int y = (child.getTop() + child.getBottom()) / 2;
        float scale = 1 - (Math.abs(verticalCenter - y) / (float) (recyclerBounds.height() - child.getHeight()));

        child.setPivotX(0);

        child.setScaleX(scale);
        child.setScaleY(scale);
    }

    /**
     * This function calculates horizontal position of child view depending on it's vertical position
     * using the circle equation.
     *
     * @param y Vertical positioning of the child view.
     * @return Horizontal positioning of the child view.
     */

    private int calculateCircleXFromY(int y) {
        int centerY = verticalCenter;
        return (int) (Math.sqrt((radius * radius) - ((y - centerY) * (y - centerY))) + centerX);
    }

    /**
     * This function calculates horizontal position of child view depending on it's vertical position
     * using the circle equation.
     *
     * @param y Vertical positioning of the child view.
     * @return Horizontal positioning of the child view.
     */

    private int calculateEllipseXFromY(int y) {
        int centerY = verticalCenter;
        return (int) (Math.sqrt((1 - (((y - centerY) * (y - centerY)) / (minorRadius * minorRadius))) * (majorRadius * majorRadius)) + centerX);
    }

    /**
     * This function is responsible for centering of the list items on idle scroll state with
     * reference to a vertical center.
     */

    public void stabilize() {
        int minDistance = Integer.MAX_VALUE;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int y = (child.getTop() + child.getBottom()) / 2;
            if (Math.abs(y - verticalCenter) < Math.abs(minDistance)) {
                minDistance = y - verticalCenter;
            } else {
                break;
            }
        }
        recyclerView.smoothScrollBy(0, minDistance);
    }
}
