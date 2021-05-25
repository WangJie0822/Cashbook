@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState

/**
 * SmartRefreshLayout DataBinding 适配器
 */

/** 扩展属性 是否正在刷新 */
@set:BindingAdapter("android:bind_smart_refreshing")
@get:InverseBindingAdapter(
    attribute = "android:bind_smart_refreshing",
    event = "android:bind_smart_refreshingAttrChanged"
)
var SmartRefreshLayout.refreshing: Boolean
    get() = state == RefreshState.Refreshing
    set(value) {
        if (refreshing == value) {
            return
        }
        if (value) {
            autoRefresh()
        } else {
            finishRefresh()
        }
    }

/** 扩展属性 是否正在加载更多 */
@set:BindingAdapter("android:bind_smart_loadMore")
@get:InverseBindingAdapter(
    attribute = "android:bind_smart_loadMore",
    event = "android:bind_smart_loadMoreAttrChanged"
)
var SmartRefreshLayout.loadMore: Boolean
    get() = state == RefreshState.Loading
    set(value) {
        if (loadMore && value) {
            return
        }
        if (!loadMore && !value) {
            return
        }
        if (value) {
            autoLoadMore()
        } else {
            finishLoadMore()
        }
    }

/** 扩展属性 刷新状态，包含是否正在刷新、是否成功、结束延时、是否没有更多 */
@set:BindingAdapter("android:bind_smart_refreshState")
@get:InverseBindingAdapter(
    attribute = "android:bind_smart_refreshState",
    event = "android:bind_smart_refreshingAttrChanged"
)
var SmartRefreshLayout.refreshState: SmartRefreshState?
    get() = SmartRefreshState(refreshing)
    set(value) {
        if (null == value) {
            return
        }
        if (refreshing == value.loading) {
            return
        }
        if (value.loading) {
            // 下拉刷新
            if (value.delay == 0) {
                autoRefresh()
            } else {
                autoRefresh(value.delay)
            }
        } else {
            // 刷新结束
            if (value.delay == 0 && !value.noMore) {
                finishRefresh(value.success)
            } else if (value.delay > 0 && value.success && !value.noMore) {
                finishRefresh(value.delay)
            } else {
                finishRefresh(value.delay, value.success, value.noMore)
            }
        }
    }

/** 扩展属性 加载更多状态，包含是否正在加载、是否成功、结束延时、是否没有更多 */
@set:BindingAdapter("android:bind_smart_loadMoreState")
@get:InverseBindingAdapter(
    attribute = "android:bind_smart_loadMoreState",
    event = "android:bind_smart_loadMoreAttrChanged"
)
var SmartRefreshLayout.loadMoreState: SmartRefreshState?
    get() = SmartRefreshState(loadMore)
    set(value) {
        if (null == value) {
            return
        }
        if (loadMore == value.loading) {
            return
        }
        if (value.loading) {
            // 加载更多
            if (value.delay == 0) {
                autoLoadMore()
            } else {
                autoLoadMore(value.delay)
            }
        } else {
            // 加载结束
            if (value.delay == 0 && !value.noMore) {
                finishLoadMore(value.success)
            } else if (value.delay > 0 && value.success && !value.noMore) {
                finishLoadMore(value.delay)
            } else {
                finishLoadMore(value.delay, value.success, value.noMore)
            }
        }
    }

/**
 * 给 [SmartRefreshLayout] 设置刷新回调 [onRefresh]
 * > [onRefresh] 无入参，无返回值
 *
 * > [listener] 为属性变化监听，`DataBinding` 自动实现，无需配置
 */
@BindingAdapter(
    "android:bind_smart_onRefresh",
    "android:bind_smart_refreshingAttrChanged",
    requireAll = false
)
fun SmartRefreshLayout.setSmartRefreshLayoutOnRefresh(
    onRefresh: (() -> Unit)?,
    listener: InverseBindingListener?
) {
   setOnRefreshListener {
        listener?.onChange()
        onRefresh?.invoke()
    }
}

/**
 * 给 [SmartRefreshLayout] 设置刷新回调 [onRefresh]
 * > [onRefresh] 参见 [SmartLoadDataListener]
 *
 * > [listener] 为属性变化监听，`DataBinding` 自动实现，无需配置
 */
@BindingAdapter(
    "android:bind_smart_onRefresh",
    "android:bind_smart_refreshingAttrChanged",
    requireAll = false
)
fun SmartRefreshLayout.setSmartRefreshLayoutOnRefresh(
    onRefresh: SmartLoadDataListener?,
    listener: InverseBindingListener?
) {
    setOnRefreshListener {
        listener?.onChange()
        onRefresh?.onLoadData()
    }
}

/**
 * 给 [SmartRefreshLayout] 设置是否允许下拉刷新 [enable]
 */
@BindingAdapter("android:bind_smart_refresh_enable")
fun SmartRefreshLayout. setSmartRefreshLayoutRefreshEnable( enable: Boolean) {
    setEnableRefresh(enable)
}

/**
 * 给 [SmartRefreshLayout] 设置加载更多回调 [onLoadMore]
 * > [onLoadMore] 无入参，无返回值
 *
 * > [listener] 为属性变化监听，`DataBinding` 自动实现，无需配置
 */
@BindingAdapter(
    "android:bind_smart_onLoadMore",
    "android:bind_smart_loadMoreAttrChanged",
    requireAll = false
)
fun SmartRefreshLayout.setSmartRefreshLayoutOnLoadMore(
    onLoadMore: (() -> Unit)?,
    listener: InverseBindingListener?
) {
    setOnLoadMoreListener {
        listener?.onChange()
        onLoadMore?.invoke()
    }
}

/**
 * 给 [SmartRefreshLayout] 设置加载更多回调 [onLoadMore]
 * > [onLoadMore] 参见 [SmartLoadDataListener]
 *
 * > [listener] 为属性变化监听，`DataBinding` 自动实现，无需配置
 */
@BindingAdapter(
    "android:bind_smart_onLoadMore",
    "android:bind_smart_loadMoreAttrChanged",
    requireAll = false
)
fun SmartRefreshLayout.setSmartRefreshLayoutOnLoadMore(
    onLoadMore: SmartLoadDataListener?,
    listener: InverseBindingListener?
) {
   setOnLoadMoreListener {
        listener?.onChange()
        onLoadMore?.onLoadData()
    }
}

/**
 * 给 [SmartRefreshLayout] 设置是否允许上拉加载更多 [enable]
 */
@BindingAdapter("android:bind_smart_loadMore_enable")
fun SmartRefreshLayout.setSmartRefreshLayoutLoadMoreEnable( enable: Boolean) {
    setEnableLoadMore(enable)
}

/**
 * 给 [SmartRefreshLayout] 设置是否显示没有更多 [noMore]
 */
@BindingAdapter("android:bind_smart_noMore")
fun SmartRefreshLayout.setSmartRefreshLayoutNoMore( noMore: Boolean) {
   setNoMoreData(noMore)
}

/**
 * [SmartRefreshLayout] 数据加载回调接口
 */
interface SmartLoadDataListener {

    /**
     * 刷新、加载更多触发时回调
     */
    fun onLoadData()
}

/**
 * 控件加载状态
 * > 包含 [loading] 是否正在加载，[success] 是否加载成功-默认`true`，
 * > [noMore] 是否没有更多数据-默认`false`，[delay] 延时时间-默认`0`
 */
data class SmartRefreshState(
    val loading: Boolean,
    val success: Boolean = true,
    val noMore: Boolean = false,
    val delay: Int = 0
)