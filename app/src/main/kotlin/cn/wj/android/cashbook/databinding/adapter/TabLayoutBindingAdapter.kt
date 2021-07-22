@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.viewpager2.widget.ViewPager2
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.setupWithViewPager2
import com.google.android.material.tabs.TabLayout

/**
 * TabLayout 相关适配器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/21
 */

/** 将 [TabLayout] 与 id 为 [viewPager2Id] 的 [ViewPager2] 相关联 */
@BindingAdapter("android:bind_tl_viewpager2")
fun TabLayout.bindViewPager2(viewPager2Id: Int?) {
    if (null == viewPager2Id) {
        return
    }

    val window = (context as? Activity)?.window
    val v = if (null != window) {
        window.decorView.findViewById(viewPager2Id)
    } else {
        var parentView = parent as? ViewGroup
        var vp2View = parentView?.findViewById<View>(viewPager2Id)
        while (null != parentView && null == vp2View) {
            parentView = parentView.parent as? ViewGroup
            vp2View = parentView?.findViewById(viewPager2Id)
        }
        vp2View
    }
    if (null == v) {
        logger().w("View with R.id.$viewPager2Id not found!")
        return
    }
    if (v !is ViewPager2) {
        logger().w("View with R.id.$viewPager2Id is not the ViewPager2!")
        return
    }
    setupWithViewPager2(v)
}

@set:BindingAdapter("android:bind_tl_currentItem")
@get:InverseBindingAdapter(
    attribute = "android:bind_tl_currentItem",
    event = "android:bind_srl_currentItemAttrChanged"
)
var TabLayout.currentItem: Int
    get() = selectedTabPosition
    set(value) {
        selectTab(getTabAt(value))
    }

/** 设置选中回调 */
@BindingAdapter("android:bind_tl_onSelected", "android:bind_srl_currentItemAttrChanged", requireAll = false)
fun TabLayout.addOnSelectedListener(
    onSelected: ((Int) -> Unit)?,
    listener: InverseBindingListener?
) {
    addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            onSelected?.invoke(tab.position)
            listener?.onChange()
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {

        }

        override fun onTabReselected(tab: TabLayout.Tab?) {

        }
    })
}

/** 设置标签列表 */
@BindingAdapter("android:bind_tl_tabs")
fun TabLayout.setTabList(list: List<Any>) {
    removeAllTabs()
    list.forEach {
        addTab(newTab().setText(it.toString()))
    }
}