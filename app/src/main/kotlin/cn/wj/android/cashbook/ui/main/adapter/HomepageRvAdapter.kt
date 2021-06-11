package cn.wj.android.cashbook.ui.main.adapter

import androidx.recyclerview.widget.DiffUtil
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.data.entity.HomepageEntity
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.databinding.RecyclerItemRecordDayBinding
import cn.wj.android.cashbook.ui.main.viewmodel.MainViewModel
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
class HomepageRvAdapter : BaseRvListDBAdapter<
        HomepageRvAdapter.ViewHolder,
        RecyclerItemRecordDayBinding,
        MainViewModel,
        HomepageEntity>(diffCallback = object : DiffUtil.ItemCallback<HomepageEntity>() {
    override fun areItemsTheSame(oldItem: HomepageEntity, newItem: HomepageEntity): Boolean {
        return oldItem.date == newItem.date
    }

    override fun areContentsTheSame(oldItem: HomepageEntity, newItem: HomepageEntity): Boolean {
        return oldItem.list.toString() == newItem.list.toString()
    }
}) {

    override val layoutResId: Int = R.layout.recycler_item_record_day

    override fun convert(holder: BaseRvViewHolder<HomepageEntity>, entity: HomepageEntity) {
        super.convert(holder, entity)
        (holder as ViewHolder).run {
            mBinding.rvRecord.run {
                layoutManager = WrapContentLinearLayoutManager()
                adapter = SimpleRvListAdapter<RecordEntity>(R.layout.recycler_item_record).apply {
                    this.viewModel = this@HomepageRvAdapter.viewModel
                    submitList(entity.list)
                }
            }
        }
    }

    class ViewHolder(binding: RecyclerItemRecordDayBinding) : BaseRvDBViewHolder<RecyclerItemRecordDayBinding, HomepageEntity>(binding)

}