package kapil.circularlayoutmanager

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

/**
 * Item decorator for recycler view. Adds margin to the outermost children of the list to induce an
 * over scroll effect with drag.
 */
class RecyclerItemDecoration : ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.top = parent.context.resources.getDimension(R.dimen.margin_xs).toInt()
        outRect.bottom = parent.context.resources.getDimension(R.dimen.margin_xs).toInt()
        if (parent.getChildAdapterPosition(view) == 0) outRect.top =
            parent.context.resources.getDimension(R.dimen.margin_xxxl).toInt()
        if (parent.getChildAdapterPosition(view) == parent.adapter!!.itemCount - 1)
            outRect.bottom = parent.context.resources.getDimension(R.dimen.margin_xxxl).toInt()
    }
}