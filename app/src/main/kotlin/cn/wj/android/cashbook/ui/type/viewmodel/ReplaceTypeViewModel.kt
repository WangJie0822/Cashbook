package cn.wj.android.cashbook.ui.type.viewmodel

import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore

/**
 * 分类替换 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/2
 */
class ReplaceTypeViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 返回点击 */
    val onBackClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }
}