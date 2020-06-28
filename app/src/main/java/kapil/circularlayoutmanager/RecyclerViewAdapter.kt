package kapil.circularlayoutmanager

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kapil.circularlayoutmanager.RecyclerViewAdapter.MyViewHolder

/**
 * Adapter for recycler view.
 */
internal class RecyclerViewAdapter(
    private val context: Context,
    private val list: List<Model>
) : RecyclerView.Adapter<MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.list_row, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.event.text = list[position].event
        holder.timings.text = list[position].timings
    }

    override fun getItemCount(): Int {
        return list.size
    }

    /**
     * View Holder for recycler view.
     */
    internal inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var event: TextView = itemView.findViewById<View>(R.id.event) as TextView
        var timings: TextView = itemView.findViewById<View>(R.id.timings) as TextView
    }

}