@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import cn.wj.android.cashbook.base.ext.DEFAULT_CLICK_THROTTLE_MS
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.fitsStatusBar
import cn.wj.android.cashbook.base.ext.setOnThrottleClickListener

/*
 * View DataBinding 适配器
 */

/** 给 [View] 设置点击事件 [click]，传递数据 [item]，并设置重复点击拦截间隔时间 [throttle]，[throttle] 默认 [DEFAULT_CLICK_THROTTLE_MS] */
@BindingAdapter(
    "android:bind_onClick",
    "android:bind_onClick_item",
    "android:bind_onClick_throttle",
    requireAll = false
)
fun <T> View.setViewOnClick(click: ViewItemClickListener<T>?, item: T, throttle: Long?) {
    if (null == click) {
        this.setOnClickListener(null)
        return
    }
    val interval = throttle ?: DEFAULT_CLICK_THROTTLE_MS
    this.setOnThrottleClickListener({ click.onItemClick(item) }, interval)
}

/** 给 [View] 设置点击事件 [listener] 并设置重复点击拦截间隔时间 [throttle]，[throttle] 默认 [DEFAULT_CLICK_THROTTLE_MS] */
@BindingAdapter("android:bind_onClick", "android:bind_onClick_throttle", requireAll = false)
fun View.setViewOnClick(listener: ViewClickListener?, throttle: Long?) {
    if (null == listener) {
        this.setOnClickListener(null)
        return
    }
    val interval = throttle ?: DEFAULT_CLICK_THROTTLE_MS
    this.setOnThrottleClickListener({ listener.onClick() }, interval)
}

/** View 点击事件 */
interface ViewClickListener {

    /** 点击回调 */
    fun onClick()
}

/** View 点击事件，传递参数 [T] */
interface ViewItemClickListener<T> {

    /** 点击回调，传递参数 [item] */
    fun onItemClick(item: T)
}

/** 给 [View] 设置点击事件 [click] 并设置重复点击拦截间隔时间 [throttle]，[throttle] 默认 [DEFAULT_CLICK_THROTTLE_MS] */
@BindingAdapter("android:bind_onClick", "android:bind_onClick_throttle", requireAll = false)
fun View.setViewOnClick(click: ((View) -> Unit)?, throttle: Long?) {
    if (null == click) {
        this.setOnClickListener(null)
        return
    }
    val interval = throttle ?: DEFAULT_CLICK_THROTTLE_MS
    this.setOnThrottleClickListener({ click.invoke(this) }, interval)
}

/** 给 [View] 设置点击事件 [click] 并设置重复点击拦截间隔时间 [throttle]，[throttle] 默认 [DEFAULT_CLICK_THROTTLE_MS] */
@BindingAdapter("android:bind_onClick", "android:bind_onClick_throttle", requireAll = false)
fun View.setViewOnClick(click: (() -> Unit)?, throttle: Long?) {
    if (null == click) {
        this.setOnClickListener(null)
        return
    }
    val interval = throttle ?: DEFAULT_CLICK_THROTTLE_MS
    this.setOnThrottleClickListener(click, interval)
}

/** 给 [View] 设置点击事件 [click]，传递数据 [item]，并设置重复点击拦截间隔时间 [throttle]，[throttle] 默认 [DEFAULT_CLICK_THROTTLE_MS] */
@BindingAdapter(
    "android:bind_onClick",
    "android:bind_onClick_item",
    "android:bind_onClick_throttle",
    requireAll = false
)
fun <T> View.setViewOnClick(click: ((View, T) -> Unit)?, item: T, throttle: Long?) {
    if (null == click) {
        this.setOnClickListener(null)
        return
    }
    val interval = throttle ?: DEFAULT_CLICK_THROTTLE_MS
    this.setOnThrottleClickListener({ click.invoke(this, item) }, interval)
}

/** 给 [View] 设置点击事件 [click]，传递数据 [item]，并设置重复点击拦截间隔时间 [throttle]，[throttle] 默认 [DEFAULT_CLICK_THROTTLE_MS] */
@BindingAdapter(
    "android:bind_onClick",
    "android:bind_onClick_item",
    "android:bind_onClick_throttle",
    requireAll = false
)
fun <T> View.setViewOnClick(click: ((T) -> Unit)?, item: T, throttle: Long?) {
    if (null == click) {
        this.setOnClickListener(null)
        return
    }
    val interval = throttle ?: DEFAULT_CLICK_THROTTLE_MS
    this.setOnThrottleClickListener({ click.invoke(item) }, interval)
}

/** 给 [View] 设置长点击事件 [click] */
@BindingAdapter("android:bind_onLongClick")
fun View.setViewOnLongClick(click: ((View) -> Boolean)?) {
    this.setOnLongClickListener(click)
}

