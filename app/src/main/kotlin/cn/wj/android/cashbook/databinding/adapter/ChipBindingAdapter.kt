@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import androidx.annotation.ColorRes
import androidx.databinding.BindingAdapter
import com.google.android.material.chip.Chip

/**
 * Chip DataBinding 适配器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/31
 */

/** 图标着色 */
@BindingAdapter("android:bind_chip_checkedIconTint")
fun Chip.setIconTintRes(@ColorRes res: Int?) {
    if (null == res) {
        return
    }
    setCheckedIconTintResource(res)
}

/** 冻结选中状态 */
@BindingAdapter("android:bind_chip_freezeChecked")
fun Chip.freezeCheckedState(checked: Boolean?) {
    if (null == checked) {
        return
    }
    setOnCheckedChangeListener { _, isChecked ->
        if (isChecked != checked) {
            setChecked(checked)
        }
    }
    isChecked = checked
}