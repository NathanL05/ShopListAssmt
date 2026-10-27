package com.mmgpt.shoplistgpt

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.MotionEvents
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Double-tapping a category name on the Plan categories screen (pager page 0)
 * or the Shop summary screen (last pager page) should open that category's
 * page. These tests cover the two ways it went to the wrong page:
 *
 *  1. CategoryPagerAdapter used positions as item IDs, so ViewPager2 kept
 *     showing fragments built for a different mode or category count.
 *  2. GestureDetector reports the FIRST tap of a double-tap, and accepts two
 *     taps on adjacent rows, so tapping row A then row B navigated to A.
 */
@RunWith(AndroidJUnit4::class)
class CategoryNavigationTest {

    private val names = listOf("Fruit", "Dairy", "Bakery", "Meat")
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        seedCategories(names)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    // --- Root cause 1: stale pager pages -----------------------------------

    @Test
    fun shopPages_afterRecreationInPlanMode_areShopPages() {
        onView(withId(R.id.btnPlan)).perform(click())
        scenario.recreate()
        onView(withId(R.id.btnShop)).perform(click())

        names.forEachIndexed { index, name ->
            showPage(index)
            assertVisibleHeading(name)
            // The Plan categories editor (with Clear All) must not leak into Shop.
            onView(allOf(withId(R.id.btnClearAll), isDisplayed())).check(doesNotExist())
        }
        showPage(names.size)
        assertVisibleHeading("Summary")
    }

    @Test
    fun summaryDoubleTap_afterCategoryCountChanges_opensTappedCategory() {
        onView(withId(R.id.btnShop)).perform(click())
        showPage(names.size) // build the summary fragment at page 4
        pressBack() // back to the start page; MainActivity overrides onBackPressed

        // Same as importing a file with more categories: the Shop pager is reused.
        val more = names + listOf("Frozen", "Drinks")
        scenario.onActivity { activity ->
            ViewModelProvider(activity)[ShoppingListViewModel::class.java]
                .setCategories(buildCategories(more))
        }
        onView(withId(R.id.btnShop)).perform(click())

        showPage(more.size)
        assertVisibleHeading("Summary")
        onVisibleList().perform(tapRows("Frozen", "Frozen"))

        assertEquals(4, currentPage())
        waitForPagerIdle()
        assertVisibleHeading("Frozen")
    }

    // --- Root cause 2: double-tap across two different rows ---------------

    @Test
    fun summary_tapsOnTwoDifferentRows_doNotNavigate() {
        onView(withId(R.id.btnShop)).perform(click())
        val summaryPage = names.size
        showPage(summaryPage)

        onVisibleList().perform(tapRows("Dairy", "Bakery"))

        assertEquals(summaryPage, currentPage())
    }

    @Test
    fun planCategories_tapsOnTwoDifferentRows_doNotNavigate() {
        onView(withId(R.id.btnPlan)).perform(click())
        showPage(0)

        onVisibleList().perform(tapRows("Dairy", "Bakery"))

        assertEquals(0, currentPage())
    }

    // --- Controls: a genuine double-tap on one row still navigates --------

    @Test
    fun summary_doubleTapOnOneRow_opensThatCategory() {
        onView(withId(R.id.btnShop)).perform(click())
        showPage(names.size)

        onVisibleList().perform(tapRows("Bakery", "Bakery"))

        assertEquals(2, currentPage())
        waitForPagerIdle()
        assertVisibleHeading("Bakery")
    }

    @Test
    fun planCategories_doubleTapOnOneRow_opensThatCategory() {
        onView(withId(R.id.btnPlan)).perform(click())
        showPage(0)

        onVisibleList().perform(tapRows("Dairy", "Dairy"))

        // Plan page 0 is the categories list, so category 1 is page 2.
        assertEquals(2, currentPage())
        waitForPagerIdle()
        assertVisibleHeading("Dairy")
    }

    // --- Helpers ------------------------------------------------------------

    private fun seedCategories(headings: List<String>) {
        val state = JSONObject().put("categories", JSONArray().apply {
            buildCategories(headings).forEach { cat ->
                put(JSONObject().apply {
                    put("id", cat.id)
                    put("heading", cat.heading)
                    put("items", JSONArray().apply {
                        cat.items.forEach { item ->
                            put(JSONObject().put("id", item.id).put("name", item.name))
                        }
                    })
                })
            }
        })
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("state", Context.MODE_PRIVATE)
            .edit().clear().putString("state", state.toString()).commit()
    }

    private fun buildCategories(headings: List<String>): List<ShoppingCategory> =
        headings.mapIndexed { index, heading ->
            ShoppingCategory(
                id = index.toLong(),
                heading = heading,
                items = listOf(
                    ShoppingItem(id = index * 10L, name = "$heading item 1"),
                    ShoppingItem(id = index * 10L + 1, name = "$heading item 2")
                )
            )
        }

    private fun showPage(page: Int) {
        scenario.onActivity {
            it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(page, false)
        }
        onIdle()
    }

    private fun currentPage(): Int {
        var page = -1
        scenario.onActivity { page = it.findViewById<ViewPager2>(R.id.viewPager).currentItem }
        return page
    }

    /** Smooth scrolls run on frame callbacks, which Espresso doesn't wait for. */
    private fun waitForPagerIdle() {
        val deadline = SystemClock.uptimeMillis() + 3000
        while (SystemClock.uptimeMillis() < deadline) {
            var state = ViewPager2.SCROLL_STATE_IDLE
            scenario.onActivity { state = it.findViewById<ViewPager2>(R.id.viewPager).scrollState }
            if (state == ViewPager2.SCROLL_STATE_IDLE) break
            SystemClock.sleep(50)
        }
        onIdle()
    }

    private fun assertVisibleHeading(text: String) {
        onView(allOf(withId(R.id.tvHeading), isCompletelyDisplayed()))
            .check(matches(withText(text)))
    }

    private fun onVisibleList() =
        onView(allOf(withId(R.id.recyclerView), isDisplayed()))

    /**
     * Taps the rows labelled [first] then [second] in quick succession (well
     * inside the double-tap timeout), like a user's two fingertip taps.
     */
    private fun tapRows(first: String, second: String) = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(RecyclerView::class.java)

        override fun getDescription() = "tap row \"$first\" then row \"$second\""

        override fun perform(uiController: UiController, view: View) {
            tap(uiController, centerOfLabel(view as RecyclerView, first))
            uiController.loopMainThreadForAtLeast(80)
            tap(uiController, centerOfLabel(view, second))
            // Let the double-tap timeout expire so no gesture is left pending.
            uiController.loopMainThreadForAtLeast(400)
        }

        private fun tap(uiController: UiController, point: FloatArray) {
            val down = MotionEvents.sendDown(uiController, point, floatArrayOf(1f, 1f)).down
            try {
                MotionEvents.sendUp(uiController, down, point)
            } finally {
                down.recycle()
            }
        }

        private fun centerOfLabel(list: RecyclerView, label: String): FloatArray {
            val matches = ArrayList<View>()
            list.findViewsWithText(matches, label, View.FIND_VIEWS_WITH_TEXT)
            val textView = matches.filterIsInstance<TextView>().first { it.text.toString() == label }
            val location = IntArray(2)
            textView.getLocationOnScreen(location)
            return floatArrayOf(
                location[0] + textView.width / 2f,
                location[1] + textView.height / 2f
            )
        }
    }
}
