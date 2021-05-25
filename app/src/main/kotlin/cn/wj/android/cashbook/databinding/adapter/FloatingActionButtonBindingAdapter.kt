@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import androidx.databinding.BindingAdapter
import cn.wj.android.cashbook.base.ext.base.condition
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

/**
 * FloatingActionButton DataBinding 适配器
 *
 * - 创建时间：2021/1/18
 *
 * @author 王杰
 */

/**
 * 设置 [ExtendedFloatingActionButton] 是否展开 [extend]
 */
@BindingAdapter("android:bind_efab_extend")
fun ExtendedFloatingActionButton.setExtendedFloatingActionButtonStrategy(extend: Boolean?) {
    if (extend.condition) {
        extend()
    } else {
        shrink()
    }
}