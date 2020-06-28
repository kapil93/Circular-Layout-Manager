package kapil.circularlayoutmanager

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener

/**
 * Item click listener for recycler view which uses [GestureDetector] to detect a single tap
 * on a list item and provides a callback for the same.
 */
internal class OnRecyclerItemClickListener(
    context: Context?,
    private val clickListener: OnItemClickListener
) : OnItemTouchListener {

    private val gestureDetector: GestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return true
        }
    })

    internal interface OnItemClickListener {
        fun onItemClick(parent: RecyclerView?, childIndex: Int)
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        val childView = rv.findChildViewUnder(e.x, e.y)
        if (gestureDetector.onTouchEvent(e)) {
            val childIndex = rv.indexOfChild(childView)
            if (childIndex != -1) {
                clickListener.onItemClick(rv, childIndex)
            }
            return true
        }
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
}