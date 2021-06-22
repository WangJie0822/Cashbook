package cn.wj.android.cashbook.ui.asset.viewmodel

import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.event.LifecycleEvent

/**
 * 隐藏资产长按菜单 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/7
 */
class InvisibleAssetLongClickMenuViewModel : BaseViewModel() {

    /** 隐藏点击事件 */
    val cancelHiddenClickEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 隐藏点击 */
    val onCancelHiddenClick: () -> Unit = {
        cancelHiddenClickEvent.value = 0
    }
}