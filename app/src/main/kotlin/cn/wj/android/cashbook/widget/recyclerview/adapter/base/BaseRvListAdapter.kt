@file:Suppress("unused", "LeakingThis")

package cn.wj.android.cashbook.widget.recyclerview.adapter.base

import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import cn.wj.android.cashbook.widget.recyclerview.adapter.ADAPTER_ANIM_ALL
import cn.wj.android.cashbook.widget.recyclerview.adapter.callback.AdapterListUpdateCallback
import cn.wj.android.cashbook.widget.recyclerview.defaultDiffCallback
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
abstract class BaseRvListAdapter<VH : BaseRvViewHolder<E>, E : Any>
    : AbstractAdapter<BaseRvViewHolder<E>, E> {

    /** 数据集合 */
    val mDiffer: AsyncListDiffer<E>

    private val mListener = AsyncListDiffer.ListListener<E> { previousList, currentList ->
        this.onCurrentListChanged(previousList, currentList)
    }

    /**
     * 构造方法
     *
     * @param anim 是否使用动画
     * @param config Differ config
     */
    constructor(anim: Int, config: AsyncDifferConfig<E>) {
        mDiffer = AsyncListDiffer(AdapterListUpdateCallback(this, anim), config)
        mDiffer.addListListener(mListener)
    }

    /**
     * 构造方法
     *
     * @param anim 是否使用动画
     * @param diffCallback 数据是否相同回调
     */
    constructor(anim: Int = ADAPTER_ANIM_ALL, diffCallback: DiffUtil.ItemCallback<E> = defaultDiffCallback())
            : this(anim, AsyncDifferConfig.Builder(diffCallback).build())

    override fun getDataCount(): Int {
        return mDiffer.currentList.size
    }

    override fun getItem(position: Int): E {
        return mDiffer.currentList[position]
    }

    override fun getViewHolderClass(): Class<BaseRvViewHolder<E>> {
        @Suppress("UNCHECKED_CAST")
        return getActualTypeList()[0] as Class<BaseRvViewHolder<E>>
    }

    /**
     * 列表变化回调
     */
    open fun onCurrentListChanged(previousList: List<E>, currentList: List<E>) {}

    open fun submitList(list: List<E>?) {
        mDiffer.submitList(list)
    }

    open fun submitList(list: List<E>?, commitCallback: Runnable?) {
        mDiffer.submitList(list, commitCallback)
    }
}