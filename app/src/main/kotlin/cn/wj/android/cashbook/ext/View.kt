@file:Suppress("unused")
@file:JvmName("ViewExt")

package cn.wj.android.cashbook.ext

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import cn.wj.android.cashbook.R

/** 默认点击过滤间隔时间，单位：ms */
const val DEFAULT_CLICK_THROTTLE_MS = 250L


/** 隐藏软键盘 */
fun View?.hideSoftKeyboard() {
    if (this == null) {
        return
    }
    this.clearFocus()
    (this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
        .hideSoftInputFromWindow(this.windowToken, 0)
}

/**
 * 根据间隔时间[interval]处理点击事件[onClick]
 * > [interval] 默认值 [DEFAULT_CLICK_THROTTLE_MS]
 *
 * > 这里的 View 只用于快速点击判断，只有两次点击间隔超过[interval]，[onClick]事件才会响应
 */
@JvmOverloads
inline fun View.disposeThrottleClick(
    onClick: () -> Unit,
    interval: Long = DEFAULT_CLICK_THROTTLE_MS
) {
    if (interval > 0) {
        val lastTime = (this.getTag(R.id.view_click_tag) as? Long) ?: 0L
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTime > interval) {
            onClick.invoke()
            this.setTag(R.id.view_click_tag, currentTime)
        }
    } else {
        onClick.invoke()
    }
}

/**
 * 设置点击事件
 * > 在间隔时间内重复点击事件将被过滤
 *
 * > [interval] 间隔事件，默认[DEFAULT_CLICK_THROTTLE_MS]
 */
@JvmOverloads
inline fun View.setOnThrottleClickListener(
    crossinline onClick: () -> Unit,
    interval: Long = DEFAULT_CLICK_THROTTLE_MS
) {
    this.setOnClickListener {
        this.disposeThrottleClick({
            onClick.invoke()
        }, interval)
    }
}