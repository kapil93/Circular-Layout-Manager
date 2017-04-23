package kapil.circularlayoutmanager;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

import java.util.List;

/**
 * This is a custom layout manager for recycler view which displays list items in a circular or
 * elliptical fashion.
 */

public class CircularLayoutManager extends RecyclerView.LayoutManager {
    private RecyclerView recyclerView;
    private Rect recyclerBounds;

    private int topOfFirstChild;

    private Rect childDecoratedBoundsWithMargin;
    private int verticalCenter;
    private boolean scrolled;

    private float radius;
    private float a, b;
    private float centerX;

    public CircularLayoutManager(Context context, int radius, int centerX) {
        this.radius = DpToPx(context, radius);
        this.centerX = DpToPx(context, centerX);
    }

    public CircularLayoutManager(Context context, int a, int b, int centerX) {
        this.a = DpToPx(context, a);
        this.b = DpToPx(context, b);
        this.centerX = DpToPx(context, centerX);
    }

    private float DpToPx(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
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

            int childLeft;
            if (radius == 0) {
                childLeft = calculateEllipseXFromY((childTop + (childTop + getDecoratedMeasuredHeight(child) + sumOfVerticalMargins)) / 2);
            } else {
                childLeft = calculateCircleXFromY((childTop + (childTop + getDecoratedMeasuredHeight(child) + sumOfVerticalMargins)) / 2);
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

    private void scaleChild(View child) {
        int y = (child.getTop() + child.getBottom()) / 2;
        float scale = 1 - (Math.abs(verticalCenter - y) / (float) recyclerBounds.height());

        child.setPivotX(0);

        child.setScaleX(scale);
        child.setScaleY(scale);
    }

    private int calculateCircleXFromY(int y) {
        int centerY = verticalCenter;

        return (int) (Math.sqrt((radius * radius) - ((y - centerY) * (y - centerY))) + centerX);
    }

    private int calculateEllipseXFromY(int y) {
        int centerY = verticalCenter;

        return (int) (Math.sqrt((1 - (((y - centerY) * (y - centerY)) / (b * b))) * (a * a)) + centerX);
    }

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
}
