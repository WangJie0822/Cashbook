package cn.wj.android.cashbook.databinding.adapter

import androidx.databinding.BindingAdapter
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * [SwitchMaterial]
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/10/11
 */

@BindingAdapter("android:bind_sm_onCheckedChange")
fun SwitchMaterial.setOnCheckedChangeListener(onChange: ((Boolean) -> Unit)?) {
    if (null == onChange) {
        setOnCheckedChangeListener(null)
    } else {
        setOnCheckedChangeListener { _, isChecked ->
            onChange.invoke(isChecked)
        }
    }
}