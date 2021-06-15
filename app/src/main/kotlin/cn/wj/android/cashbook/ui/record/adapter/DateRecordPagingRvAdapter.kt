package cn.wj.android.cashbook.ui.record.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import cn.wj.android.cashbook.BR
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.data.entity.DateRecordEntity
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.databinding.RecyclerItemRecordDateBinding
import cn.wj.android.cashbook.interfaces.RecordListClickListener
import cn.wj.android.cashbook.widget.recyclerview.adapter.simple.SimpleRvListAdapter
import cn.wj.android.cashbook.widget.recyclerview.holder.BaseRvDBViewHolder
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager

/**
 * 首页数据列表适配器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/11
 */
class DateRecordPagingRvAdapter : PagingDataAdapter<DateRecordEntity, DateRecordPagingRvAdapter.ViewHolder>(diffCallback = object : DiffUtil.ItemCallback<DateRecordEntity>() {
    override fun areItemsTheSame(oldItem: DateRecordEntity, newItem: DateRecordEntity): Boolean {
        return oldItem.date == newItem.date
    }

    override fun areContentsTheSame(oldItem: DateRecordEntity, newItem: DateRecordEntity): Boolean {
        return oldItem.list.toString() == newItem.list.toString()
    }
}) {

    var viewModel: RecordListClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RecyclerItemRecordDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.setVariable(BR.viewModel, viewModel)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entity = getItem(position) ?: return
        holder.bindData(entity)
        holder.mBinding.rvRecord.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = SimpleRvListAdapter<RecordEntity>(R.layout.recycler_item_record).apply {
                this.viewModel = this@DateRecordPagingRvAdapter.viewModel
                submitList(entity.list)
            }
        }
    }

    class ViewHolder(binding: RecyclerItemRecordDateBinding) : BaseRvDBViewHolder<RecyclerItemRecordDateBinding, DateRecordEntity>(binding)
}