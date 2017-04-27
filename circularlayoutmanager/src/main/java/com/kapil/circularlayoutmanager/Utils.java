package com.kapil.circularlayoutmanager;

import android.content.Context;
import android.util.TypedValue;

/**
 * This class contains utility helper functions.
 */

class Utils {

    /**
     * Function to convert a value given in dp to pixels (px).
     *
     * @param context Current context, used to access resources.
     * @param dp      The value (in dp) to be converted.
     * @return        The value in pixels.
     */

    static float dpToPx(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
