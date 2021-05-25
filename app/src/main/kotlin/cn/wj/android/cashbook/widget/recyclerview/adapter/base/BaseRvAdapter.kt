@file:Suppress("unused")

package cn.wj.android.cashbook.widget.recyclerview.adapter.base

import cn.wj.android.cashbook.widget.recyclerview.holder.BaseRvViewHolder

/**
 * RecyclerView 适配器基类
 * - 若要使 头布局、脚布局 等必须在设置适配器之前设置布局管理器
 *
 * @param VH ViewHolder 类型，继承 [BaseRvViewHolder]
 * @param E  数据类型
 *
 * @author 王杰
 */
abstract class BaseRvAdapter<out VH : BaseRvViewHolder<E>, E>
    : AbstractAdapter<BaseRvViewHolder<E>, E>() {

    /** 数据集合  */
    val mData = arrayListOf<E>()

    override fun getDataCount(): Int {
        return mData.size
    }

    override fun bindCustomViewHolder(holder: BaseRvViewHolder<E>, fixedPosition: Int) {
        // 普通布局，绑定数据
        if (fixedPosition >= mData.size) {
            return
        }
        convert(holder, getItem(fixedPosition))
    }

    override fun getItem(position: Int): E {
        return mData[position]
    }

    override fun getViewHolderClass(): Class<BaseRvViewHolder<E>> {
        @Suppress("UNCHECKED_CAST")
        return getActualTypeList()[0] as Class<BaseRvViewHolder<E>>
    }

    /**
     * 刷新列表
     *
     * @param ls 新数据
     */
    fun refresh(ls: Collection<E>?) {
        if (ls.isNullOrEmpty()) {
            return
        }
        mData.clear()
        mData.addAll(ls)
        mWrapper.notifyDataSetChanged()
    }

    /**
     * 清空数据
     */
    fun clear() {
        mData.clear()
        mWrapper.notifyDataSetChanged()
    }

    /**
     * 加载数据
     *
     * @param ls 数据列表
     */
    fun loadMore(ls: Collection<E>?) {
        if (ls.isNullOrEmpty()) {
            return
        }
        mData.addAll(ls)
        mWrapper.notifyDataSetChanged()
    }
}