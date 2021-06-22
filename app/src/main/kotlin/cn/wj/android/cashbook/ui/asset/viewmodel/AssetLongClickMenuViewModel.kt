package cn.wj.android.cashbook.ui.asset.viewmodel

import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.event.LifecycleEvent

/**
 * 资产长按菜单 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/7
 */
class AssetLongClickMenuViewModel : BaseViewModel() {

    /** 编辑点击事件 */
    val editClickEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 隐藏点击事件 */
    val hiddenClickEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 编辑点击 */
    val onEditClick: () -> Unit = {
        editClickEvent.value = 0
    }

    /** 隐藏点击 */
    val onHiddenClick: () -> Unit = {
        hiddenClickEvent.value = 0
    }
}