package kapil.circularlayoutmanager;

import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RelativeLayout relativeLayout;
    RecyclerView recyclerView;
    FloatingActionButton fab, clickWheelFab;
    List<Model> list;
    int screenWidth, screenHeight;
    boolean clickWheelSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        measureScreen();

        scaleContainingLayout();

        initializeList();

        recyclerView = (RecyclerView) findViewById(R.id.list_container);
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(getApplicationContext(), list);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new CircularLayoutManager(MainActivity.this, recyclerView,
                (int) (screenWidth * (-720.0 / 720.0)), (int) (screenWidth * (185.0 / 720.0)), (int) (screenWidth * (1000.0 / 720.0)), (int) (screenWidth * (350.0 / 720.0))));

        scaleRecyclerView();

        initializeFabs(adapter);

        initializeClickListener();
    }

    private void measureScreen() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
    }

    private void scaleContainingLayout() {
        relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout);
        relativeLayout.post(new Runnable() {
            @Override
            public void run() {
                CoordinatorLayout.LayoutParams mParams;
                mParams = (CoordinatorLayout.LayoutParams) relativeLayout.getLayoutParams();
                mParams.height = relativeLayout.getWidth();
                relativeLayout.setLayoutParams(mParams);
                relativeLayout.postInvalidate();
            }
        });
    }

    private void initializeList() {
        list = new ArrayList<>();
        String event = "Event ", timing = "12:00am - 12:00pm";

        for (int i = 0; i < 3; i++) {
            Model model = new Model();
            model.setEvent(event + (i + 1));
            model.setTimings(timing);
            list.add(model);
        }
    }

    private void scaleRecyclerView() {
        RelativeLayout.LayoutParams rParams;
        rParams = (RelativeLayout.LayoutParams) recyclerView.getLayoutParams();
        rParams.height = (int) (screenWidth * (370.0 / 720.0));
        rParams.width = (int) (screenWidth * (500.0 / 720.0));
        recyclerView.setLayoutParams(rParams);
    }

    private void initializeFabs(final RecyclerViewAdapter adapter) {
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Model model = new Model();
                model.setEvent("Event " + (1 + list.size()));
                model.setTimings("12:00am - 12:00pm");
                list.add(model);
                adapter.notifyItemInserted(list.size() - 1);
            }
        });

        final ImageView clickWheel = (ImageView) findViewById(R.id.click_wheel);
        clickWheel.setAlpha(0.5f);
        clickWheel.setVisibility(View.INVISIBLE);

        clickWheelSelected = false;

        clickWheelFab = (FloatingActionButton) findViewById(R.id.click_wheel_fab);
        clickWheelFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!clickWheelSelected) {
                    clickWheel.setVisibility(View.VISIBLE);
                    implementClickWheel(clickWheel, screenWidth / 2, screenWidth / 2, (screenWidth - (int) (screenWidth * (100 / 720.0))) / 2, screenWidth / 2);
                    clickWheelSelected = true;
                } else {
                    clickWheel.setVisibility(View.INVISIBLE);
                    clickWheelSelected = false;
                }
            }
        });
    }

    private void initializeClickListener() {
        recyclerView.addOnItemTouchListener(new OnRecyclerItemClickListener(getApplicationContext(), new OnRecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void OnItemClick(View view, int position) {
                if (view != null) {
                    Toast.makeText(MainActivity.this, ((TextView) view.findViewById(R.id.event)).getText(), Toast.LENGTH_SHORT).show();
                }
            }
        }));
    }

    private void implementClickWheel(final ImageView clickWheel, final float xCenter, final float yCenter, final float innerRadius, final float outerRadius) {

        final VelocityTracker[] velocityTracker = {null};

        clickWheel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(!clickWheelSelected){
                    return false;
                }
                if (motionEvent != null) {
                    int index = motionEvent.getActionIndex();
                    float x = MotionEventCompat.getX(motionEvent, index);
                    float y = MotionEventCompat.getY(motionEvent, index);
                    int pointerId = motionEvent.getPointerId(index);

                    if (((((x - xCenter) * (x - xCenter)) + ((y - yCenter) * (y - yCenter))) > (innerRadius * innerRadius))
                            && ((((x - xCenter) * (x - xCenter)) + ((y - yCenter) * (y - yCenter))) < (outerRadius * outerRadius))) {

                        switch (motionEvent.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:

                                if (velocityTracker[0] == null) {
                                    velocityTracker[0] = VelocityTracker.obtain();
                                } else {
                                    velocityTracker[0].clear();
                                }

                                velocityTracker[0].addMovement(motionEvent);

                                break;
                            case MotionEvent.ACTION_MOVE:

                                velocityTracker[0].addMovement(motionEvent);
                                velocityTracker[0].computeCurrentVelocity(1000);
                                float xVel = VelocityTrackerCompat.getXVelocity(velocityTracker[0], pointerId);
                                float yVel = VelocityTrackerCompat.getYVelocity(velocityTracker[0], pointerId);

                                int delta = 0;

                                if ((x <= xCenter) && (y < yCenter)) {
                                    delta = (int) (xVel - yVel);
                                } else if ((x > xCenter) && (y <= yCenter)) {
                                    delta = (int) (xVel + yVel);
                                } else if ((x >= xCenter) && (y > yCenter)) {
                                    delta = (int) (yVel - xVel);
                                } else if ((x < xCenter) && (y >= yCenter)) {
                                    delta = (int) (-xVel - yVel);
                                }

                                recyclerView.smoothScrollBy(0, delta);

                                break;
                        }

                    } else {
                        return false;
                    }
                }
                return true;
            }
        });
    }

}
