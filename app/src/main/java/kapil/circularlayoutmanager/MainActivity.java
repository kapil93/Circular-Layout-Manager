package kapil.circularlayoutmanager;

import android.os.Bundle;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kapil.circularlayoutmanager.CircularLayoutManager;
import com.kapil.circularlayoutmanager.OnItemClickListener;
import com.kapil.circularlayoutmanager.ScrollWheel;

import java.util.ArrayList;
import java.util.List;

/**
 * An example activity implementing a recycler view with a circular layout manager and scroll wheel.
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private RecyclerView recyclerView;
    private ScrollWheel scrollWheel;

    private FloatingActionButton addItemButton;
    private FloatingActionButton scrollWheelToggleButton;

    private List<Model> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setViews();
    }

    private void initViews() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        scrollWheel = (ScrollWheel) findViewById(R.id.scroll_wheel);

        addItemButton = (FloatingActionButton) findViewById(R.id.add_item_button);
        scrollWheelToggleButton = (FloatingActionButton) findViewById(R.id.scroll_wheel_toggle_button);
    }

    private void setViews() {
        initializeList();
        recyclerView.setAdapter(new RecyclerViewAdapter(getApplicationContext(), list));
        recyclerView.addItemDecoration(new RecyclerItemDecoration());
        recyclerView.setLayoutManager(new CircularLayoutManager(getApplicationContext(), 200, -100));
        recyclerView.addOnItemTouchListener(new OnRecyclerItemClickListener(getApplicationContext(),
                new OnRecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void OnItemClick(RecyclerView parent, int childIndex) {
                Toast.makeText(MainActivity.this, ((TextView) parent.getChildAt(childIndex)
                        .findViewById(R.id.event)).getText(), Toast.LENGTH_SHORT).show();
            }
        }));

        scrollWheel.setRecyclerView(recyclerView);
        scrollWheel.setScrollWheelEnabled(false);
        scrollWheel.setHighlightTouchAreaEnabled(false);
        //scrollWheel.setConsumeTouchOutsideTouchAreaEnabled(false);
        scrollWheel.setTouchAreaThickness(50);
        scrollWheel.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(ScrollWheel scrollWheel, int childIndex) {
                Toast.makeText(MainActivity.this, "OC " + ((TextView) scrollWheel.getRecyclerView()
                        .getChildAt(childIndex).findViewById(R.id.event)).getText(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemLongClick(ScrollWheel scrollWheel, int childIndex) {
                Toast.makeText(MainActivity.this, "OLC " + ((TextView) scrollWheel.getRecyclerView()
                        .getChildAt(childIndex).findViewById(R.id.event)).getText(), Toast.LENGTH_SHORT).show();
            }
        });

        addItemButton.setOnClickListener(this);
        scrollWheelToggleButton.setOnClickListener(this);
    }

    private void initializeList() {
        list = new ArrayList<>();
        String event = "Event ", timing = "12:00am - 12:00pm";

        for (int i = 0; i < 10; i++) {
            Model model = new Model();
            model.setEvent(event + (i + 1));
            model.setTimings(timing);

            list.add(model);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.add_item_button:

                Model model = new Model();
                model.setEvent("Event " + (1 + list.size()));
                model.setTimings("12:00am - 12:00pm");

                list.add(model);

                recyclerView.getAdapter().notifyItemChanged(list.size() - 2);
                recyclerView.getAdapter().notifyItemInserted(list.size() - 1);

                break;

            case R.id.scroll_wheel_toggle_button:

                scrollWheel.setScrollWheelEnabled(!scrollWheel.isScrollWheelEnabled());
                scrollWheel.setHighlightTouchAreaEnabled(!scrollWheel.isHighlightTouchAreaEnabled());

                break;
        }
    }
}
