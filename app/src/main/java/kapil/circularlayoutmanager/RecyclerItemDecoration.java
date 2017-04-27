package kapil.circularlayoutmanager;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

/**
 * Item decorator for recycler view. Adds margin to the outermost children of the list to induce an
 * over scroll effect with drag.
 */

class RecyclerItemDecoration extends RecyclerView.ItemDecoration {
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.top = (int) DpToPx(parent.getContext(), 5);
        outRect.bottom = (int) DpToPx(parent.getContext(), 5);

        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = (int) DpToPx(parent.getContext(), 100);
        }
        if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1) {
            outRect.bottom = (int) DpToPx(parent.getContext(), 100);
        }
    }

    private float DpToPx(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
