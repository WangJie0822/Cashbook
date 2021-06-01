@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.databinding.BindingAdapter

/**
 * MotionLayout DataBinding 适配器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/1
 */

/** 绑定开始结束状态切换 */
@BindingAdapter("android:bind_ml_transToStart")
fun MotionLayout.transToStart(start: Boolean?) {
    if (null == start) {
        return
    }
    if (start) {
        transitionToStart()
    } else {
        transitionToEnd()
    }
}