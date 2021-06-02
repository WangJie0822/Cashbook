package cn.wj.android.cashbook.ui.general.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cn.wj.android.cashbook.R

/**
 * 底部空白适配器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于2021/5/26
 */
class BlankFooterAdapter : RecyclerView.Adapter<BlankFooterAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recycler_item_blank, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    override fun getItemCount(): Int {
        return 1
    }
}