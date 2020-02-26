package kapil.circularlayoutmanager;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Item click listener for recycler view which uses {@link GestureDetector} to detect a single tap
 * on a list item and provides a callback for the same.
 */

class OnRecyclerItemClickListener implements RecyclerView.OnItemTouchListener {

    private OnItemClickListener clickListener;
    private GestureDetector gestureDetector;

    interface OnItemClickListener {
        void OnItemClick(RecyclerView parent, int childIndex);
    }

    OnRecyclerItemClickListener(Context context, OnItemClickListener clickListener) {
        this.clickListener = clickListener;
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        View childView = rv.findChildViewUnder(e.getX(), e.getY());
        if (gestureDetector.onTouchEvent(e)) {
            int childIndex = rv.indexOfChild(childView);
            if (childIndex != -1) {
                clickListener.OnItemClick(rv, childIndex);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }
}
