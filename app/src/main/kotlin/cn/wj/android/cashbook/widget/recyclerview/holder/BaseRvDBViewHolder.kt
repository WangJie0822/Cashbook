@file:Suppress("JoinDeclarationAndAssignment")

package cn.wj.android.cashbook.widget.recyclerview.holder

import android.view.View
import androidx.databinding.ViewDataBinding
import cn.wj.android.cashbook.BR

/**
 * ViewHolder基类
 *
 * @param DB DataBinding 类，继承 [ViewDataBinding]，与 Adapter 一致
 * @param E 数据实体类，与 Adapter 一致
 *
 * @author 王杰
 */
open class BaseRvDBViewHolder<DB : ViewDataBinding, E> : BaseRvViewHolder<E> {

    /** DataBinding 对象 */
    lateinit var mBinding: DB

    /**
     * 构造方法，头布局、脚布局使用
     *
     * @param view 布局 View 对象
     */
    constructor(view: View) : super(view)

    /**
     * 构造方法，普通布局使用
     *
     * @param binding DataBinding 对象
     */
    constructor(binding: DB) : this(binding.root) {
        mBinding = binding
    }

    /**
     * 绑定数据
     *
     * @param entity 数据实体对象
     */
    override fun bindData(entity: E) {
        mBinding.setVariable(BR.item, entity)
        mBinding.executePendingBindings()
    }
}