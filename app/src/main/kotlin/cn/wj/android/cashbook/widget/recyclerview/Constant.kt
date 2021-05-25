@file:Suppress("unused")

package cn.wj.android.cashbook.widget.recyclerview

import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil

/** 布局类型 - 头布局 */
const val VIEW_TYPE_HEADER = 0x0101551

/** 布局类型 - 正常 */
const val VIEW_TYPE_NORMAL = 0x1011552

/** 布局类型 - 脚布局 */
const val VIEW_TYPE_FOOTER = 0x0101553

/** 布局类型 - 空布局 */
const val VIEW_TYPE_EMPTY = 0x0101554

fun <E> defaultDiffCallback(): DiffUtil.ItemCallback<E> {
    return object : DiffUtil.ItemCallback<E>() {
        override fun areItemsTheSame(oldItem: E, newItem: E): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: E, newItem: E): Boolean {
            return oldItem.toString() == newItem.toString()
        }
    }
}

fun <E> defaultDiffConfig(): AsyncDifferConfig<E> {
    return AsyncDifferConfig.Builder(defaultDiffCallback<E>()).build()
}