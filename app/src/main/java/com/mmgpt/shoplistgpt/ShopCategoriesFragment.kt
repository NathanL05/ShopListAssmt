package com.mmgpt.shoplistgpt

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.mmgpt.shoplistgpt.adapter.CategorySummaryAdapter
import com.mmgpt.shoplistgpt.databinding.FragmentCategoryBinding

class ShopCategoriesFragment : Fragment() {

    private val viewModel: ShoppingListViewModel by activityViewModels()
    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = CategorySummaryAdapter()
        binding.recyclerView.adapter = adapter

        val gestureDetector = GestureDetectorCompat(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private var firstTapPosition = RecyclerView.NO_POSITION

            // The touch listener is on the RecyclerView, so e.x/e.y are already
            // in its coordinate system.
            private fun positionAt(e: MotionEvent): Int {
                val child = binding.recyclerView.findChildViewUnder(e.x, e.y)
                return child?.let { binding.recyclerView.getChildAdapterPosition(it) } ?: RecyclerView.NO_POSITION
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
                if (position != RecyclerView.NO_POSITION && position == firstTapPosition) {
                    // Row N is category N, which lives on pager page N.
                    val viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
                    viewPager.currentItem = position
                    return true
                }
                return false
            }
        })

        binding.recyclerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        viewModel.categories.observe(viewLifecycleOwner) { list ->
            binding.tvHeading.text = getString(R.string.summary_heading)
            binding.tvCount.text = ""
            adapter.submitList(list)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
