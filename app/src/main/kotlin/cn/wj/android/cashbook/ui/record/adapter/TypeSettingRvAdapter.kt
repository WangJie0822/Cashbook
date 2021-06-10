package cn.wj.android.cashbook.ui.record.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cn.wj.android.cashbook.databinding.RecyclerItemTypeSettingBinding
import cn.wj.android.cashbook.ui.type.viewmodel.ConsumptionTypeViewModel

/**
 * 类型设置适配器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/10
 */
class TypeSettingRvAdapter(private val viewModel: ConsumptionTypeViewModel) : RecyclerView.Adapter<TypeSettingRvAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RecyclerItemTypeSettingBinding.inflate(LayoutInflater.from(parent.context))
        binding.viewModel = viewModel
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    class ViewHolder(binding: RecyclerItemTypeSettingBinding) : RecyclerView.ViewHolder(binding.root)
}