package cn.wj.android.cashbook.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import cn.wj.android.cashbook.databinding.RecyclerItemBillBinding
import cn.wj.android.cashbook.widget.recyclerview.defaultDiffCallback
import cn.wj.android.cashbook.widget.recyclerview.holder.BaseRvDBViewHolder

class BillListRvAdapter :
    PagingDataAdapter<String, BillListRvAdapter.ViewHolder>(defaultDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            RecyclerItemBillBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(getItem(position).orEmpty())
    }

    class ViewHolder(binding: RecyclerItemBillBinding) :
        BaseRvDBViewHolder<RecyclerItemBillBinding, String>(binding)
}