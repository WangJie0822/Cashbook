package cn.wj.android.cashbook.ui.record.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import cn.wj.android.cashbook.BR
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.databinding.RecyclerItemRecordBinding
import cn.wj.android.cashbook.interfaces.RecordListClickListener
import cn.wj.android.cashbook.widget.recyclerview.holder.BaseRvDBViewHolder

/**
 * 搜索记录列表适配器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/24
 */
class SearchRecordPagingRvAdapter : PagingDataAdapter<RecordEntity, SearchRecordPagingRvAdapter.ViewHolder>(diffCallback = object : DiffUtil.ItemCallback<RecordEntity>() {
    override fun areItemsTheSame(oldItem: RecordEntity, newItem: RecordEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: RecordEntity, newItem: RecordEntity): Boolean {
        return oldItem.toString() == newItem.toString()
    }
}) {

    var viewModel: RecordListClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RecyclerItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.setVariable(BR.viewModel, viewModel)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entity = getItem(position) ?: return
        holder.bindData(entity)
    }

    class ViewHolder(binding: RecyclerItemRecordBinding) : BaseRvDBViewHolder<RecyclerItemRecordBinding, RecordEntity>(binding)
}