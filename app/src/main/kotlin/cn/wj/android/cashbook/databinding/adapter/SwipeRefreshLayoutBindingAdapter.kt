@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cn.wj.android.cashbook.base.ext.base.color
import cn.wj.android.cashbook.databinding.constants.COLOR_MARK
import cn.wj.android.cashbook.databinding.constants.RESOURCE_MARK
import cn.wj.android.databinding.adapter.getIdentifier

/*
 * SwipeRefreshLayout DataBinding 适配器
 */

/** 为 [SwipeRefreshLayout] 设置进度条颜色 [color] */
@BindingAdapter("android:bind_srl_schemeColorsInt")
fun SwipeRefreshLayout.setSwipeRefreshLayoutSchemeColors(@ColorInt color: Int?) {
    if (null == color) {
        return
    }
    setColorSchemeColors(color)
}

/**
 * 为 [SwipeRefreshLayout] 设置进度条颜色 [colorStr]
 * > [colorStr] 多个使用 `,` 分隔，可使用颜色id `"app_color_white"` 或色值 `"#FFFFFF"`
 */
@BindingAdapter("android:bind_srl_schemeColorsStr")
fun SwipeRefreshLayout.setSwipeRefreshLayoutSchemeColors(colorStr: String?) {
    if (null == colorStr) {
        return
    }
    // 获取颜色集合
    val colorStrLs = colorStr.split(",")
    // 转换后的颜色集合
    val colorLs = arrayListOf<Int>()
    // 统计颜色
    colorStrLs.forEach {
        val value = it.trim()
        if (colorStr.startsWith(COLOR_MARK)) {
            // 颜色值
            colorLs.add(Color.parseColor(value))
        } else if (colorStr.startsWith(RESOURCE_MARK)) {
            // 颜色 id 字符串
            val colorId = value.getIdentifier(context)
            if (colorId != 0) {
                colorLs.add(colorId.color)
            }
        }
    }
    setColorSchemeColors(*colorLs.toIntArray())
}

/** [SwipeRefreshLayout] 刷新状态 */
@set:BindingAdapter("android:bind_srl_refreshing")
@get:InverseBindingAdapter(
    attribute = "android:bind_srl_refreshing",
    event = "android:bind_srl_refreshingAttrChanged"
)
var SwipeRefreshLayout.refreshing: Boolean
    get() = isRefreshing
    set(value) {
        isRefreshing = value
    }

/**
 * 给 [SwipeRefreshLayout] 设置刷新事件 [refresh]
 * > [refresh]: () -> [Unit]
 *
 * > [listener] 为属性变化监听，`DataBinding` 自动实现，无需传递
 */
@BindingAdapter(
    "android:bind_srl_onRefresh",
    "android:bind_srl_refreshingAttrChanged",
    requireAll = false
)
fun SwipeRefreshLayout.setSwipeRefreshLayoutRefreshListener(
    refresh: (() -> Unit)?,
    listener: InverseBindingListener?
) {
    setOnRefreshListener {
        refresh?.invoke()
        listener?.onChange()
    }
}