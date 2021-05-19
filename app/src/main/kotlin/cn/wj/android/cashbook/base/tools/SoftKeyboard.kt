@file:JvmName("SoftKeyboardTools")
@file:Suppress("unused")

package cn.wj.android.cashbook.base.tools

import android.view.MotionEvent
import android.view.View
import android.widget.EditText

/** 根据当前焦点控件[v]、触摸事件[ev]判断是否需要隐藏软键盘 */
fun shouldHideInput(v: View?, ev: MotionEvent): Boolean {
    if (v is EditText) {
        // 是输入框
        val leftTop = intArrayOf(0, 0)
        // 获取输入框当前的位置
        v.getLocationInWindow(leftTop)
        val top = leftTop[1]
        val bottom = top + v.height
        // 触摸位置不在输入框范围内，需要隐藏
        return !(ev.y > top && ev.y < bottom)
    }
    return false
}