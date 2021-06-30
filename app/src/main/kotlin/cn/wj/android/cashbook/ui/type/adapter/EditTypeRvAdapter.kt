package cn.wj.android.cashbook.ui.type.adapter

import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
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
        TypeEntity>(diffCallback = object : DiffUtil.ItemCallback<TypeEntity>() {
    override fun areItemsTheSame(oldItem: TypeEntity, newItem: TypeEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TypeEntity, newItem: TypeEntity): Boolean {
        return oldItem.toString() == newItem.toString()
    }
}) {

    override val layoutResId: Int = R.layout.recycler_item_edit_type_first

    override fun convert(holder: BaseRvViewHolder<TypeEntity>, entity: TypeEntity) {
        super.convert(holder, entity)

        (holder as? ViewHolder)?.mBinding?.rvSecond?.run {
            layoutManager = GridLayoutManager(context, 4)
            val secondRvAdapter = SimpleRvListAdapter<TypeEntity>(R.layout.recycler_item_edit_type_second, areItemsTheSame = { old, new -> old.id == new.id })
            adapter = ConcatAdapter(
                secondRvAdapter.apply {
                    this.viewModel = this@EditTypeRvAdapter.viewModel
                    submitList(entity.childList)
                },
                OneItemAdapter(R.layout.recycler_footer_add_second_type) {
                    this@EditTypeRvAdapter.viewModel?.onAddSecondTypeClick?.invoke(entity)
                }
            )
            val helper = ItemTouchHelper(DragItemTouchCallback(secondRvAdapter) { ls ->
                viewModel?.edit?.value = true
                val index = changedList.indexOfFirst { it.id == entity.id }
                if (index >= 0) {
                    changedList[index] = entity.copy(childList = ls)
                }
            })
            helper.attachToRecyclerView(this)
        }
    }

    /** 用于统计二级分类变化的数据列表 */
    private val changedList = arrayListOf<TypeEntity>()

    /** 获取并返回排序之后的数据列表 */
    fun getChangeList(): List<TypeEntity> {
        val ls = arrayListOf<TypeEntity>()
        mDiffer.currentList.forEach { first ->
            val childLs = changedList.firstOrNull { it.id == first.id }?.childList.orEmpty()
            ls.add(first.copy(childList = childLs))
        }
        return ls
    }

    override fun submitList(list: List<TypeEntity>?) {
        super.submitList(list)
        changedList.clear()
        changedList.addAll(list.orEmpty())
    }

    class ViewHolder(binding: RecyclerItemEditTypeFirstBinding) : BaseRvDBViewHolder<RecyclerItemEditTypeFirstBinding, TypeEntity>(binding)
}