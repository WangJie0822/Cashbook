package cn.wj.android.cashbook.ui.record.adapter

import androidx.recyclerview.widget.DiffUtil
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.data.entity.DateRecordEntity
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.databinding.RecyclerItemRecordDateBinding
import cn.wj.android.cashbook.interfaces.RecordListClickListener
import cn.wj.android.cashbook.widget.recyclerview.adapter.base.BaseRvListDBAdapter
import cn.wj.android.cashbook.widget.recyclerview.adapter.simple.SimpleRvListAdapter
import cn.wj.android.cashbook.widget.recyclerview.holder.BaseRvDBViewHolder
import cn.wj.android.cashbook.widget.recyclerview.holder.BaseRvViewHolder
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager

/**
 * 首页数据列表适配器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/11
 */
class DateRecordRvAdapter : BaseRvListDBAdapter<
        DateRecordRvAdapter.ViewHolder,
        RecyclerItemRecordDateBinding,
        RecordListClickListener,
        DateRecordEntity>(diffCallback = object : DiffUtil.ItemCallback<DateRecordEntity>() {
    override fun areItemsTheSame(oldItem: DateRecordEntity, newItem: DateRecordEntity): Boolean {
        return oldItem.date == newItem.date
    }

    override fun areContentsTheSame(oldItem: DateRecordEntity, newItem: DateRecordEntity): Boolean {
        return oldItem.list.toString() == newItem.list.toString()
    }
}) {

    override val layoutResId: Int = R.layout.recycler_item_record_date

    override fun convert(holder: BaseRvViewHolder<DateRecordEntity>, entity: DateRecordEntity) {
        super.convert(holder, entity)
        (holder as ViewHolder).run {
            mBinding.rvRecord.run {
                layoutManager = WrapContentLinearLayoutManager()
                adapter = SimpleRvListAdapter<RecordEntity>(R.layout.recycler_item_record).apply {
                    this.viewModel = this@DateRecordRvAdapter.viewModel
                    submitList(entity.list)
                }
            }
        }
    }

    class ViewHolder(binding: RecyclerItemRecordDateBinding) : BaseRvDBViewHolder<RecyclerItemRecordDateBinding, DateRecordEntity>(binding)
}