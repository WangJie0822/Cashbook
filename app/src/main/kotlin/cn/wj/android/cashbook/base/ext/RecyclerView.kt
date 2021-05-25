@file:Suppress("unused")

package cn.wj.android.cashbook.base.ext

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 * 判断是否在最顶部
 */
fun RecyclerView.isTop(): Boolean {
    return when (val layoutManager = this.layoutManager) {
        is LinearLayoutManager -> 0 == layoutManager.findFirstCompletelyVisibleItemPosition()
        is GridLayoutManager -> 0 == layoutManager.findFirstCompletelyVisibleItemPosition()
        is StaggeredGridLayoutManager -> 0 in layoutManager.findFirstCompletelyVisibleItemPositions(null)
        else -> false
    }
}

/**
 * 获取最后一个显示的 Item 位置
 */
fun RecyclerView.getLastVisiblePosition(): Int {
    return when (val layoutManager = this.layoutManager) {
        is LinearLayoutManager -> layoutManager.findLastVisibleItemPosition()
        is GridLayoutManager -> layoutManager.findLastVisibleItemPosition()
        is StaggeredGridLayoutManager -> layoutManager.findLastVisibleItemPositions(null).maxOrNull()
                ?: -1
        else -> -1
    }
}

/**
 * 滑动到最顶部
 *
 * @param smooth 是否平滑滚动
 */
fun RecyclerView.scrollToTop(smooth: Boolean = false) {
    if (this.adapter != null) {
        if (smooth) {
            this.smoothScrollToPosition(0)
        } else {
            this.scrollToPosition(0)
        }
    }
}

/**
 * 滑动到底部
 *
 * @param smooth 是否平滑滚动
 */
fun RecyclerView.scrollToBottom(smooth: Boolean = false) {
    if (this.adapter != null) {
        if (smooth) {
            this.smoothScrollToPosition(this.adapter!!.itemCount - 1)
        } else {
            this.scrollToPosition(this.adapter!!.itemCount - 1)
        }
    }
}