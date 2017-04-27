package com.kapil.circularlayoutmanager;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * This view implements scroll wheel functionality.
 *
 * It converts circular touch motion into input for the scrolling of a recycler view.
 */

public class ScrollWheel extends View implements GestureDetector.OnGestureListener {
    private GestureDetector gestureDetector;
    private RecyclerView recyclerView;

    private OnItemClickListener onItemClickListener;

    private boolean scrollWheelEnabled;
    private boolean consumeTouchOutsideTouchAreaEnabled;

    private int xCenter;
    private int yCenter;
    private int innerRadius;
    private int outerRadius;
    private int touchAreaThickness;
    private boolean touchInitiatedBetweenCircles;

    private Paint touchAreaPaint;
    private boolean highlightTouchAreaEnabled;

    public ScrollWheel(Context context) {
        super(context);
        init();
    }

    public ScrollWheel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScrollWheel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gestureDetector = new GestureDetector(getContext(), this);

        scrollWheelEnabled = true;
        consumeTouchOutsideTouchAreaEnabled = true;

        touchAreaThickness = -1;

        touchAreaPaint = new Paint();
        touchAreaPaint.setAntiAlias(true);
        touchAreaPaint.setStyle(Paint.Style.STROKE);
        touchAreaPaint.setColor(Color.parseColor("#50FF0000"));

        highlightTouchAreaEnabled = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        xCenter = getMeasuredWidth() / 2;
        yCenter = getMeasuredHeight() / 2;

        int minDimension = Math.min(getMeasuredWidth(), getMeasuredHeight());


        outerRadius = minDimension / 2;
        innerRadius = touchAreaThickness == -1 ? minDimension / 4 : outerRadius - touchAreaThickness;

        touchAreaPaint.setStrokeWidth(outerRadius - innerRadius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (highlightTouchAreaEnabled) {
            canvas.drawCircle(xCenter, yCenter, (innerRadius + outerRadius) / 2, touchAreaPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if ((recyclerView == null) || (!scrollWheelEnabled)) {
            return super.onTouchEvent(event);
        }

        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            ((CircularLayoutManager) recyclerView.getLayoutManager()).stabilize();
        }

        return gestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {

        float x = e.getX();
        float y = e.getY();

        touchInitiatedBetweenCircles = ((((x - xCenter) * (x - xCenter)) + ((y - yCenter) * (y - yCenter))) > (innerRadius * innerRadius))
                && ((((x - xCenter) * (x - xCenter)) + ((y - yCenter) * (y - yCenter))) < (outerRadius * outerRadius));

        return consumeTouchOutsideTouchAreaEnabled || touchInitiatedBetweenCircles;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if ((onItemClickListener != null) && (consumeTouchOutsideTouchAreaEnabled)) {
            int childIndex = getChildIndexUnder(e.getX(), e.getY());
            if (childIndex != -1) {
                onItemClickListener.onItemClick(ScrollWheel.this, childIndex);
            }
        }
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (!touchInitiatedBetweenCircles) {
            return false;
        }

        int delta = 0;

        float x = e2.getX();
        float y = e2.getY();

        if ((x <= xCenter) && (y < yCenter)) {
            delta = (int) (distanceX - distanceY);
        } else if ((x > xCenter) && (y <= yCenter)) {
            delta = (int) (distanceX + distanceY);
        } else if ((x >= xCenter) && (y > yCenter)) {
            delta = (int) (-distanceX + distanceY);
        } else if ((x < xCenter) && (y >= yCenter)) {
            delta = (int) (-distanceX - distanceY);
        }

        recyclerView.scrollBy(0, delta);

        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if ((onItemClickListener != null) && (consumeTouchOutsideTouchAreaEnabled)) {
            int childIndex = getChildIndexUnder(e.getX(), e.getY());
            if (childIndex != -1) {
                onItemClickListener.onItemLongClick(ScrollWheel.this, childIndex);
            }
        }
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (!touchInitiatedBetweenCircles) {
            return false;
        }

        int delta = 0;

        float x = e2.getX();
        float y = e2.getY();

        if ((x <= xCenter) && (y < yCenter)) {
            delta = (int) (velocityX - velocityY);
        } else if ((x > xCenter) && (y <= yCenter)) {
            delta = (int) (velocityX + velocityY);
        } else if ((x >= xCenter) && (y > yCenter)) {
            delta = (int) (-velocityX + velocityY);
        } else if ((x < xCenter) && (y >= yCenter)) {
            delta = (int) (-velocityX - velocityY);
        }

        recyclerView.fling(0, -delta);

        return true;
    }

    int getChildIndexUnder(float x, float y) {
        View child = recyclerView.findChildViewUnder(x, y);
        if (child != null) {
            return recyclerView.indexOfChild(child);
        }
        return -1;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    /**
     * Setter function for recycler view.
     *
     * @param recyclerView Instance of recycler view that will be scrolled using scroll wheel.
     */

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public boolean isScrollWheelEnabled() {
        return scrollWheelEnabled;
    }

    /**
     * Function to toggle scroll wheel functionality.
     *
     * By default set to true.
     *
     * @param scrollWheelEnabled true or false.
     */

    public void setScrollWheelEnabled(boolean scrollWheelEnabled) {
        this.scrollWheelEnabled = scrollWheelEnabled;
    }

    public boolean isConsumeTouchOutsideTouchAreaEnabled() {
        return consumeTouchOutsideTouchAreaEnabled;
    }

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
     * @param consumeTouchOutsideTouchAreaEnabled true or false.
     */

    public void setConsumeTouchOutsideTouchAreaEnabled(boolean consumeTouchOutsideTouchAreaEnabled) {
        this.consumeTouchOutsideTouchAreaEnabled = consumeTouchOutsideTouchAreaEnabled;
    }

    public int getTouchAreaThickness() {
        return touchAreaThickness;
    }

    public void setTouchAreaThickness(int thickness) {
        touchAreaThickness = (int) Utils.dpToPx(getContext(), thickness);
        invalidate();
    }

    public int getTouchAreaColor() {
        return touchAreaPaint.getColor();
    }

    public void setTouchAreaColor(int touchAreaColor) {
        touchAreaPaint.setColor(touchAreaColor);
        invalidate();
    }

    public boolean isHighlightTouchAreaEnabled() {
        return highlightTouchAreaEnabled;
    }

    /**
     * Function to show or hide path of action of the scroll wheel. Enabling it will highlight an
     * area on the screen as a cue for the user to use the scroll wheel.
     *
     * By default set to true.
     *
     * @param highlightTouchAreaEnabled true or false.
     */

    public void setHighlightTouchAreaEnabled(boolean highlightTouchAreaEnabled) {
        this.highlightTouchAreaEnabled = highlightTouchAreaEnabled;
        invalidate();
    }
}
