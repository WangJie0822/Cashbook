@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.viewpager.widget.ViewPager
import cn.wj.android.cashbook.base.ext.base.condition

/*
 * ViewPager DataBinding 适配器
 */

/**
 * 给 [ViewPager] 添加页码切换监听 [scrolled] & [selected] & [changed]
 * > [listener] 为属性变化监听，`DataBinding` 自动实现
 */
@BindingAdapter(
    "android:bind_vp_change_scrolled",
    "android:bind_vp_change_selected",
    "android:bind_vp_change_changed",
    "android:bind_vp_currentItemAttrChanged",
    requireAll = false
)
fun ViewPager.setViewPagerPageChangeListener(
    scrolled: ((Int, Float, Int) -> Unit)?,
    selected: ((Int) -> Unit)?,
    changed: ((Int) -> Unit)?,
    listener: InverseBindingListener?
) {
    this.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
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

/** 将 [ViewPager] 预加载页数设置为 [offscreenPageLimit] */
@BindingAdapter("android:bind_vp_offscreenPageLimit")
fun ViewPager.setViewPagerOffscreenPageLimit(offscreenPageLimit: Int?) {
    if (null == offscreenPageLimit) {
        return
    }
    this.offscreenPageLimit = offscreenPageLimit
}

/** 根据是否平滑滚动 [smoothScroll] 将 [ViewPager] 切换到指定页 [currentItem] */
@BindingAdapter("android:bind_vp_currentItem", "android:bind_vp_smoothScroll", requireAll = false)
fun ViewPager.setViewPagerCurrentItem(currentItem: Int?, smoothScroll: Boolean?) {
    if (null == currentItem) {
        return
    }
    this.setCurrentItem(currentItem, smoothScroll.condition)
}

/** 获取 [ViewPager] 当前位置 */
@InverseBindingAdapter(attribute = "android:bind_vp_currentItem")
fun ViewPager.getViewPagerCurrentItem(): Int {
    return this.currentItem
}