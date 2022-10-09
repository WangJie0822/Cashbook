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
fun TextInputLayout.setErrorHint(error: CharSequence?) {
    if (error.isNullOrBlank()) {
        isErrorEnabled = false
        return
    }
    this.error = error
    editText?.run {
        if (!isFocusable) {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }
    }
}

/** 设置提示文本 */
@BindingAdapter("android:bind_til_hint")
fun TextInputLayout.setHintCS(text: CharSequence?) {
    hint = text
}

/** 设置后方按钮点击事件 */
@BindingAdapter("android:bind_til_onEndIconClick")
fun TextInputLayout.setOnEndIconClick(click: (() -> Unit)?) {
    if (null == click) {
        setEndIconOnClickListener(null)
    } else {
        setEndIconOnClickListener {
            click.invoke()
        }
    }
}