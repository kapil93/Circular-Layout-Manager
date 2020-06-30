package kapil.circularlayoutmanager

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kapil.circularlayoutmanager.CircularLayoutManagerNew
import com.kapil.circularlayoutmanager.INVALID_INDEX
import com.kapil.circularlayoutmanager.getChildAdapterPosition
import kotlinx.android.synthetic.main.activity_main.*

/**
 * An example activity implementing a recycler view with a circular layout manager and scroll wheel.
 *
 * In this example, the scroll wheel touch area partially lays over the recycler view.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeRecyclerView()
        initializeScrollWheel()

        addItemButton!!.setOnClickListener { addItemToList() }
        scrollWheelToggleButton!!.setOnClickListener {
            recyclerView.scrollToPosition(4)
//            toggleScrollWheel()
        }
    }

    private fun initializeRecyclerView() {
        recyclerView.adapter = RecyclerViewAdapter().apply {
            submitList(getInitialList())
            onItemClickListener = { showMessage(event) }
        }
        recyclerView.addItemDecoration(RecyclerItemDecoration())
        recyclerView.layoutManager = CircularLayoutManagerNew(
            resources.getDimension(R.dimen.circular_list_radius),
            resources.getDimension(R.dimen.circular_list_center_x)
        ).apply { shouldIgnoreHeaderAndFooterMargins = true }
    }

    private fun initializeScrollWheel() {
        scrollWheel.isEnabled = false
        scrollWheel.isHighlightTouchAreaEnabled = false
//        scrollWheel.isHandleClicksEnabled = false
        scrollWheel.onItemClickListener = { x, y ->
            val index = recyclerView.getChildAdapterPosition(x, y)
            if (index != INVALID_INDEX) showMessage("OC " + getList()[index].event)
        }
        scrollWheel.onItemLongClickListener = { x, y ->
            val index = recyclerView.getChildAdapterPosition(x, y)
            if (index != INVALID_INDEX) showMessage("OLC " + getList()[index].event)
        }
        scrollWheel.onScrollListener = { recyclerView.scrollBy(0, it.toInt()) }
        scrollWheel.onFlingListener = { recyclerView.fling(0, it.toInt()) }
        scrollWheel.onTouchReleasedListener = {
//            (recyclerView!!.layoutManager as CircularLayoutManager).stabilize()
        }
    }

    private fun addItemToList() {
        (recyclerView.adapter as RecyclerViewAdapter).apply {
            submitList(currentList.toMutableList().apply {
                add(Model(size + 1, "Event ${size + 1}", "12:00am - 12:00pm"))
            }) { recyclerView.invalidateItemDecorations() }
        }
    }

    private fun toggleScrollWheel() {
        scrollWheel!!.isEnabled = !scrollWheel!!.isEnabled
        scrollWheel!!.isHighlightTouchAreaEnabled = !scrollWheel!!.isHighlightTouchAreaEnabled
    }

    private fun getInitialList() = (1..10).map { Model(it, "Event $it", "12:00am - 12:00pm") }

    private fun getList() = (recyclerView.adapter as RecyclerViewAdapter).currentList

    private fun showMessage(msg: String) =
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
}