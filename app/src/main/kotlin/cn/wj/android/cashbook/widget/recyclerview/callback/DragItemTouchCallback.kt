package cn.wj.android.cashbook.widget.recyclerview.callback

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import cn.wj.android.cashbook.base.ext.base.toNewList
import cn.wj.android.cashbook.widget.recyclerview.adapter.base.BaseRvListAdapter
import java.util.Collections

/**
 * 支持拖拽的回调
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/24
 */
class DragItemTouchCallback<T>(private val adapter: BaseRvListAdapter<*, T>, private val onMoved: ((List<T>) -> Unit)? = null) :
    ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
        ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    ) {

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val fromPosition = viewHolder.bindingAdapterPosition
        val toPosition = target.bindingAdapterPosition
        val ls = adapter.mDiffer.currentList.toNewList()
        Collections.swap(ls, fromPosition, toPosition)
        adapter.submitList(ls)
        onMoved?.invoke(ls)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
    }
}