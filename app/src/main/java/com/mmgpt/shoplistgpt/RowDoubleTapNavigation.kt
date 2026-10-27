package com.mmgpt.shoplistgpt

import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/**
 * Double-tapping a row of [list] switches the main pager to the page at the
 * same position as the row. [canOpen] decides which rows can be opened.
 */
fun Fragment.openPageOnRowDoubleTap(
    list: RecyclerView,
    canOpen: (position: Int) -> Boolean = { true },
) {
    val gestureDetector = GestureDetectorCompat(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
        private var firstTapPosition = RecyclerView.NO_POSITION

        // The touch listener is on the RecyclerView, so e.x/e.y are already
        // in its coordinate system.
        private fun positionAt(e: MotionEvent): Int {
            val child = list.findChildViewUnder(e.x, e.y)
            return child?.let { list.getChildAdapterPosition(it) } ?: RecyclerView.NO_POSITION
        }

        // onDoubleTap receives the FIRST tap's down event, and two taps up to
        // ~100dp apart (i.e. on adjacent rows) still count as a double-tap.
        // Remember the first tap's row here and only navigate in
        // onDoubleTapEvent if the second tap lands on the same row.
        override fun onDoubleTap(e: MotionEvent): Boolean {
            firstTapPosition = positionAt(e)
            return true
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            if (e.actionMasked != MotionEvent.ACTION_DOWN) return false
            val position = positionAt(e)
            if (position != RecyclerView.NO_POSITION && position == firstTapPosition && canOpen(position)) {
                val viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
                viewPager.currentItem = position
                return true
            }
            return false
        }
    })

    list.setOnTouchListener { _, event ->
        gestureDetector.onTouchEvent(event)
        false
    }
}
