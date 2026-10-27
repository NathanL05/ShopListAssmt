package com.mmgpt.shoplistgpt

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

import com.mmgpt.shoplistgpt.PlanCategoryFragment
import com.mmgpt.shoplistgpt.PlanCategoriesFragment

class CategoryPagerAdapter(
    activity: FragmentActivity,
    private val viewModel: ShoppingListViewModel,
    private val planMode: Boolean = false
) : FragmentStateAdapter(activity) {

    private val categoryCount: Int
        get() = viewModel.categories.value?.size ?: 0

    override fun getItemCount(): Int = categoryCount + 1

    override fun createFragment(position: Int): Fragment =
        if (planMode) {
            if (position == 0) {
                PlanCategoriesFragment()
            } else {
                PlanCategoryFragment.newInstance(position - 1)
            }
        } else {
            val size = categoryCount
            if (position == size) {
                ShopCategoriesFragment()
            } else {
                CategoryFragment.newInstance(position)
            }
        }

    // FragmentStateAdapter reuses an existing fragment whenever a page's item ID
    // is unchanged. The default ID is the position, but the fragment type at a
    // position depends on the mode (and, in Shop mode, on the category count,
    // because the summary page is last). Encode the mode, page type and category
    // index in the ID so a stale fragment is never shown at the wrong position.
    override fun getItemId(position: Int): Long =
        if (planMode) {
            if (position == 0) PLAN_SUMMARY_ID else PLAN_PAGE_BASE + (position - 1)
        } else {
            if (position == categoryCount) SHOP_SUMMARY_ID else SHOP_PAGE_BASE + position
        }

    override fun containsItem(itemId: Long): Boolean {
        val size = categoryCount
        return if (planMode) {
            itemId == PLAN_SUMMARY_ID || itemId - PLAN_PAGE_BASE in 0 until size
        } else {
            itemId == SHOP_SUMMARY_ID || itemId - SHOP_PAGE_BASE in 0 until size
        }
    }

    companion object {
        private const val SHOP_SUMMARY_ID = -1L
        private const val PLAN_SUMMARY_ID = -2L
        private const val SHOP_PAGE_BASE = 0L
        private const val PLAN_PAGE_BASE = 1_000_000_000L
    }
}
