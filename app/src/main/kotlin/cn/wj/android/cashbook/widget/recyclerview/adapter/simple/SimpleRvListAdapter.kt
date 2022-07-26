@file:Suppress("unused")

package cn.wj.android.cashbook.widget.recyclerview.adapter.simple

import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import cn.wj.android.cashbook.widget.recyclerview.AreTheSame
import cn.wj.android.cashbook.widget.recyclerview.adapter.ADAPTER_ANIM_ALL
import cn.wj.android.cashbook.widget.recyclerview.adapter.base.BaseRvListDBAdapter
import cn.wj.android.cashbook.widget.recyclerview.holder.BaseRvDBViewHolder
import cn.wj.android.cashbook.widget.recyclerview.holder.BaseRvViewHolder
import java.lang.reflect.ParameterizedType

/**
 * 简易 列表适配器
 * - 使用 DataBinding
 * - 数据绑定对象名为 viewModel
 *
 * @param layoutResId 布局 id
 * @param anim 是否开启动画
 * @param areContentsTheSame 内容是否相同
 *
 * @author 王杰
 */
class SimpleRvListAdapter<E : Any>(
    override val layoutResId: Int,
    anim: Int = ADAPTER_ANIM_ALL,
    areItemsTheSame: AreTheSame<E> = { old, new -> old === new },
    areContentsTheSame: AreTheSame<E> = { old, new -> old.toString() == new.toString() }
) : BaseRvListDBAdapter<
        SimpleRvListAdapter.ViewHolder<E>,
        ViewDataBinding,
        Any,
        E>(
    anim = anim,
    diffCallback = object : DiffUtil.ItemCallback<E>() {
        override fun areItemsTheSame(oldItem: E, newItem: E): Boolean {
            return areItemsTheSame(oldItem, newItem)
        }

        override fun areContentsTheSame(oldItem: E, newItem: E): Boolean {
            return areContentsTheSame(oldItem, newItem)
        }
    }
) {

    override fun getViewHolderClass(): Class<BaseRvViewHolder<E>> {
        @Suppress("UNCHECKED_CAST")
        return ((getActualTypeList()[0] as ParameterizedType).rawType) as Class<BaseRvViewHolder<E>>
    }

    class ViewHolder<E> : BaseRvDBViewHolder<ViewDataBinding, E> {

        constructor(view: View) : super(view)

        constructor(binding: ViewDataBinding) : super(binding)
    }
}