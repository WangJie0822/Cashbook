@file:Suppress("unused")

package cn.wj.android.cashbook.widget.calculator

import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData

/**
 *
 * > [王杰](mailto:15555650921@163.com) 创建于2021/5/30
 */

/** 绑定计算器显示 */
@BindingAdapter("android:bind_cv_calculator_text")
fun CalculatorView.bindStr(field: ObservableField<String>?) {
    if (null == field) {
        return
    }
    this.bindCalculatorStr(field)
}

/** 绑定计算器显示 */
@BindingAdapter("android:bind_cv_calculator_text")
fun CalculatorView.bindStr(liveData: MutableLiveData<String>?) {
    if (null == liveData) {
        return
    }
    this.bindCalculatorStr(liveData)
}

/** 绑定等于号背景着色 */
@BindingAdapter("android:bind_cv_equals_backgroundTint")
fun CalculatorView.bindEqualsBackgroundTint(color: Int?) {
    if (null == color) {
        return
    }
    this.setEqualsBackground(color)
}