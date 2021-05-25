package cn.wj.android.cashbook.widget.recyclerview.adapter.base

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import cn.wj.android.cashbook.widget.recyclerview.OnEmptyClickListener
import cn.wj.android.cashbook.widget.recyclerview.VIEW_TYPE_NORMAL
import cn.wj.android.cashbook.widget.recyclerview.adapter.AdapterWrapper
import cn.wj.android.cashbook.widget.recyclerview.holder.BaseRvViewHolder
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * 适配器抽象类
 *
 * - 创建时间：2020/11/16
 *
 * @author 王杰
 */
abstract class AbstractAdapter<VH : BaseRvViewHolder<E>, E> : RecyclerView.Adapter<VH>() {

    /** 包装类对象 */
    @Suppress("LeakingThis")
    protected val mWrapper = AdapterWrapper(this)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        mWrapper.onAttachedToRecyclerView(recyclerView)
    }

    override fun onViewAttachedToWindow(holder: VH) {
        mWrapper.onViewAttachedToWindow(holder)
    }

    override fun getItemCount(): Int {
        return mWrapper.getItemCount()
    }

    override fun getItemViewType(position: Int): Int {
        return mWrapper.getItemViewType(position) { fixedPosition ->
            getCustomItemViewType(fixedPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return mWrapper.onCreateViewHolder(viewType, { view ->
            createSpecialViewHolder(view)
        }, {
            createCustomViewHolder(parent, viewType)
        })
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        mWrapper.onBindViewHolder(position) { fixedPosition ->
            bindCustomViewHolder(holder, fixedPosition)
        }
    }

    /**
     * 获取自定义布局类型
     *
     * @param fixedPosition 不计算头布局、脚布局的下标
     *
     * @return 布局类型，默认 [VIEW_TYPE_NORMAL]
     */
    open fun getCustomItemViewType(fixedPosition: Int): Int {
        return VIEW_TYPE_NORMAL
    }

    /**
     * 创建特殊 [RecyclerView.ViewHolder] 对象
     *
     * @return [RecyclerView.ViewHolder] 对象
     */
    open fun createSpecialViewHolder(view: View): VH {
        @Suppress("UNCHECKED_CAST")
        val clazz = getViewHolderClass()
        val constructor = clazz.getConstructor(View::class.java)
        return constructor.newInstance(view)
    }

    /**
     * 绑定自定义 [RecyclerView.ViewHolder]
     *
     * @param holder [RecyclerView.ViewHolder] 对象
     * @param fixedPosition 不计算头布局、脚布局的下标
     */
    open fun bindCustomViewHolder(holder: VH, fixedPosition: Int) {
        // 普通布局，绑定数据
        if (fixedPosition < 0 || fixedPosition >= getDataCount()) {
            // 数组越界
            return
        }
        // 绑定布局
        convert(holder, getItem(fixedPosition))
    }

    /**
     * 绑定数据
     *
     * @param holder [VH] 对象
     * @param entity   数据对象
     */
    open fun convert(holder: VH, entity: E) {
        holder.bindData(entity)
    }

    /**
     * 设置空布局
     *
     * @param layoutResID 布局资源 id
     * @param parent [ViewGroup] 父控件，用于加载布局 **若使用默认 mRecyclerView 务必在 [RecyclerView]#setAdapter() 之后调用**
     */
    fun setEmptyView(@LayoutRes layoutResID: Int, parent: ViewGroup? = mWrapper.mRecyclerView) {
        mWrapper.setEmptyView(layoutResID, parent)
    }

    /**
     * 设置空布局
     *
     * @param emptyView 空布局
     */
    fun setEmptyView(emptyView: View) {
        mWrapper.setEmptyView(emptyView)
    }

    /**
     * 添加头布局
     *
     * @param header 头布局
     * @param index 添加位置 默认从上往下
     * @param orientation 排列方式 [LinearLayout.VERTICAL]、[LinearLayout.HORIZONTAL] 默认竖排
     *
     * @return 插入的实际位置
     */
    fun addHeaderView(header: View, index: Int = -1, orientation: Int = LinearLayout.VERTICAL): Int {
        return mWrapper.addHeaderView(header, index, orientation)
    }

    /**
     * 添加头布局
     *
     * @param footer 脚布局
     * @param index 添加位置 默认从上往下
     * @param orientation 排列方式 [LinearLayout.VERTICAL]、[LinearLayout.HORIZONTAL] 默认竖排
     *
     * @return 插入的实际位置
     */
    fun addFooterView(footer: View, index: Int = -1, orientation: Int = LinearLayout.VERTICAL): Int {
        return mWrapper.addFooterView(footer, index, orientation)
    }

    /**
     * 清除头布局
     */
    fun clearHeaderView() {
        mWrapper.clearHeaderView()
    }

    /**
     * 清除脚布局
     */
    fun clearFooterView() {
        mWrapper.clearFooterView()
    }

    /**
     * 判断是否显示空布局
     *
     * @return 是否显示空布局
     */
    fun isShowEmpty(): Boolean {
        return mWrapper.isShowEmpty()
    }

    /**
     * 判断是否是空布局
     *
     * @param position View 位置
     *
     * @return 是否是空布局
     */
    fun isEmpty(position: Int): Boolean {
        return mWrapper.isEmpty(position)
    }

    /**
     * 判断是否有头布局
     *
     * @return 是否有头布局
     */
    fun hasHeader(): Boolean {
        return mWrapper.hasHeader()
    }

    /**
     * 根据下标判断是否是头布局
     *
     * @param position View 下标
     *
     * @return 是否是头布局
     */
    fun isHeader(position: Int): Boolean {
        return mWrapper.isHeader(position)
    }

    /**
     * 获取头布局位置
     *
     * @return 头布局位置
     */
    fun getHeaderViewPosition(): Int {
        return mWrapper.getHeaderViewPosition()
    }

    /**
     * 判断是否有脚布局
     *
     * @return 是否有脚布局
     */
    fun hasFooter(): Boolean {
        return mWrapper.hasFooter()
    }

    /**
     * 根据下标判断是否是脚布局
     *
     * @param position View 下标
     * @return 是否是脚布局
     */
    fun isFooter(position: Int): Boolean {
        return mWrapper.isFooter(position)
    }

    /**
     * 获取脚布局位置
     *
     * @return 脚布局位置
     */
    fun getFooterViewPosition(): Int {
        return mWrapper.getFooterViewPosition()
    }

    /**
     * 设置空布局点击事件
     *
     * @param lis 空布局点击事件监听
     */
    fun setOnEmptyClickListener(lis: OnEmptyClickListener) {
        mWrapper.mEmptyClickListener = lis
    }

    /**
     * 空布局是否显示头布局
     *
     * @param showHeaderWhenEmpty 是否显示头布局
     */
    fun showHeaderWhenEmpty(showHeaderWhenEmpty: Boolean) {
        mWrapper.showHeaderWhenEmpty = showHeaderWhenEmpty
    }

    /**
     * 空布局是否显示脚布局
     *
     * @param showFooterWhenEmpty 是否显示脚布局
     */
    fun showFooterWhenEmpty(showFooterWhenEmpty: Boolean) {
        mWrapper.showFooterWhenEmpty = showFooterWhenEmpty
    }

    /**
     * 获取泛型实际类型列表
     */
    protected fun getActualTypeList(): Array<Type> = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments

    /**
     * 获取数据总数
     *
     * @return 数据总数
     */
    abstract fun getDataCount(): Int

    /**
     * 根据下标获取数据
     *
     * @param position 数据下标
     *
     * @return 数据对象
     */
    abstract fun getItem(position: Int): E

    /**
     * 创建自定义 [VH] 对象
     *
     * @param parent 父布局
     * @param viewType 布局类型，[getItemViewType] 返回
     *
     * @return [VH] 对象
     */
    abstract fun createCustomViewHolder(parent: ViewGroup, viewType: Int): VH

    /**
     * 获取 [VH] 类对象
     */
    abstract fun getViewHolderClass(): Class<VH>
}