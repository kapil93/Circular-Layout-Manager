package com.kapil.circularlayoutmanager;

import android.content.Context;
import android.util.TypedValue;

/**
 * This class contains utility helper functions.
 */

class Utils {

    static float dpToPx(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