/** 给 [View] 设置长点击事件 [click] */
@BindingAdapter("android:bind_onLongClick")
fun View.setViewOnLongClick(click: (() -> Boolean)?) {
    if (null == click) {
        this.setOnLongClickListener(null)
        return
    }
    this.setOnLongClickListener { click.invoke() }
}

/** 给 [View] 设置长点击事件 [click] 并传递数据 [item] */
@BindingAdapter("android:bind_onLongClick", "android:bind_onLongClick_item")
fun <T> View.setViewOnLongClick(click: ((View, T) -> Boolean)?, item: T) {
    if (null == click) {
        this.setOnLongClickListener(null)
        return
    }
    this.setOnLongClickListener { click.invoke(it, item) }
}

/** 给 [View] 设置长点击事件 [click] 并传递数据 [item] */
@BindingAdapter("android:bind_onLongClick", "android:bind_onLongClick_item")
fun <T> View.setViewOnLongClick(click: ((T) -> Boolean)?, item: T) {
    if (null == click) {
        this.setOnLongClickListener(null)
        return
    }
    this.setOnLongClickListener { click.invoke(item) }
}

/** 设置 [View] 显示状态 [visibility] */
@BindingAdapter("android:bind_visibility")
fun View.setViewVisibility(visibility: Int?) {
    if (null == visibility) {
        return
    }
    if (this.visibility == visibility) {
        return
    }
    this.visibility = visibility
}

/** 根据是否显示 [show]，是否移除 [gone] 设置 [View] 的显示状态 */
@BindingAdapter("android:bind_visibility", "android:bind_visibility_gone", requireAll = false)
fun View.setViewVisibility(show: Boolean?, gone: Boolean? = true) {
    val visibility =
        if (show.condition) View.VISIBLE else if (gone != false) View.GONE else View.INVISIBLE
    if (this.visibility == visibility) {
        return
    }
    this.visibility = visibility
}

/** 设置 [View] 的选中状态为 [selected] */
@BindingAdapter("android:bind_selected")
fun View.setViewSelected(selected: Boolean?) {
    if (this.isSelected == selected) {
        return
    }
    this.isSelected = selected.condition
}

/** 设置 [View] 的启用状态为 [enable] */
@BindingAdapter("android:bind_enable")
fun View.setViewEnable(enable: Boolean?) {
    this.isEnabled = enable.condition
}

/**
 * 设置 [View] 能否获取焦点 [focusable]
 * > [listener] 为属性变化监听，`DataBinding` 自动实现
 */
@BindingAdapter("android:bind_focusable", "android:bind_focusableAttrChanged", requireAll = false)
fun View.setViewFocusable(focusable: Boolean?, listener: InverseBindingListener?) {
    if (this.isFocusable == focusable) {
        return
    }
    this.isFocusable = focusable.condition
    listener?.onChange()
}

/** 获取 [View] 能否获取焦点 */
@InverseBindingAdapter(attribute = "android:bind_focusable")
fun View.getViewFocusable(): Boolean {
    return this.isFocusable
}

/**
 * 给 [View] 设置焦点变化监听 [onChange]
 * > [listener] 为属性变化监听，`DataBinding` 自动实现
 */
@BindingAdapter(
    "android:bind_focus_change",
    "android:bind_focusableAttrChanged",
    requireAll = false
)
fun View.setViewFocusableListener(
    onChange: ((Boolean) -> Unit)?,
    listener: InverseBindingListener?
) {
    if (null == onChange) {
        this.onFocusChangeListener = null
        return
    }
    this.setOnFocusChangeListener { _, hasFocus ->
        onChange.invoke(hasFocus)
        listener?.onChange()
    }
}

/**
 * 给 [View] 设置焦点变化监听 [onChange]
 * > [listener] 为属性变化监听，`DataBinding` 自动实现
 */
@BindingAdapter(
    "android:bind_focus_change",
    "android:bind_focusableAttrChanged",
    requireAll = false
)
fun View.setViewFocusableListener(
    onChange: ((View, Boolean) -> Unit)?,
    listener: InverseBindingListener?
) {
    if (null == onChange) {
        this.onFocusChangeListener = null
        return
    }
    this.setOnFocusChangeListener { _, hasFocus ->
        onChange.invoke(this, hasFocus)
        listener?.onChange()
    }
}

/** 给 [View] 设置触摸事件监听 */
@BindingAdapter("android:bind_onTouch")
fun View.setViewOnTouch(onTouch: ((View, MotionEvent) -> Boolean)?) {
    this.setOnTouchListener(onTouch)
}

/** 给 [View] 设置触摸事件监听 */
@SuppressLint("ClickableViewAccessibility")
@BindingAdapter("android:bind_onTouch")
fun View.setViewOnTouch(onTouch: ((MotionEvent) -> Boolean)?) {
    if (null == onTouch) {
        this.setOnTouchListener(null)
        return
    }
    this.setOnTouchListener { _, ev ->
        onTouch.invoke(ev)
    }
}

