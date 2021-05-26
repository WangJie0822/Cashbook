package cn.wj.android.cashbook.widget.recyclerview.adapter.callback

import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import cn.wj.android.cashbook.widget.recyclerview.adapter.ADAPTER_ANIM_CHANGED
import cn.wj.android.cashbook.widget.recyclerview.adapter.ADAPTER_ANIM_INSERTED
import cn.wj.android.cashbook.widget.recyclerview.adapter.ADAPTER_ANIM_MOVED
import cn.wj.android.cashbook.widget.recyclerview.adapter.ADAPTER_ANIM_REMOVED
import cn.wj.android.cashbook.widget.recyclerview.adapter.base.BaseRvAdapter
import cn.wj.android.cashbook.widget.recyclerview.adapter.base.BaseRvListAdapter

/**
 * 有动画回调
 * - 数据变化部分更新
 *
 * @param mAdapter 需要更新的 [RecyclerView.Adapter] 对象
 */
class AdapterListUpdateCallback(private val mAdapter: RecyclerView.Adapter<*>, private val anim: Int) : ListUpdateCallback {
    /** {@inheritDoc}  */
    override fun onInserted(position: Int, count: Int) {
        if (anim and ADAPTER_ANIM_INSERTED == 0) {
            mAdapter.notifyDataSetChanged()
            return
        }
        var realPosition = position
        if ((mAdapter is BaseRvAdapter<*, *> && mAdapter.hasHeader()) || (mAdapter is BaseRvListAdapter<*, *> && mAdapter.hasHeader())) {
            realPosition++
        }
        mAdapter.notifyItemRangeInserted(realPosition, count)
    }

    /** {@inheritDoc}  */
    override fun onRemoved(position: Int, count: Int) {
        if (anim and ADAPTER_ANIM_REMOVED == 0) {
            mAdapter.notifyDataSetChanged()
            return
        }
        var realPosition = position
        if ((mAdapter is BaseRvAdapter<*, *> && mAdapter.hasHeader()) || (mAdapter is BaseRvListAdapter<*, *> && mAdapter.hasHeader())) {
            realPosition++
        }
        mAdapter.notifyItemRangeRemoved(realPosition, count)
    }

    /** {@inheritDoc}  */
    override fun onMoved(fromPosition: Int, toPosition: Int) {
        if (anim and ADAPTER_ANIM_MOVED == 0) {
            mAdapter.notifyDataSetChanged()
            return
        }
        var realFromPosition = fromPosition
        var realToPosition = toPosition
        if ((mAdapter is BaseRvAdapter<*, *> && mAdapter.hasHeader()) || (mAdapter is BaseRvListAdapter<*, *> && mAdapter.hasHeader())) {
            realFromPosition++
            realToPosition++
        }
        mAdapter.notifyItemMoved(realFromPosition, realToPosition)
    }

    /** {@inheritDoc}  */
    override fun onChanged(position: Int, count: Int, payload: Any?) {
        if (anim and ADAPTER_ANIM_CHANGED == 0) {
            mAdapter.notifyDataSetChanged()
            return
        }
        var realPosition = position
        if ((mAdapter is BaseRvAdapter<*, *> && mAdapter.hasHeader()) || (mAdapter is BaseRvListAdapter<*, *> && mAdapter.hasHeader())) {
            realPosition++
        }
        mAdapter.notifyItemRangeChanged(realPosition, count, payload)
    }
}