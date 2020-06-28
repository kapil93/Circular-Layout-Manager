package kapil.circularlayoutmanager

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.kapil.circularlayoutmanager.Utils.dpToPx

/**
 * Item decorator for recycler view. Adds margin to the outermost children of the list to induce an
 * over scroll effect with drag.
 */
internal class RecyclerItemDecoration : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.top = dpToPx(parent.context, 5f).toInt()
        outRect.bottom = dpToPx(parent.context, 5f).toInt()
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = dpToPx(parent.context, 100f).toInt()
        }
        if (parent.getChildAdapterPosition(view) == parent.adapter!!.itemCount - 1) {
            outRect.bottom = dpToPx(parent.context, 100f).toInt()
        }
    }
//
//    /**
//     * Function to convert a value given in dp to pixels (px).
//     *
//     * @param context Current context, used to access resources.
//     * @param dp      The value (in dp) to be converted.
//     * @return        The value in pixels.
//     */
//    private fun dpToPx(context: Context, dp: Float): Float {
//        return TypedValue.applyDimension(
//            TypedValue.COMPLEX_UNIT_DIP,
//            dp,
//            context.resources.displayMetrics
//        )
//    }
}