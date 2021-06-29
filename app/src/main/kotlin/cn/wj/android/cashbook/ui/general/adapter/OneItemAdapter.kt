package cn.wj.android.cashbook.ui.general.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cn.wj.android.cashbook.base.ext.setOnThrottleClickListener

/**
 * 只有一个 Item 的适配器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于2021/6/29
 */
class OneItemAdapter(private val layoutResId: Int, private val onClick: (() -> Unit)? = null) : RecyclerView.Adapter<OneItemAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(layoutResId, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (null != onClick) {
            holder.itemView.setOnThrottleClickListener({
                onClick.invoke()
            })
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}