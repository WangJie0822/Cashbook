package cn.wj.android.cashbook.ui.type.viewmodel

import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.event.LifecycleEvent

/**
 * 编辑类型菜单 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/30
 */
class EditTypeMenuViewModel : BaseViewModel() {

    /** 编辑点击事件 */
    val editClickEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 删除点击事件 */
    val deleteClickEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 统计点击事件 */
    val statisticsClickEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 编辑点击 */
    val onEditClick: () -> Unit = {
        editClickEvent.value = 0
    }

    /** 删除点击 */
    val onDeleteClick: () -> Unit = {
        deleteClickEvent.value = 0
    }

    /** 统计点击 */
    val onStatisticsClick: () -> Unit = {
        statisticsClickEvent.value = 0
    }
}