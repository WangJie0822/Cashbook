package cn.wj.android.cashbook.widget.recyclerview.adapter.callback

import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView

/**
 * 无动画回调
 * - 数据变化直接更新
 *
 * @param mAdapter 需要更新的 [RecyclerView.Adapter] 对象
 */
class AdapterListUpdateNoAnimCallback(private val mAdapter: RecyclerView.Adapter<*>)
    : ListUpdateCallback {

    /** {@inheritDoc}  */
    override fun onInserted(position: Int, count: Int) {
        mAdapter.notifyDataSetChanged()
    }

    /** {@inheritDoc}  */
    override fun onRemoved(position: Int, count: Int) {
        mAdapter.notifyDataSetChanged()
    }

    /** {@inheritDoc}  */
    override fun onMoved(fromPosition: Int, toPosition: Int) {
        mAdapter.notifyDataSetChanged()
    }

    /** {@inheritDoc}  */
    override fun onChanged(position: Int, count: Int, payload: Any?) {
        mAdapter.notifyDataSetChanged()
    }
}
