package kapil.circularlayoutmanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kapil.circularlayoutmanager.RecyclerViewAdapter.MyViewHolder
import kotlinx.android.synthetic.main.list_row.view.*


class RecyclerViewAdapter : ListAdapter<Model, MyViewHolder>(DIFF_CALLBACK) {

    var onItemClickListener: (Model.() -> Unit)? = null

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Model>() {
            override fun areItemsTheSame(oldItem: Model, newItem: Model) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Model, newItem: Model) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder =
        MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_row, parent, false))

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) =
        holder.bindTo(getItem(position)!!)

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val eventView: TextView = itemView.event
        private val timingsView: TextView = itemView.timings

        fun bindTo(model: Model) {
            eventView.text = model.event
            timingsView.text = model.timings
            itemView.setOnClickListener { onItemClickListener?.invoke(model) }
        }
    }
}