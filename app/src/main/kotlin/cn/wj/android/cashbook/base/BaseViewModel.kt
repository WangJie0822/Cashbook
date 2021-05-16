package cn.wj.android.cashbook.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.wj.android.cashbook.model.SnackbarModel
import cn.wj.android.cashbook.model.UiNavigationModel

/**
 * [ViewModel] 基类
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 20201/3/8
 */
abstract class BaseViewModel : ViewModel() {

    /** Snackbar 控制 */
    val snackbarData = MutableLiveData<SnackbarModel>()

    /** 界面跳转控制 */
    val uiNavigationData = MutableLiveData<UiNavigationModel>()
}