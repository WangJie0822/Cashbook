@file:Suppress("unused")

package cn.wj.android.cashbook.widget.recyclerview.layoutmanager

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * [RecyclerView] 线性布局
 * * 处理[IndexOutOfBoundsException]异常
 *
 * @author 王杰
 */
class WrapContentLinearLayoutManager
    : LinearLayoutManager {

    @JvmOverloads
    constructor(orientation: Int = RecyclerView.VERTICAL, reverseLayout: Boolean = false) : super(
            null,
            orientation,
            reverseLayout
    )

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
            context,
            attrs,
            defStyleAttr,
            defStyleRes
    )

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
    
    }
}
