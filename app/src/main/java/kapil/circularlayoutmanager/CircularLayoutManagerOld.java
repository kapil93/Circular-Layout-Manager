package kapil.circularlayoutmanager;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Deprecated
public class CircularLayoutManagerOld extends RecyclerView.LayoutManager {

    Activity activity;
    RecyclerView recyclerView;
    long xCenter, yCenter, radius, a, b;
    boolean isCircle, pointsGenerated;

    HashMap<Integer, Point> mapWithKey_Index;
    HashMap<Point, Integer> mapWithKey_Point;
    int lastIndexInMap;
    List<Point> childPoints;
    Rect recyclerBounds;
    int heightOfView, widthOfView;
    RecyclerView.Recycler mRecycler;
    RecyclerView.State mState;
    int screenWidth;
    float baseScale;

    public CircularLayoutManagerOld(Activity activity, RecyclerView recyclerView, long xCenter, long yCenter, long radius) {
        this.activity = activity;
        this.recyclerView = recyclerView;
        this.xCenter = xCenter;
        this.yCenter = yCenter;
        this.radius = radius;

        childPoints = new ArrayList<>();

        isCircle = true;

        pointsGenerated = false;
    }

    public CircularLayoutManagerOld(Activity activity, RecyclerView recyclerView, long xCenter, long yCenter, long a, long b) {
        this.activity = activity;
        this.recyclerView = recyclerView;
        this.xCenter = xCenter;
        this.yCenter = yCenter;
        this.a = a;
        this.b = b;

        childPoints = new ArrayList<>();

        isCircle = false;

        pointsGenerated = false;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    private void setRange(RecyclerView.Recycler recycler) {
        View view = recycler.getViewForPosition(0);
        measureChildWithMargins(view, 0, 0);
        heightOfView = getDecoratedMeasuredHeight(view);
        widthOfView = getDecoratedMeasuredWidth(view);

        int top = 0 - (2 * heightOfView);
        int bottom = recyclerView.getHeight() + (2 * heightOfView);
        int left = 0 - (2 * widthOfView);
        int right = recyclerView.getWidth() + (2 * widthOfView);

        recyclerBounds = new Rect(left, top, right, bottom);
    }

    private void generateCirclePoints() {

        List<Point> circlePoints = new ArrayList<>();
        mapWithKey_Index = new HashMap<>();
        mapWithKey_Point = new HashMap<>();
        int index = 0;

        long x = radius;
        long y = 0;
        int err = 0;

        while (x >= y) {
            Point point = new Point((int) (xCenter + x), (int) (yCenter + y));
            circlePoints.add(point);

            if (recyclerBounds.contains(point.x, point.y)) {
                mapWithKey_Index.put(index, point);
                mapWithKey_Point.put(point, index);
                index++;
            }

            y += 1;
            err += 1 + 2 * y;
            if (2 * (err - x) + 1 > 0) {
                x -= 1;
                err += 1 - 2 * x;
            }
        }

        for (int i = (circlePoints.size() - 1); i >= 0; i--) {

            Point newPoint = new Point(circlePoints.get(i).y - (int) yCenter + (int) xCenter, circlePoints.get(i).x - (int) xCenter + (int) yCenter);
            circlePoints.add(newPoint);

            if ((recyclerBounds.contains(newPoint.x, newPoint.y)) && (!mapWithKey_Point.containsKey(newPoint))) {
                mapWithKey_Index.put(index, newPoint);
                mapWithKey_Point.put(newPoint, index);
                index++;
            }
        }

        for (int i = (circlePoints.size() - 1); i >= 0; i--) {

            Point newPoint = new Point((2 * (int) xCenter) - circlePoints.get(i).x, circlePoints.get(i).y);
            circlePoints.add(newPoint);

            if ((recyclerBounds.contains(newPoint.x, newPoint.y)) && (!mapWithKey_Point.containsKey(newPoint))) {
                mapWithKey_Index.put(index, newPoint);
                mapWithKey_Point.put(newPoint, index);
                index++;
            }
        }

        for (int i = (circlePoints.size() - 1); i >= 0; i--) {
            Point newPoint = new Point(circlePoints.get(i).x, (2 * (int) yCenter) - circlePoints.get(i).y);

            if ((recyclerBounds.contains(newPoint.x, newPoint.y)) && (!mapWithKey_Point.containsKey(newPoint))) {
                mapWithKey_Index.put(index, newPoint);
                mapWithKey_Point.put(newPoint, index);
                index++;
            }
        }

        lastIndexInMap = index - 1;
        pointsGenerated = true;
    }

    void generateEllipsePoints() {

        List<Point> ellipsePoints = new ArrayList<>();
        mapWithKey_Index = new HashMap<>();
        mapWithKey_Point = new HashMap<>();
        int index = 0;

        long xrad2, yrad2, twoxrad2, twoyrad2;
        long x, y, dp, dpx, dpy;

        xrad2 = a * a;
        yrad2 = b * b;

        twoxrad2 = 2 * xrad2;
        twoyrad2 = 2 * yrad2;
        y = dpy = 0;
        x = a;
        dpx = twoyrad2 * x;

        Point mPoint = new Point((int) (xCenter + x), (int) (yCenter + y));
        ellipsePoints.add(mPoint);

        if (recyclerBounds.contains(mPoint.x, mPoint.y)) {
            mapWithKey_Index.put(index, mPoint);
            mapWithKey_Point.put(mPoint, index);
            index++;
        }

        dp = (long) (0.5 + xrad2 - (yrad2 * a) + (0.25 * yrad2));

        while (dpy < dpx) {
            y = y + 1;
            dpy = dpy + twoxrad2;
            if (dp < 0) {
                dp = dp + xrad2 + dpy;
            } else {
                x = x - 1;
                dpx = dpx - twoyrad2;
                dp = dp + xrad2 + dpy - dpx;
            }

            Point point = new Point((int) (xCenter + x), (int) (yCenter + y));
            ellipsePoints.add(point);

            if (recyclerBounds.contains(point.x, point.y)) {
                mapWithKey_Index.put(index, point);
                mapWithKey_Point.put(point, index);
                index++;
            }
        }

        dp = (long) (0.5 + xrad2 * (y + 0.5) * (y + 0.5) +
                yrad2 * (x - 1) * (x - 1) - yrad2 * xrad2);

        while (x > 0) {
            x = x - 1;
            dpx = dpx - twoyrad2;

            if (dp > 0) {
                dp = dp + yrad2 - dpx;
            } else {
                y = y + 1;
                dpy = dpy + twoxrad2;
                dp = dp + yrad2 - dpx + dpy;
            }

            Point point = new Point((int) (xCenter + x), (int) (yCenter + y));
            ellipsePoints.add(point);

            if (recyclerBounds.contains(point.x, point.y)) {
                mapWithKey_Index.put(index, point);
                mapWithKey_Point.put(point, index);
                index++;
            }
        }

        for (int i = (ellipsePoints.size() - 1); i >= 0; i--) {

            Point newPoint = new Point((2 * (int) xCenter) - ellipsePoints.get(i).x, ellipsePoints.get(i).y);
            ellipsePoints.add(newPoint);

            if ((recyclerBounds.contains(newPoint.x, newPoint.y)) && (!mapWithKey_Point.containsKey(newPoint))) {
                mapWithKey_Index.put(index, newPoint);
                mapWithKey_Point.put(newPoint, index);
                index++;
            }
        }

        for (int i = (ellipsePoints.size() - 1); i >= 0; i--) {
            Point newPoint = new Point(ellipsePoints.get(i).x, (2 * (int) yCenter) - ellipsePoints.get(i).y);

            if ((recyclerBounds.contains(newPoint.x, newPoint.y)) && (!mapWithKey_Point.containsKey(newPoint))) {
                mapWithKey_Index.put(index, newPoint);
                mapWithKey_Point.put(newPoint, index);
                index++;
            }
        }

        lastIndexInMap = index - 1;
        pointsGenerated = true;
    }

    private void measureScreen() {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;

        baseScale = (float) screenWidth*(1f/720f);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

        if (!pointsGenerated) {
            measureScreen();

            mRecycler = recycler;
            mState = state;

            setRange(recycler);

            if (isCircle) {
                generateCirclePoints();
            } else {
                generateEllipsePoints();
            }
        }

        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }

        if (getChildCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            childPoints.clear();
            fill(0, "DOWN", recycler, state);
        } else {
            if (childPoints.get(childPoints.size() - 1).y < (recyclerBounds.bottom - (2 * heightOfView))) {
                fill(getPosition(getChildAt(getChildCount() - 1)) + 1, "DOWN", recycler, state);
            }
        }
    }

