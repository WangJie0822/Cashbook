package cn.wj.android.cashbook.base.ui

import androidx.lifecycle.ViewModel
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.ProgressModel
import cn.wj.android.cashbook.data.model.SnackbarModel
import cn.wj.android.cashbook.data.model.UiNavigationModel

/**
 * [ViewModel] 基类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 20201/3/8
 */
abstract class BaseViewModel : ViewModel() {

    /** 进度弹窗显示事件 */
    val progressEvent: LifecycleEvent<ProgressModel> = LifecycleEvent()

    /** Snackbar 显示事件 */
    val snackbarEvent: LifecycleEvent<SnackbarModel> = LifecycleEvent()

    /** 界面跳转事件 */
    val uiNavigationEvent: LifecycleEvent<UiNavigationModel> = LifecycleEvent()
}