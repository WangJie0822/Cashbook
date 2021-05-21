@file:Suppress("unused")

package cn.wj.android.databinding.adapter

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.viewpager.widget.ViewPager
import cn.wj.android.cashbook.base.ext.base.condition

/*
 * ViewPager DataBinding 适配器
 */

/**
 * 给 [vp] 添加页码切换监听 [scrolled] & [selected] & [changed]
 * > [listener] 为属性变化监听，`DataBinding` 自动实现
 */
@BindingAdapter(
    "android:bind_vp_change_scrolled",
    "android:bind_vp_change_selected",
    "android:bind_vp_change_changed",
    "android:bind_vp_currentItemAttrChanged",
    requireAll = false
)
fun setViewPagerPageChangeListener(
    vp: ViewPager,
    scrolled: ((Int, Float, Int) -> Unit)?,
    selected: ((Int) -> Unit)?,
    changed: ((Int) -> Unit)?,
    listener: InverseBindingListener?
) {
    vp.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
        override fun onPageScrollStateChanged(state: Int) {
            changed?.invoke(state)
        }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            scrolled?.invoke(position, positionOffset, positionOffsetPixels)
        }

        override fun onPageSelected(position: Int) {
            selected?.invoke(position)
            listener?.onChange()
        }
    })
}

/** 将 [vp] 预加载页数设置为 [offscreenPageLimit] */
@BindingAdapter("android:bind_vp_offscreenPageLimit")
fun setViewPagerOffscreenPageLimit(vp: ViewPager, offscreenPageLimit: Int?) {
    if (null == offscreenPageLimit) {
        return
    }
    vp.offscreenPageLimit = offscreenPageLimit
}

/** 根据是否平滑滚动 [smoothScroll] 将 [vp] 切换到指定页 [currentItem] */
@BindingAdapter("android:bind_vp_currentItem", "android:bind_vp_smoothScroll", requireAll = false)
fun setViewPagerCurrentItem(vp: ViewPager, currentItem: Int?, smoothScroll: Boolean?) {
    if (null == currentItem) {
        return
    }
    vp.setCurrentItem(currentItem, smoothScroll.condition)
}

/** 获取 [vp] 当前位置 */
@InverseBindingAdapter(attribute = "android:bind_vp_currentItem")
fun getViewPagerCurrentItem(vp: ViewPager): Int {
    return vp.currentItem
}