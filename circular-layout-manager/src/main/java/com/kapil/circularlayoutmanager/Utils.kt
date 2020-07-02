package com.kapil.circularlayoutmanager

import androidx.recyclerview.widget.RecyclerView


const val INVALID_INDEX = -1

/**
 * Detects the adapter list item under a particular point.
 *
 * @param x X-coordinate of point.
 * @param y Y-coordinate of point.
 * @return child adapter position if it is found under the point or [INVALID_INDEX] if there is
 * no child view found under the point.
 */
fun RecyclerView.getChildAdapterPosition(x: Float, y: Float) =
    findChildViewUnder(x, y)?.let { getChildAdapterPosition(it) } ?: INVALID_INDEX

/**
 * Extension property to get and set the [CircularLayoutManager] to and from the [RecyclerView]
 * respectively.
 *
 * The getter would return null if the [RecyclerView.LayoutManager] attached to the RecyclerView
 * is not a CircularLayoutManager.
 */
var RecyclerView.circularLayoutManager: CircularLayoutManager?
    get() = layoutManager as? CircularLayoutManager
    set(value) {
        layoutManager = value
    }