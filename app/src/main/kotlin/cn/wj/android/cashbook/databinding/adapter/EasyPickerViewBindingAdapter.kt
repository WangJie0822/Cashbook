@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import cn.wj.android.cashbook.widget.picker.EasyPickerView

/*
 * EasyPickerView DataBinding 适配器
 */

/**
 * 给 [EasyPickerView] 添加滚动监听 [onChanged] & [onFinished]
 * > [listener] 为属性变化监听，`DataBinding` 自动实现
 */
@BindingAdapter(
    "android:bind_epv_onChanged",
    "android:bind_epv_onFinished",
    requireAll = false
)
fun EasyPickerView.setOnScrollChangedListener(
    onChanged: ((Int) -> Unit)?,
    onFinished: ((Int) -> Unit)?
) {
    setOnScrollChangedListener(object : EasyPickerView.OnScrollChangedListener {
        override fun onScrollChanged(curIndex: Int) {
            onChanged?.invoke(curIndex)
        }

        override fun onScrollFinished(curIndex: Int) {
            onFinished?.invoke(curIndex)
        }
    })
}
