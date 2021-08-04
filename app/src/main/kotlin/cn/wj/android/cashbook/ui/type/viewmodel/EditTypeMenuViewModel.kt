package cn.wj.android.cashbook.ui.type.viewmodel

import androidx.lifecycle.MutableLiveData
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

    /** 改为二级分类点击事件 */
    val changeToSecondTypeEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 改为一级分类点击事件 */
    val changeToFirstTypeEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 移动到其它一级分类点击事件 */
    val moveToOtherFirstTypeEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 统计点击事件 */
    val statisticsClickEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 标记 - 是否是一级分类 */
    val firstType: MutableLiveData<Boolean> = MutableLiveData(true)

    /** 编辑点击 */
    val onEditClick: () -> Unit = {
        editClickEvent.value = 0
    }

    /** 删除点击 */
    val onDeleteClick: () -> Unit = {
        deleteClickEvent.value = 0
    }

    /** 修改为二级分类点击 */
    val onChangeToSecondTypeClick: () -> Unit = {
        changeToSecondTypeEvent.value = 0
    }

    /** 修改为一级级分类点击 */
    val onChangeToFirstTypeClick: () -> Unit = {
        changeToFirstTypeEvent.value = 0
    }

    /** 移动到其它一级分类点击 */
    val onMoveToOtherFirstTypeClick: () -> Unit = {
        moveToOtherFirstTypeEvent.value = 0
    }

    /** 统计点击 */
    val onStatisticsClick: () -> Unit = {
        statisticsClickEvent.value = 0
    }
}