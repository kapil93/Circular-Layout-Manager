package kapil.circularlayoutmanager

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kapil.circularlayoutmanager.CircularLayoutManager
import com.kapil.circularlayoutmanager.OnItemClickListener
import com.kapil.circularlayoutmanager.ScrollWheel
import java.util.*

/**
 * An example activity implementing a recycler view with a circular layout manager and scroll wheel.
 */
class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var recyclerView: RecyclerView? = null
    private var scrollWheel: ScrollWheel? = null
    private var addItemButton: FloatingActionButton? = null
    private var scrollWheelToggleButton: FloatingActionButton? = null
    private var list: MutableList<Model>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        setViews()
    }

    private fun initViews() {
        recyclerView = findViewById<View>(R.id.recycler_view) as RecyclerView
        scrollWheel = findViewById<View>(R.id.scroll_wheel) as ScrollWheel
        addItemButton =
            findViewById<View>(R.id.add_item_button) as FloatingActionButton
        scrollWheelToggleButton =
            findViewById<View>(R.id.scroll_wheel_toggle_button) as FloatingActionButton
    }

    private fun setViews() {
        initializeList()
        recyclerView!!.adapter = RecyclerViewAdapter(applicationContext, list!!)
        recyclerView!!.addItemDecoration(RecyclerItemDecoration())
        recyclerView!!.layoutManager = CircularLayoutManager(applicationContext, 200, -100)
        recyclerView!!.addOnItemTouchListener(
            OnRecyclerItemClickListener(
                applicationContext,
                object : OnRecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(parent: RecyclerView?, childIndex: Int) {
                        Toast.makeText(
                            this@MainActivity,
                            (parent!!.getChildAt(childIndex)
                                .findViewById<View>(R.id.event) as TextView).text,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        )
        scrollWheel!!.recyclerView = recyclerView
        scrollWheel!!.isScrollWheelEnabled = false
        scrollWheel!!.isHighlightTouchAreaEnabled = false
        //scrollWheel.setConsumeTouchOutsideTouchAreaEnabled(false);
//        scrollWheel.setTouchAreaThickness(50);
        scrollWheel!!.setOnItemClickListener(object :
            OnItemClickListener {
            override fun onItemClick(
                scrollWheel: ScrollWheel?,
                childIndex: Int
            ) {
                Toast.makeText(
                    this@MainActivity,
                    "OC " + (scrollWheel!!.recyclerView!!
                        .getChildAt(childIndex)
                        .findViewById<View>(R.id.event) as TextView).text,
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onItemLongClick(
                scrollWheel: ScrollWheel?,
                childIndex: Int
            ) {
                Toast.makeText(
                    this@MainActivity,
                    "OLC " + (scrollWheel!!.recyclerView!!
                        .getChildAt(childIndex)
                        .findViewById<View>(R.id.event) as TextView).text,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
        addItemButton!!.setOnClickListener(this)
        scrollWheelToggleButton!!.setOnClickListener(this)
    }

    private fun initializeList() {
        list = ArrayList()
        (0..9).forEach {
            val model = Model("Event ${it + 1}", "12:00am - 12:00pm")
            list!!.add(model)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.add_item_button -> {
                val model = Model("Event ${1 + list!!.size}", "12:00am - 12:00pm")
                list!!.add(model)
                recyclerView!!.adapter!!.notifyItemChanged(list!!.size - 2)
                recyclerView!!.adapter!!.notifyItemInserted(list!!.size - 1)
            }
            R.id.scroll_wheel_toggle_button -> {
                scrollWheel!!.isScrollWheelEnabled = !scrollWheel!!.isScrollWheelEnabled
                scrollWheel!!.isHighlightTouchAreaEnabled =
                    !scrollWheel!!.isHighlightTouchAreaEnabled
            }
        }
    }
}