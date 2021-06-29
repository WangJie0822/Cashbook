package cn.wj.android.cashbook.ui.record.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.databinding.RecyclerItemTypeSecondBinding
import cn.wj.android.cashbook.ui.type.viewmodel.ConsumptionTypeViewModel
import cn.wj.android.cashbook.widget.recyclerview.adapter.simple.SimpleRvListAdapter

/**
 * 二级类型适配器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/10
 */
class TypeSecondRvAdapter(private val viewModel: ConsumptionTypeViewModel) : RecyclerView.Adapter<TypeSecondRvAdapter.ViewHolder>() {

    val list = arrayListOf<TypeEntity>()

    var itemPosition = 0

    var visiblePosition = 0

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RecyclerItemTypeSecondBinding.inflate(LayoutInflater.from(parent.context))
        binding.viewModel = viewModel
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.visiblePosition = visiblePosition
        holder.binding.rvType.run {
            layoutManager = GridLayoutManager(context, 5)
            adapter = SimpleRvListAdapter<TypeEntity>(R.layout.recycler_item_type).apply {
                this.viewModel = this@TypeSecondRvAdapter.viewModel
                submitList(list[position].childList)
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        val manager = recyclerView.layoutManager
        if (manager is GridLayoutManager) {
            // 独占一行
            manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int = if (itemPosition == position) {
                    5
                } else {
                    1
                }
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        val manager = recyclerView.layoutManager
        if (manager is GridLayoutManager) {
            // 独占一行
            manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int = 1
            }
        }
    }

    class ViewHolder(val binding: RecyclerItemTypeSecondBinding) : RecyclerView.ViewHolder(binding.root)
}