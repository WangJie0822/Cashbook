@file:Suppress("unused")
@file:JvmName("TabLayoutExt")

package cn.wj.android.cashbook.base.ext

import androidx.core.view.doOnDetach
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
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

var TabLayout.pager2Adapter: RecyclerView.Adapter<*>?
    get() = getTag(R.id.tag_tablayout_pager2adapter) as? RecyclerView.Adapter<*>
    set(value) {
        setTag(R.id.tag_tablayout_pager2adapter, value)
    }

var TabLayout.pager2ChangeCallback: TabLayoutOnPageChangeCallback?
    get() = getTag(R.id.tag_tablayout_pageChangeCallback) as? TabLayoutOnPageChangeCallback
    set(value) {
        setTag(R.id.tag_tablayout_pageChangeCallback, value)
    }

var TabLayout.currentVp2SelectedListener: ViewPagerOnTabSelectedListener?
    get() = getTag(R.id.tag_tablayout_currentVp2SelectedListener) as? ViewPagerOnTabSelectedListener
    set(value) {
        setTag(R.id.tag_tablayout_currentVp2SelectedListener, value)
    }

var TabLayout.pager2AdapterObserver: Pager2AdapterObserver?
    get() = getTag(R.id.tag_tablayout_pagerAdapterObserver) as? Pager2AdapterObserver
    set(value) {
        setTag(R.id.tag_tablayout_pagerAdapterObserver, value)
    }

fun TabLayout.setupWithViewPager2(vp: ViewPager2?, autoRefresh: Boolean = true, implicitSetup: Boolean = false) {
    pager2ChangeCallback?.let { callback ->
        viewPager2?.unregisterOnPageChangeCallback(callback)
    }

    if (null != currentVp2SelectedListener) {
        removeOnTabSelectedListener(currentVp2SelectedListener as OnTabSelectedListener)
        currentVp2SelectedListener = null
    }

    viewPager2 = vp
    viewPager2?.let { vp2 ->
        if (null == pager2ChangeCallback) {
            pager2ChangeCallback = TabLayoutOnPageChangeCallback(this)
        }
        pager2ChangeCallback?.let { callback ->
            callback.reset()
            vp2.registerOnPageChangeCallback(callback)
        }
        if (null == currentVp2SelectedListener) {
            currentVp2SelectedListener = ViewPagerOnTabSelectedListener(vp2)
            addOnTabSelectedListener(currentVp2SelectedListener as OnTabSelectedListener)
        }
        val pagerAdapter = vp2.adapter
        if (null != pagerAdapter) {
            setPager2Adapter(pagerAdapter, autoRefresh)
        }

        // Now update the scroll position to match the ViewPager's current item
        setScrollPosition(vp2.currentItem, 0f, true)
    }
    if (null == vp) {
        viewPager2 = null
        setPager2Adapter(null, false)
    }
    doOnDetach {
        if (implicitSetup) {
            // If we've been setup with a ViewPager implicitly, let's clear out any listeners, etc
            setupWithViewPager(null)
        }
    }
}

fun TabLayout.setPager2Adapter(adapter: RecyclerView.Adapter<*>?, addObserver: Boolean) {
    pager2AdapterObserver?.let { observer ->
        adapter?.unregisterAdapterDataObserver(observer)
    }
    pager2Adapter = adapter
    if (addObserver) {
        if (null == pager2AdapterObserver) {
            pager2AdapterObserver = Pager2AdapterObserver(this)
        }
        pager2AdapterObserver?.let { observer ->
            pager2Adapter?.registerAdapterDataObserver(observer)
        }
    }
    populateFromPager2Adapter()
}

fun TabLayout.populateFromPager2Adapter() {
    removeAllTabs()
    pager2Adapter?.let { adapter ->
        if (adapter !is TabsAdapter) {
            // 适配器必须实现 TabsAdapter 接口
            logger().e(IllegalArgumentException("ViewPager2's adapter must implementation TabsAdapter"), "populateFromPager2Adapter")
            return
        }
        for (i in 0 until adapter.itemCount) {
            addTab(newTab().setText(adapter.getPageTitle(i)), false)
        }

        viewPager2?.let { vp2 ->
            if (adapter.itemCount > 0) {
                val curItem = vp2.currentItem
                if (curItem != selectedTabPosition && curItem < tabCount) {
                    selectTab(getTabAt(curItem))
                }
            }
        }
    }
}

class Pager2AdapterObserver(private val tabLayout: TabLayout) : RecyclerView.AdapterDataObserver() {
    override fun onChanged() {
        tabLayout.populateFromPager2Adapter()
    }

    override fun onStateRestorationPolicyChanged() {
        tabLayout.populateFromPager2Adapter()
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

/** 和 [TabLayout] 绑定使用的 [ViewPager2] 适配器接口 */
interface TabsAdapter {
    /** 根据下标 [position] 获取并返回当前 Tab 标签 */
    fun getPageTitle(position: Int): String?
}