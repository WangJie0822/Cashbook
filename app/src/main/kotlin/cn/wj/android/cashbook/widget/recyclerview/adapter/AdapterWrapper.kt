package cn.wj.android.cashbook.widget.recyclerview.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import cn.wj.android.cashbook.widget.recyclerview.adapter.base.AbstractAdapter
import cn.wj.android.cashbook.widget.recyclerview.OnEmptyClickListener
import cn.wj.android.cashbook.widget.recyclerview.VIEW_TYPE_EMPTY
import cn.wj.android.cashbook.widget.recyclerview.VIEW_TYPE_FOOTER
import cn.wj.android.cashbook.widget.recyclerview.VIEW_TYPE_HEADER
import cn.wj.android.cashbook.widget.recyclerview.VIEW_TYPE_NORMAL
import cn.wj.android.cashbook.widget.recyclerview.like
import cn.wj.android.cashbook.widget.recyclerview.removeParent

/**
 * 适配器包装类
 *
 * - 创建时间：2020/11/16
 *
 * @author 王杰
 */
class AdapterWrapper(private val mAdapter: AbstractAdapter<*, *>) {

    /** Adapter 绑定的 RecyclerView 对象 */
    internal var mRecyclerView: RecyclerView? = null

    /** 空布局 */
    private var mEmptyLayout: FrameLayout? = null

    /** 头布局 */
    private var mHeaderLayout: LinearLayout? = null

    /** 脚布局 */
    private var mFooterLayout: LinearLayout? = null

    /** 空布局布局 id */
    private var emptyViewLayoutResID: Int? = null

    /** 标记 - 显示空布局是是否显示头布局 默认不显示 */
    var showHeaderWhenEmpty = false

    /** 标记 - 显示空布局时是否显示脚布局 默认不显示 */
    var showFooterWhenEmpty = false

    /** 空布局点击事件监听 */
    var mEmptyClickListener: OnEmptyClickListener? = null

    /**
     * 获取 [RecyclerView] 列表 item 总数
     *
     * @return item 总数
     */
    fun getItemCount(): Int {
        return if (isShowEmpty()) {
            // 显示空布局
            if (hasHeader() && showHeaderWhenEmpty) {
                // 有头布局，且显示
                if (hasFooter() && showFooterWhenEmpty) {
                    // 有脚布局，且显示
                    3
                } else {
                    // 无脚布局或不显示
                    2
                }
            } else {
                // 无头布局或不显示
                if (hasFooter() && showFooterWhenEmpty) {
                    // 有脚布局，且显示
                    2
                } else {
                    // 无脚布局或不显示
                    1
                }
            }
        } else {
            // 不显示空布局
            if (hasHeader()) {
                // 有头布局
                if (hasFooter()) {
                    // 有脚布局
                    mAdapter.getDataCount() + 2
                } else {
                    // 无脚布局
                    mAdapter.getDataCount() + 1
                }
            } else {
                // 无头布局
                if (hasFooter()) {
                    // 有脚布局
                    mAdapter.getDataCount() + 1
                } else {
                    // 无脚布局
                    mAdapter.getDataCount()
                }
            }
        }
    }

    /**
     * 根据 item 下标获取当前 item 布局类型
     *
     * @param position item 下标
     * @param customViewType 自定义布局类型方法块
     * @return View 类型 [VIEW_TYPE_HEADER]、[VIEW_TYPE_NORMAL]、[VIEW_TYPE_FOOTER]
     */
    fun getItemViewType(position: Int, customViewType: (Int) -> Int): Int {
        return when {
            isEmpty(position) -> VIEW_TYPE_EMPTY     // 空布局
            isHeader(position) -> VIEW_TYPE_HEADER   // 头布局
            isFooter(position) -> VIEW_TYPE_FOOTER   // 脚布局
            else -> customViewType(if (hasHeader()) position - 1 else position) // 其他布局
        }
    }

