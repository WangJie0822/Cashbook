@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import androidx.core.widget.NestedScrollView
import androidx.databinding.BindingAdapter

/**
 * 为 [NestedScrollView] 设置滚动监听 [onScroll]
 * > [onScroll]: (`scrollY`: [Int], `oldScrollY`: [Int]) -> [Unit]
 *
 * > `scrollY`: 当前垂直滚动原点 & `oldScrollY`: 先前垂直滚动原点
 */
@BindingAdapter("android:bind_nested_onScrollChange")
fun NestedScrollView.setOnScrollChangeListener(onScroll: ((Int, Int) -> Unit)?) {
    if (null == onScroll) {
        setOnScrollChangeListener(null as? NestedScrollView.OnScrollChangeListener?)
        return
    }
    setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
        onScroll.invoke(scrollY, oldScrollY)
    })
}

/** 为 [NestedScrollView] 设置滚动监听 [onScroll] */
@BindingAdapter("android:bind_nested_onScrollChange")
fun NestedScrollView.setOnScrollChangeListener(onScroll: NestedScrollListener?) {
    setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
        onScroll?.onScrollChange(scrollY, oldScrollY)
    })
}

/** 滚动监听接口，提供 [onScrollChange] 回调 */
interface NestedScrollListener {

    /** 滚动回调，传递当前垂直滚动原点 [scrollY] 以及先前垂直滚动原点 [oldScrollY] */
    fun onScrollChange(scrollY: Int, oldScrollY: Int)
}