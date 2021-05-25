package cn.wj.android.cashbook.widget.recyclerview

import android.view.View
import android.view.ViewGroup

/**
 *
 * - 创建时间：2020/11/16
 *
 * @author 王杰
 */

/**
 * 移除父布局
 */
internal fun View?.removeParent() {
    (this?.parent as? ViewGroup)?.removeView(this)
}


internal fun View.like(v: View): Boolean {
    return this.javaClass == v.javaClass
            && this.id == v.id
            && (this as? ViewGroup)?.childCount == (v as? ViewGroup)?.childCount
}