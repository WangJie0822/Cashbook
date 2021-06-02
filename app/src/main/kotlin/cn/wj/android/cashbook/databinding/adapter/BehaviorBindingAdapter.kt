@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.google.android.material.bottomsheet.BottomSheetBehavior

/*
 * Behavior DataBinding 适配器
 */

/** 抽屉状态 */
@set:BindingAdapter("android:bind_behavior_bottomSheetState")
@get:InverseBindingAdapter(
    attribute = "android:bind_behavior_bottomSheetState",
    event = "android:bind_behavior_bottomSheetStateAttrChanged"
)
var View.bottomSheetState: Int
    get() = BottomSheetBehavior.from(this).state
    set(value) {
        BottomSheetBehavior.from(this).state = value
    }

/** 底部抽屉状态变化监听 */
@BindingAdapter("android:bind_behavior_onBottomSheetStateChanged", "android:bind_behavior_bottomSheetStateAttrChanged", requireAll = false)
fun View.setOnBottomSheetBehaviorStateChanged(onStateChanged: ((Int) -> Unit)?, listener: InverseBindingListener?) {
    if (null == onStateChanged) {
        return
    }
    val behavior = BottomSheetBehavior.from(this)
    behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            onStateChanged.invoke(newState)
            listener?.onChange()
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }
    })
}

/** 底部抽屉隐藏回调 */
@BindingAdapter("android:bind_behavior_onBottomSheetHidden", "android:bind_behavior_bottomSheetStateAttrChanged", requireAll = false)
fun View.setOnBottomSheetBehaviorHidden(onHidden: (() -> Unit)?, listener: InverseBindingListener?) {
    if (null == onHidden) {
        return
    }
    setOnBottomSheetBehaviorStateChanged({ newState ->
        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
            onHidden.invoke()
        }
    }, listener)
}