package cn.wj.android.cashbook.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import cn.wj.android.cashbook.databinding.RecyclerItemAssetClassificationGroupBinding
import cn.wj.android.cashbook.widget.recyclerview.holder.BaseRvDBViewHolder

/**
 * 资产分类组适配器，只会显示一个 [resId] 对应的标题
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/2
 */
class AssetClassificationGroupRvAdapter(@StringRes private val resId: Int) : RecyclerView.Adapter<AssetClassificationGroupRvAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RecyclerItemAssetClassificationGroupBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(resId)
    }

    override fun getItemCount(): Int {
        return 1
    }

    class ViewHolder(binding: RecyclerItemAssetClassificationGroupBinding) : BaseRvDBViewHolder<RecyclerItemAssetClassificationGroupBinding, Int>(binding)

}