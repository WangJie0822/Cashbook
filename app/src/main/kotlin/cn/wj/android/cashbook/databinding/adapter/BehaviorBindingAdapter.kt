@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import android.view.View
import androidx.databinding.BindingAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior

/*
 * Behavior DataBinding 适配器
 */

/** 底部抽屉状态变化监听 */
@BindingAdapter("android:bind_behavior_onBottomSheetStateChanged")
fun View.setOnBottomSheetBehaviorStateChanged(onStateChanged: ((Int) -> Unit)?) {
    if (null == onStateChanged) {
        return
    }
    val behavior = BottomSheetBehavior.from(this)
    behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            onStateChanged.invoke(newState)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }
    })
}

/** 底部抽屉隐藏回调 */
@BindingAdapter("android:bind_behavior_onBottomSheetHidden")
fun View.setOnBottomSheetBehaviorHidden(onHidden: (() -> Unit)?) {
    if (null == onHidden) {
        return
    }
    setOnBottomSheetBehaviorStateChanged { newState ->
        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
            onHidden.invoke()
        }
    }
}