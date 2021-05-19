@file:Suppress("unused")
@file:JvmName("EditTextExt")

package cn.wj.android.cashbook.base.ext

import android.content.Context
import android.os.Build
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import cn.wj.android.cashbook.base.ext.base.logger

/** 显示软键盘 */
fun EditText.showSoftKeyboard() {
    this.isFocusable = true
    this.isFocusableInTouchMode = true
    this.requestFocus()
    (this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
        this,
        InputMethodManager.SHOW_FORCED
    )
}

/** 设置[EditText]在获取焦点时是否弹起软键盘[show] */
fun EditText.showSoftInputOnFocus(show: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.showSoftInputOnFocus = show
    } else {
        try {
            val cls = EditText::class.java
            val setShowSoftInputOnFocus =
                cls.getMethod("setShowSoftInputOnFocus", Boolean::class.javaPrimitiveType)
            setShowSoftInputOnFocus.isAccessible = false
            setShowSoftInputOnFocus.invoke(this, show)
        } catch (e: Exception) {
            logger("EditTextExt").e(e, "showSoftInputOnFocus")
        }
    }
}