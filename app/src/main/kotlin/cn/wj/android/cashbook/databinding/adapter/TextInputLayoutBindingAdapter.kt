@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout

/**
 * TextInputLayout
 *
 * - 创建时间：2020/12/15
 *
 * @author 王杰
 */

/**
 * 设置错误提示
 */
@BindingAdapter("android:bind_til_error")
fun setError(til: TextInputLayout, error: CharSequence?) {
    if (error.isNullOrBlank()) {
        til.isErrorEnabled = false
        return
    }
    til.error = error
    til.editText?.run {
        if (!isFocusable) {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }
    }
}