package cn.wj.android.cashbook.base.ui

import androidx.lifecycle.ViewModel
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.SnackbarModel
import cn.wj.android.cashbook.data.model.UiNavigationModel

/**
 * [ViewModel] 基类
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 20201/3/8
 */
abstract class BaseViewModel : ViewModel() {

    /** Snackbar 显示事件 */
    val snackbarEvent = LifecycleEvent<SnackbarModel>()

    /** 界面跳转事件 */
    val uiNavigationEvent = LifecycleEvent<UiNavigationModel>()
}