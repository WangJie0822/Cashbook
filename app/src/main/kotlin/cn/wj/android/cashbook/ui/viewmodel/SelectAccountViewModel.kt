package cn.wj.android.cashbook.ui.viewmodel

import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.data.transform.toSnackbarModel

/**
 * 选择账户 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/1
 */
class SelectAccountViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 底部隐藏 */
    val onBottomSheetHidden: () -> Unit = {
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 添加点击 */
    val onAddClick: () -> Unit = {
        snackbarData.value = "添加账户".toSnackbarModel()
    }
}