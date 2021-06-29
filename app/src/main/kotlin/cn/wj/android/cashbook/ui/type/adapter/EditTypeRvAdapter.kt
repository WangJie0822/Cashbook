package cn.wj.android.cashbook.ui.type.adapter

import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.databinding.RecyclerItemEditTypeFirstBinding
import cn.wj.android.cashbook.ui.general.adapter.OneItemAdapter
import cn.wj.android.cashbook.ui.type.viewmodel.TypListViewModel
import cn.wj.android.cashbook.widget.recyclerview.adapter.base.BaseRvListDBAdapter
import cn.wj.android.cashbook.widget.recyclerview.adapter.simple.SimpleRvListAdapter
import cn.wj.android.cashbook.widget.recyclerview.callback.DragItemTouchCallback
import cn.wj.android.cashbook.widget.recyclerview.holder.BaseRvDBViewHolder
import cn.wj.android.cashbook.widget.recyclerview.holder.BaseRvViewHolder

/**
 * 编辑类型列表适配器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/29
 */
class EditTypeRvAdapter : BaseRvListDBAdapter<
        EditTypeRvAdapter.ViewHolder,
        RecyclerItemEditTypeFirstBinding,
        TypListViewModel,
        TypeEntity>() {

    override val layoutResId: Int = R.layout.recycler_item_edit_type_first

    override fun convert(holder: BaseRvViewHolder<TypeEntity>, entity: TypeEntity) {
        super.convert(holder, entity)

        (holder as? ViewHolder)?.mBinding?.rvSecond?.run {
            layoutManager = GridLayoutManager(context, 4)
            val secondRvAdapter = SimpleRvListAdapter<TypeEntity>(R.layout.recycler_item_edit_type_second)
            adapter = ConcatAdapter(
                secondRvAdapter.apply {
                    this.viewModel = this@EditTypeRvAdapter.viewModel
                    submitList(entity.childList)
                },
                OneItemAdapter(R.layout.recycler_footer_add_second_type) {
                    this@EditTypeRvAdapter.viewModel?.onAddSecondTypeClick?.invoke(entity)
                }
            )
            val helper = ItemTouchHelper(DragItemTouchCallback(secondRvAdapter))
            helper.attachToRecyclerView(this)
        }
    }

    class ViewHolder(binding: RecyclerItemEditTypeFirstBinding) : BaseRvDBViewHolder<RecyclerItemEditTypeFirstBinding, TypeEntity>(binding)
}