@file:Suppress("unused")
@file:JvmName("TabLayoutExt")

package cn.wj.android.cashbook.base.ext

import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import cn.wj.android.cashbook.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import java.lang.ref.WeakReference

/**
 * [TabLayout]
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/28
 */

var TabLayout.viewPager2: ViewPager2?
    get() = getTag(R.id.tag_tablayout_viewpager2) as? ViewPager2
    set(value) {
        setTag(R.id.tag_tablayout_viewpager2, value)
    }

var TabLayout.pageChangeCallback: TabLayoutOnPageChangeCallback?
    get() = getTag(R.id.tag_tablayout_pageChangeCallback) as? TabLayoutOnPageChangeCallback
    set(value) {
        setTag(R.id.tag_tablayout_pageChangeCallback, value)
    }

var TabLayout.currentVp2SelectedListener: ViewPagerOnTabSelectedListener?
    get() = getTag(R.id.tag_tablayout_currentVp2SelectedListener) as? ViewPagerOnTabSelectedListener
    set(value) {
        setTag(R.id.tag_tablayout_currentVp2SelectedListener, value)
    }

fun TabLayout.setupWithViewPager2(vp: ViewPager2) {
    // 绑定前必须设置适配器
    val adapter = vp.adapter ?: throw IllegalStateException("Please setup adapter first!")
    if (adapter !is TabsAdapter) {
        // 适配器必须实现 TabsAdapter 接口
        throw IllegalArgumentException("ViewPager2's adapter must implementation TabsAdapter")
    }
    // 添加 Tabs
    adapter.getTabs().forEach { tabText ->
        addTab(newTab().setText(tabText))
    }
    pageChangeCallback?.let { callback ->
        viewPager2?.unregisterOnPageChangeCallback(callback)
    }

    if (null != currentVp2SelectedListener) {
        removeOnTabSelectedListener(currentVp2SelectedListener as OnTabSelectedListener)
        currentVp2SelectedListener = null
    }

    viewPager2 = vp
    viewPager2?.let { vp2 ->
        if (null == pageChangeCallback) {
            pageChangeCallback = TabLayoutOnPageChangeCallback(this)
        }
        pageChangeCallback?.let { callback ->
            callback.reset()
            vp2.registerOnPageChangeCallback(callback)
        }
        if (null == currentVp2SelectedListener) {
            currentVp2SelectedListener = ViewPagerOnTabSelectedListener(vp2)
            addOnTabSelectedListener(currentVp2SelectedListener as OnTabSelectedListener)
        }
        // TODO
    }

}

class ViewPagerOnTabSelectedListener(private val viewPager: ViewPager2) : OnTabSelectedListener {
    override fun onTabSelected(tab: TabLayout.Tab) {
        viewPager.currentItem = tab.position
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
    }
}

class TabLayoutOnPageChangeCallback(tabLayout: TabLayout) : ViewPager2.OnPageChangeCallback() {

    private val tabLayoutRef: WeakReference<TabLayout> = WeakReference(tabLayout)
    private var previousScrollState = 0
    private var scrollState = 0

    override fun onPageScrollStateChanged(state: Int) {
        previousScrollState = scrollState
        scrollState = state
    }

    override fun onPageScrolled(
        position: Int, positionOffset: Float, positionOffsetPixels: Int
    ) {
        val tabLayout = tabLayoutRef.get()
        if (tabLayout != null) {
            // Only update the text selection if we're not settling, or we are settling after
            // being dragged
            val updateText = scrollState != ViewPager.SCROLL_STATE_SETTLING || previousScrollState == ViewPager.SCROLL_STATE_DRAGGING
            // Update the indicator if we're not settling after being idle. This is caused
            // from a setCurrentItem() call and will be handled by an animation from
            // onPageSelected() instead.
            val updateIndicator = !(scrollState == ViewPager.SCROLL_STATE_SETTLING && previousScrollState == ViewPager.SCROLL_STATE_IDLE)
            tabLayout.setScrollPosition(position, positionOffset, updateText, updateIndicator)
        }
    }

    override fun onPageSelected(position: Int) {
        val tabLayout = tabLayoutRef.get()
        if (tabLayout != null && tabLayout.selectedTabPosition != position && position < tabLayout.tabCount) {
            // Select the tab, only updating the indicator if we're not being dragged/settled
            // (since onPageScrolled will handle that).
            val updateIndicator = (scrollState == ViewPager.SCROLL_STATE_IDLE
                    || (scrollState == ViewPager.SCROLL_STATE_SETTLING
                    && previousScrollState == ViewPager.SCROLL_STATE_IDLE))
            tabLayout.selectTab(tabLayout.getTabAt(position), updateIndicator)
        }
    }

    fun reset() {
        scrollState = ViewPager2.SCROLL_STATE_IDLE
        previousScrollState = scrollState
    }

}

interface TabsAdapter {
    fun getTabs(): Array<String>
}