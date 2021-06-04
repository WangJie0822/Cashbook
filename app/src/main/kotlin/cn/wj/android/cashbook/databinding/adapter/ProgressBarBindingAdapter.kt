@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import android.os.Build
import android.widget.ProgressBar
import androidx.databinding.BindingAdapter
import cn.wj.android.cashbook.base.ext.base.condition

/*
 * ProgressBar DataBinding 适配器
 */

/** 设置进度 */
@BindingAdapter("android:bind_pb_progress", "android:bind_pb_animate", requireAll = false)
fun ProgressBar.setDbProgress(progress: Int?, animate: Boolean?) {
    if (null == progress) {
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        setProgress(progress, animate.condition)
    } else {
        setProgress(progress)
    }
}