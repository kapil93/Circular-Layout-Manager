package com.kapil.circularlayoutmanager;

/**
 * Created by witworks on 28/04/17.
 */

public interface OnItemClickListener {

    void onItemClick(ScrollWheel scrollWheel, int childIndex);

    void onItemLongClick(ScrollWheel scrollWheel, int childIndex);
}