    /**
     * 创建 [RecyclerView.ViewHolder] 对象
     *
     * @param viewType 布局类型 [getItemViewType] 返回
     * @param createSpecialViewHolder 创建特殊布局代码块，头布局、脚布局、空布局
     * @param createCustomViewHolder 创建自定义布局代码块
     *
     * @return [RecyclerView.ViewHolder] 对象
     */
    fun <VH> onCreateViewHolder(
        viewType: Int,
        createSpecialViewHolder: (View) -> VH,
        createCustomViewHolder: () -> VH
    ): VH {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                // 头布局
                mHeaderLayout.removeParent()
                createSpecialViewHolder(mHeaderLayout!!)
            }
            VIEW_TYPE_FOOTER -> {
                // 脚布局
                mFooterLayout.removeParent()
                createSpecialViewHolder(mFooterLayout!!)
            }
            VIEW_TYPE_EMPTY -> {
                // 空布局
                mEmptyLayout.removeParent()
                createSpecialViewHolder(mEmptyLayout!!)
            }
            else -> {
                createCustomViewHolder()
            }
        }
    }

    /**
     * 绑定 [RecyclerView.ViewHolder] 数据
     *
     * @param position item 数据下标
     * @param customBindViewHolder 绑定布局代码块
     */
    fun onBindViewHolder(position: Int, customBindViewHolder: (Int) -> Unit) {
        if (needFix(position)) {
            // 头布局、脚布局、空布局，返回不做操作
            return
        }
        customBindViewHolder(if (hasHeader()) position - 1 else position)
    }

    /**
     * 绑定到 [RecyclerView]
     *
     * @param recyclerView [RecyclerView] 对象
     */
    fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        mRecyclerView = recyclerView
        if (emptyViewLayoutResID != null) {
            setEmptyView(emptyViewLayoutResID!!)
        }
        val manager = mRecyclerView!!.layoutManager
        if (manager is GridLayoutManager) {
            // 如果是 Grid 布局
            manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) =
                    if (needFix(position)) {
                        manager.spanCount
                    } else {
                        // 如果是头布局或者脚布局，独占一行
                        1
                    }
            }
        }
    }

    /**
     * 绑定到窗口
     *
     * @param holder [RecyclerView.ViewHolder] 对象
     */
    fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        val lp = holder.itemView.layoutParams
        lp?.let {
            if (it is StaggeredGridLayoutManager.LayoutParams) {
                // 是瀑布流式布局
                if (needFix(holder.layoutPosition)) {
                    // 是头布局或脚布局，独占一行
                    it.isFullSpan = true
                }
            }
        }
    }

    /**
     * 设置空布局
     *
     * @param layoutResID 布局资源 id
     * @param parent [ViewGroup] 父控件，用于加载布局
     */
    fun setEmptyView(@LayoutRes layoutResID: Int, parent: ViewGroup? = mRecyclerView) {
        emptyViewLayoutResID = if (parent != null) {
            // 已绑定 RecyclerView
            val empty = LayoutInflater.from(parent.context).inflate(layoutResID, parent, false)
            setEmptyView(empty)
            null
        } else {
            layoutResID
        }
    }

    /**
     * 设置空布局
     *
     * @param emptyView 空布局
     */
    fun setEmptyView(emptyView: View) {
        // 标记，是否第一次创建
        var insert = false
        if (mEmptyLayout == null) {
            // 未初始化，初始化空布局
            mEmptyLayout = createEmptyLayout(emptyView.context)
            // 第一次创建
            insert = true
        }
        // 添加空布局
        mEmptyLayout!!.removeAllViews()
        mEmptyLayout!!.addView(emptyView)
        if (insert) {
            // 第一次创建
            if (isShowEmpty()) {
                // 显示空布局
                var position = 0
                if (hasHeader() && showHeaderWhenEmpty) {
                    // 有头布局并且显示
                    position++
                }
                mAdapter.notifyItemInserted(position)
            }
        }
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
    fun addHeaderView(
        header: View,
        index: Int = -1,
        orientation: Int = LinearLayout.VERTICAL
    ): Int {
        if (mHeaderLayout == null) {
            mHeaderLayout = createHeaderOrFooterLayout(header.context, orientation)
        }
        var contains = false
        for (i in 0 until mHeaderLayout!!.childCount) {
            val childAt = mHeaderLayout!!.getChildAt(i)
            if (header.like(childAt)) {
                contains = true
                break
            }
        }
        if (contains) {
            return -1
        }
        var i = index
        val childCount = mHeaderLayout!!.childCount
        if (index < 0 || index > childCount) {
            i = childCount
        }
        mHeaderLayout!!.addView(header, i)
        val position = getHeaderViewPosition()
        if (position != -1) {
            mAdapter.notifyItemInserted(position)
        }
        return i
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
    fun addFooterView(
        footer: View,
        index: Int = -1,
        orientation: Int = LinearLayout.VERTICAL
    ): Int {
        if (mFooterLayout == null) {
            mFooterLayout = createHeaderOrFooterLayout(footer.context, orientation)
        }
        var contains = false
        for (i in 0 until mFooterLayout!!.childCount) {
            val childAt = mFooterLayout!!.getChildAt(i)
            if (footer.like(childAt)) {
                contains = true
                break
            }
        }
        if (contains) {
            return -1
        }
        var i = index
        val childCount = mFooterLayout!!.childCount
        if (index < 0 || index > childCount) {
            i = childCount
        }
        mFooterLayout!!.addView(footer, i)
        val position = getFooterViewPosition()
        if (position != -1) {
            mAdapter.notifyItemInserted(position)
        }
        return i
    }

    /**
     * 清除头布局
     */
    fun clearHeaderView() {
        mHeaderLayout?.removeAllViews()
    }

    /**
     * 清除脚布局
     */
    fun clearFooterView() {
        mFooterLayout?.removeAllViews()
    }

    /**
     * 判断是否显示空布局
     *
     * @return 是否显示空布局
     */
    fun isShowEmpty() =
        mEmptyLayout != null && mEmptyLayout!!.childCount > 0 && mAdapter.getDataCount() <= 0

    /**
     * 判断是否是空布局
     *
     * @param position View 位置
     *
     * @return 是否是空布局
     */
    fun isEmpty(position: Int) = if (isShowEmpty()) {
        // 显示空布局
        if (hasHeader() && showHeaderWhenEmpty) {
            // 有头布局且显示
            position == 1
        } else {
            position == 0
        }
    } else {
        // 不显示头布局
        false
    }

    private fun createEmptyLayout(context: Context): FrameLayout {
        return FrameLayout(context).apply {
            setOnClickListener {
                mEmptyClickListener?.invoke()
            }
            // 设置布局参数
            val tempLayoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.MATCH_PARENT
            )
            layoutParams = tempLayoutParams
        }
    }

    /**
     * 创建头布局、脚布局对象
     *
     * @param context [Context] 对象
     * @param orientation 排列方式 [LinearLayout.VERTICAL]、[LinearLayout.HORIZONTAL] 默认竖排
     *
     * @return 头布局对象
     */
    private fun createHeaderOrFooterLayout(
        context: Context,
        orientation: Int = LinearLayout.VERTICAL
    ): LinearLayout {
        return LinearLayout(context).apply {
            if (orientation == LinearLayout.VERTICAL) {
                this.orientation = LinearLayout.VERTICAL
                this.layoutParams =
                    RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            } else {
                this.orientation = LinearLayout.HORIZONTAL
                this.layoutParams =
                    RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
            }
        }
    }

    /**
     * 判断是否有头布局
     *
     * @return 是否有头布局
     */
    fun hasHeader() = mHeaderLayout != null && mHeaderLayout!!.childCount > 0

    /**
     * 根据下标判断是否是头布局
     *
     * @param position View 下标
     * @return 是否是头布局
     */
    fun isHeader(position: Int) = if (isShowEmpty()) {
        // 显示空布局
        if (hasHeader() && showHeaderWhenEmpty) {
            // 有头布局且显示
            position == 0
        } else {
            // 不显示头布局
            false
        }
    } else {
        // 不显示空布局
        hasHeader() && position == 0
    }

    /**
     * 获取头布局位置
     *
     * @return 头布局位置
     */
    fun getHeaderViewPosition() = if (hasHeader()) 0 else -1

    /**
     * 判断是否有脚布局
     *
     * @return 是否有脚布局
     */
    fun hasFooter() = mFooterLayout != null && mFooterLayout!!.childCount > 0

    /**
     * 根据下标判断是否是脚布局
     *
     * @param position View 下标
     * @return 是否是脚布局
     */
    fun isFooter(position: Int) = if (!hasFooter()) {
        // 没有脚布局或不显示
        false
    } else {
        // 有脚布局且显示
        if (isShowEmpty()) {
            // 显示空布局
            if (showFooterWhenEmpty) {
                // 显示脚布局
                position == getItemCount() - 1
            } else {
                false
            }
        } else {
            position == getItemCount() - 1
        }
    }

    /**
     * 获取脚布局位置
     *
     * @return 脚布局位置
     */
    fun getFooterViewPosition() = if (!hasFooter() || (isShowEmpty() && !showFooterWhenEmpty)) {
        // 不显示脚布局
        -1
    } else {
        // 显示脚布局
        getItemCount() - 1
    }

    /**
     * 通知数据更新
     */
    fun notifyDataSetChanged() {
        if (null == mRecyclerView) {
            return
        }
        mAdapter.notifyDataSetChanged()
    }

    /**
     * 判断是否需要配置独占一行
     *
     * @param position View 位置
     *
     * @return 是否需要配置独占一行
     */
    private fun needFix(position: Int) =
        mAdapter.getItemViewType(position) in arrayOf(
            VIEW_TYPE_HEADER,
            VIEW_TYPE_FOOTER,
            VIEW_TYPE_EMPTY
        )

}