    private void fill(int positionToStartFill, String directionOfFill, RecyclerView.Recycler recycler, RecyclerView.State state) {

        int startingIndex = 0;
        int startPosition = positionToStartFill;

        switch (directionOfFill) {
            case "DOWN":
                if ((positionToStartFill < getItemCount()) && (positionToStartFill >= 0)) {
                    if (childPoints.isEmpty()) {
                        startingIndex = 0;
                        startPosition = 0;
                    } else {
                        startingIndex = mapWithKey_Point.get(childPoints.get(0));
                        startPosition = getPosition(getChildAt(0));
                    }
                    childPoints.clear();
                    detachAndScrapAttachedViews(recycler);
                }
                break;

            case "UP":

                for (int i = positionToStartFill; ((i < getItemCount()) && (i >= 0)); i--) {

                    int index = mapWithKey_Point.get(childPoints.get(0)) - heightOfView;

                    if (index > lastIndexInMap) {
                        index -= lastIndexInMap;
                    } else if (index < 0) {
                        index += lastIndexInMap;
                    }

                    Point point = mapWithKey_Index.get(index);
                    childPoints.add(0, point);

                    if (point.y < (recyclerBounds.top + (2 * heightOfView))) {
                        startPosition = i;
                        break;
                    }
                }

                if ((positionToStartFill < getItemCount()) && (positionToStartFill >= 0)) {
                    startingIndex = mapWithKey_Point.get(childPoints.get(0));
                    childPoints.clear();
                    detachAndScrapAttachedViews(recycler);
                }
                break;
        }

        for (int i = startPosition; ((i < getItemCount()) && (i >= 0)); i++) {
            View view = recycler.getViewForPosition(i);

            measureChildWithMargins(view, 0, 0);

            int index;
            if (childPoints.isEmpty()) {
                index = startingIndex;
            } else {
                index = mapWithKey_Point.get(childPoints.get(childPoints.size() - 1)) + heightOfView;
            }
            if (index > lastIndexInMap) {
                index -= lastIndexInMap;
            } else if (index < 0) {
                index += lastIndexInMap;
            }

            Point point = mapWithKey_Index.get(index);

            addView(view);
            childPoints.add(point);

            view.setScaleX(baseScale);
            view.setScaleY(baseScale);

            float scale = 1.5f*Math.abs((float) (mapWithKey_Index.get(0).y - point.y)) / ((float) (recyclerBounds.height() - (2 * heightOfView)));
            view.setScaleX(baseScale/(1+(scale)));
            view.setScaleY(baseScale/(1+(scale)));

            layoutDecorated(view, getCoordinate(point, "left"),
                    getCoordinate(point, "top"),
                    getCoordinate(point, "right"),
                    getCoordinate(point, "bottom"));

            if (point.y > (recyclerBounds.bottom - (2 * heightOfView))) {
                break;
            }
        }
        List<RecyclerView.ViewHolder> scrapList = recycler.getScrapList();
        for (int i = 0; i < scrapList.size(); i++) {
            View viewRemoved = scrapList.get(i).itemView;
            recycler.recycleView(viewRemoved);
        }
    }

