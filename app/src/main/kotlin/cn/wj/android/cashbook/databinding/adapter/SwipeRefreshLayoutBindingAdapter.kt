@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cn.wj.android.databinding.adapter.getIdentifier

/*
 * SwipeRefreshLayout DataBinding 适配器
 */

/** 为 [srl] 设置进度条颜色 [color] */
@BindingAdapter("android:bind_srl_schemeColors")
fun setSwipeRefreshLayoutSchemeColors(srl: SwipeRefreshLayout, @ColorInt color: Int?) {
    if (null == color) {
        return
    }
    srl.setColorSchemeColors(color)
}

/**
 * 为 [srl] 设置进度条颜色 [colorStr]
 * > [colorStr] 多个使用 `,` 分隔，可使用颜色id `"app_color_white"` 或色值 `"#FFFFFF"`
 */
@BindingAdapter("android:bind_srl_schemeColors")
fun setSwipeRefreshLayoutSchemeColors(srl: SwipeRefreshLayout, colorStr: String?) {
    if (null == colorStr) {
        return
    }
    // 获取颜色集合
    val colorStrLs = colorStr.split(",")
    // 转换后的颜色集合
    val colorLs = arrayListOf<Int>()
    // 统计颜色
    colorStrLs.forEach {
        colorLs.add(
            if (colorStr.contains("#")) {
                // 颜色值
                Color.parseColor(it)
            } else {
                // 颜色 id 字符串
                it.getIdentifier(srl.context, "color")
            }
        )
    }
    srl.setColorSchemeColors(*colorLs.toIntArray())
}

/** 给 [srl] 设置刷新状态 [refreshing] */
@BindingAdapter("android:bind_srl_refreshing")
fun setSwipeRefreshLayoutRefreshing(srl: SwipeRefreshLayout, refreshing: Boolean) {
    srl.isRefreshing = refreshing
}

/** 获取 [srl] 的刷新状态 */
@InverseBindingAdapter(attribute = "android:bind_srl_refreshing")
fun getSwipeRefreshLayoutRefreshing(srl: SwipeRefreshLayout): Boolean {
    return srl.isRefreshing
}

/**
 * 给 [srl] 设置刷新事件 [refresh]
 * > [refresh]: () -> [Unit]
 *
 * > [listener] 为属性变化监听，`DataBinding` 自动实现，无需传递
 */
@BindingAdapter(
    "android:bind_srl_onRefresh",
    "android:bind_srl_refreshingAttrChanged",
    requireAll = false
)
fun setSwipeRefreshLayoutRefreshListener(
    srl: SwipeRefreshLayout,
    refresh: (() -> Unit)?,
    listener: InverseBindingListener?
) {
    srl.setOnRefreshListener {
        refresh?.invoke()
        listener?.onChange()
    }
}