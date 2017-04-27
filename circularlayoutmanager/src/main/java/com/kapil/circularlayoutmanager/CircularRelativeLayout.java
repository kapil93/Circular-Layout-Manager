package com.kapil.circularlayoutmanager;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.os.Build;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * It is a Relative Layout clipped into a circle or an ellipse depending upon it's width and height.
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

    public void setPrimaryDimension(int primaryDimension) {
        this.primaryDimension = primaryDimension;
    }
}
