package com.kapil.circularlayoutmanager;

/**
 * This interface provides callback regarding recycler view item click and long click detected from
 * the scroll wheel when.
 */

public interface OnItemClickListener {

    void onItemClick(ScrollWheel scrollWheel, int childIndex);

    void onItemLongClick(ScrollWheel scrollWheel, int childIndex);
}