    private int getCoordinate(Point point, String desiredCoordinate) {
        switch (desiredCoordinate) {
            case "top":
                return point.y - (heightOfView / 2);

            case "bottom":
                return point.y + (heightOfView / 2);

            case "left":
                return point.x - (widthOfView / 2);

            case "right":
                return point.x + (widthOfView / 2);
        }
        return 0;
    }

    @Override
    public void onScrollStateChanged(int state) {
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            int minimumDistanceFromCenter = 10000;
            Point nearestPoint = childPoints.get(0);
            for (int i = 0; i < childPoints.size(); i++) {
                if (Math.abs(childPoints.get(i).y - mapWithKey_Index.get(0).y) < minimumDistanceFromCenter) {
                    minimumDistanceFromCenter = Math.abs(childPoints.get(i).y - mapWithKey_Index.get(0).y);
                    nearestPoint = childPoints.get(i);
                }
            }
            final int delta = (nearestPoint.y - mapWithKey_Index.get(0).y);
            recyclerView.smoothScrollBy(0, delta);
        }
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {

        int delta = dy, stopScrollFlag = 0;

        if (delta > 150) {
            delta = 150;
        }

        if (delta < -150) {
            delta = -150;
        }

        if (delta < (childPoints.get(0).y - mapWithKey_Index.get(0).y)) {
            delta = childPoints.get(0).y - mapWithKey_Index.get(0).y;
            stopScrollFlag = 1;
        }
        if (delta > (childPoints.get(childPoints.size() - 1).y - mapWithKey_Index.get(0).y)) {
            delta = (childPoints.get(childPoints.size() - 1).y - mapWithKey_Index.get(0).y);
            stopScrollFlag = 1;
        }

        offsetChildren(delta);

        if (childPoints.get(0).y > (recyclerBounds.top + (2 * heightOfView))) {
            fill(getPosition(getChildAt(0)) - 1, "UP", recycler, state);
        }

        if (childPoints.get(childPoints.size() - 1).y < (recyclerBounds.bottom - (2 * heightOfView))) {
            fill(getPosition(getChildAt(getChildCount() - 1)) + 1, "DOWN", recycler, state);
        }

        if (childPoints.get(0).y < (recyclerBounds.top + heightOfView)) {
            detachAndScrapView(getChildAt(0), recycler);
            childPoints.remove(0);
        }

        if (childPoints.get(childPoints.size() - 1).y > (recyclerBounds.bottom - heightOfView)) {
            detachAndScrapView(getChildAt(getChildCount() - 1), recycler);
            childPoints.remove(childPoints.size() - 1);
        }

        List<RecyclerView.ViewHolder> scrapList = recycler.getScrapList();
        for (int i = 0; i < scrapList.size(); i++) {
            View viewRemoved = scrapList.get(i).itemView;
            recycler.recycleView(viewRemoved);
        }

        if (stopScrollFlag == 1) {
            return 0;
        }
        return delta;
    }

    private void offsetChildren(int delta) {
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);

            measureChildWithMargins(view, 0, 0);

            Point point = childPoints.get(i);

            int index = mapWithKey_Point.get(point) - delta;

            if (index > lastIndexInMap) {
                index -= lastIndexInMap;
            } else if (index < 0) {
                index += lastIndexInMap;
            }

            Point newPoint = mapWithKey_Index.get(index);
            childPoints.set(i, newPoint);

            view.setScaleX(baseScale);
            view.setScaleY(baseScale);

            float scale = 1.5f*Math.abs((float) (mapWithKey_Index.get(0).y - newPoint.y)) / ((float) (recyclerBounds.height() - (2 * heightOfView)));
            view.setScaleX(baseScale/(1+(scale)));
            view.setScaleY(baseScale/(1+(scale)));

            layoutDecorated(view, getCoordinate(newPoint, "left"),
                    getCoordinate(newPoint, "top"),
                    getCoordinate(newPoint, "right"),
                    getCoordinate(newPoint, "bottom"));
        }
    }
}
