@file:Suppress("unused")

package cn.wj.android.cashbook.widget.recyclerview.adapter.simple

import android.view.View
import androidx.databinding.ViewDataBinding
import cn.wj.android.cashbook.widget.recyclerview.adapter.base.BaseRvDBAdapter
import cn.wj.android.cashbook.widget.recyclerview.holder.BaseRvDBViewHolder
import cn.wj.android.cashbook.widget.recyclerview.holder.BaseRvViewHolder
import java.lang.reflect.ParameterizedType

/**
 * 简易 列表适配器
 * - 使用 DataBinding
 * - 数据绑定对象名为 viewModel
 *
 * @author 王杰
 */
class SimpleRvAdapter<E>(override val layoutResID: Int) : BaseRvDBAdapter<
        SimpleRvAdapter.ViewHolder<E>,
        ViewDataBinding,
        Any,
        E>() {

    override fun getViewHolderClass(): Class<BaseRvViewHolder<E>> {
        @Suppress("UNCHECKED_CAST")
        return ((getActualTypeList()[0] as ParameterizedType).rawType) as Class<BaseRvViewHolder<E>>
    }

    class ViewHolder<E> : BaseRvDBViewHolder<ViewDataBinding, E> {

        constructor(view: View) : super(view)

        constructor(binding: ViewDataBinding) : super(binding)
    }
}