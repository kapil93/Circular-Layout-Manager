package com.kapil.circularlayoutmanager

/**
 * This interface provides callback regarding recycler view item click and long click detected from
 * the scroll wheel when.
 */
interface OnItemClickListener {
    fun onItemClick(scrollWheel: ScrollWheel?, childIndex: Int)
    fun onItemLongClick(scrollWheel: ScrollWheel?, childIndex: Int)
}