/** 给 [View] 设置触摸事件监听 */
@SuppressLint("ClickableViewAccessibility")
@BindingAdapter("android:bind_onTouch")
fun View.setViewOnTouch(onTouch: (() -> Boolean)?) {
    if (null == onTouch) {
        this.setOnTouchListener(null)
        return
    }
    this.setOnTouchListener { _, _ ->
        onTouch.invoke()
    }
}

/** 给 [View] 设置触摸事件监听，并传递数据 [item] */
@SuppressLint("ClickableViewAccessibility")
@BindingAdapter("android:bind_onTouch", "android:bind_onTouch_item")
fun <T> View.setViewOnTouch(onTouch: ((View, MotionEvent, T) -> Boolean)?, item: T) {
    if (null == onTouch) {
        this.setOnTouchListener(null)
        return
    }
    this.setOnTouchListener { _, event ->
        onTouch.invoke(this, event, item)
    }
}

/** 给 [View] 设置触摸事件监听，并传递数据 [item] */
@SuppressLint("ClickableViewAccessibility")
@BindingAdapter("android:bind_onTouch", "android:bind_onTouch_item")
fun <T> View.setViewOnTouch(onTouch: ((MotionEvent, T) -> Boolean)?, item: T) {
    if (null == onTouch) {
        this.setOnTouchListener(null)
        return
    }
    this.setOnTouchListener { _, ev ->
        onTouch.invoke(ev, item)
    }
}

/** 给 [View] 设置触摸事件监听，并传递数据 [item] */
@SuppressLint("ClickableViewAccessibility")
@BindingAdapter("android:bind_onTouch", "android:bind_onTouch_item")
fun <T> View.setViewOnTouch(onTouch: ((T) -> Boolean)?, item: T) {
    if (null == onTouch) {
        this.setOnTouchListener(null)
        return
    }
    this.setOnTouchListener { _, _ ->
        onTouch.invoke(item)
    }
}

/** 根据颜色字符串 [color] 设置 [View] 的背景，颜色字符串 `"#FFFFFF"` */
@BindingAdapter("android:bind_background")
fun View.setBackground(color: String?) {
    if (!color.isNullOrBlank()) {
        this.setBackgroundColor(Color.parseColor(color))
    }
}

/** 根据资源id [resId] 设置 [View] 的背景 */
@BindingAdapter("android:bind_background")
fun View.setBackgroundRes(resId: Int?) {
    if (null == resId || 0 == resId) {
        this.background = null
    } else {
        this.setBackgroundResource(resId)
    }
}

/**
 * 给 [View] 设置 z 轴高度 [elevation]
 * > 仅 API >= [Build.VERSION_CODES.LOLLIPOP] 有效
 */
@BindingAdapter("android:bind_elevation")
fun View.setElevation(elevation: Float?) {
    if (null == elevation) {
        return
    }
    ViewCompat.setElevation(this, elevation)
}

/** 将 [View] 的透明度设置为 [alpha] */
@BindingAdapter("android:bind_alpha")
fun View.setAlpha(alpha: Float?) {
    if (null == alpha) {
        return
    }
    this.alpha = alpha
}

/**
 * 给 [View] 设置比例约束 [ratio] eg: `"h,2:1"` or `"w,2:1"`
 * > [ratio]  `"约束对象,宽:高"`
 */
@BindingAdapter("android:bind_ratio")
fun View.setViewDimensionRatio(ratio: String?) {
    if (ratio.isNullOrBlank()) {
        return
    }
    val params = ratio.split(",")
    if (params.isNullOrEmpty()) {
        return
    }
    // 获取比例属性
    val values = if (params.size > 1) {
        params[1]
    } else {
        params[0]
    }.split(":")
    val start = values[0].toIntOrNull().orElse(0)
    val end = values[1].toIntOrNull().orElse(0)
    if (start == 0 || end == 0) {
        // 属性不合规
        return
    }
    // 获取约束条件
    var constraint = if (params.size > 1) {
        params[0]
    } else {
        "h"
    }
    if (constraint != "h" || constraint != "w") {
        // 约束不合规，默认高度约束
        constraint = "h"
    }
    // 更新约束尺寸
    this.post {
        this.layoutParams = this.layoutParams.apply {
            if (constraint == "h") {
                height = this.width / start * end
            } else {
                width = this.height / end * start
            }
        }
    }
}

/**
 * 设置 [View] 是否适应状态栏
 * > [View] 为布局中最上面的 [View] 且布局延伸到状态栏下方时，添加状态栏高度，并添加状态栏高度的 paddingTop，
 * 以适应沉浸式体验
 */
@BindingAdapter("android:bind_fits_status_bar")
fun View.fitsStatusBar(fits: Boolean?) {
    if (!fits.condition) {
        return
    }
    this.fitsStatusBar()
}