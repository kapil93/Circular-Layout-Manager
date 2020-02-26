package kapil.circularlayoutmanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for recycler view.
 */

class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {

    private Context context;
    private List<Model> list;

    RecyclerViewAdapter(Context context, List<Model> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.list_row, parent, false));
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.event.setText(list.get(position).getEvent());
        holder.timings.setText(list.get(position).getTimings());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    /**
     * View Holder for recycler view.
     */

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView event, timings;

        MyViewHolder(View itemView) {
            super(itemView);
            event = (TextView) itemView.findViewById(R.id.event);
            timings = (TextView) itemView.findViewById(R.id.timings);
        }
    }
}
