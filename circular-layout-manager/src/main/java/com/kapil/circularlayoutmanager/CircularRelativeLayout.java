package com.kapil.circularlayoutmanager;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.os.Build;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * It is a Relative Layout clipped into a circle or an ellipse depending upon it's width and height.
 *
 * It also provides functionality to set width and height equal in case of match_parent
 * initialization of one the parameters depending on the value of primaryDimension.
 */

public class CircularRelativeLayout extends RelativeLayout {
    private static final int WIDTH = 0;
    private static final int HEIGHT = 1;
    private static final int NONE = 2;

    private Path ovalPath;
    private @Dimension int primaryDimension;

    @IntDef({WIDTH, HEIGHT, NONE})
    @interface Dimension {

    }

    public CircularRelativeLayout(Context context) {
        super(context);
        init();
    }

    public CircularRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        primaryDimension = WIDTH;

        ovalPath = new Path();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        switch (primaryDimension) {
            case WIDTH:
                getLayoutParams().height = getMeasuredWidth();
                break;
            case HEIGHT:
                getLayoutParams().width = getMeasuredHeight();
                break;
            case NONE:
                break;
        }

        ovalPath.reset();
        ovalPath.addOval(0, 0, getMeasuredWidth(), getMeasuredHeight(), Path.Direction.CW);
        ovalPath.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.clipPath(ovalPath);
        super.onDraw(canvas);
    }

    public int getPrimaryDimension() {
        return primaryDimension;
    }

    /**
     * Sets primary dimension of the view so that it's value can be set to the other dimension of
     * the layout.
     *
     * @param primaryDimension Can be one of WIDTH, HEIGHT and NONE.
     *
     *                         When WIDTH is set as the primary dimension, the value of the width of
     *                         the layout is taken and set as height of the layout to form a square.
     *
     *                         When HEIGHT is set as the primary dimension, the value of the height
     *                         of the layout is taken and set as width of the layout to form a square.
     *
     *                         When NONE is set as the primary dimension, the value of neither width
     *                         nor height are altered.
     */

    public void setPrimaryDimension(@Dimension int primaryDimension) {
        this.primaryDimension = primaryDimension;
    }
}
