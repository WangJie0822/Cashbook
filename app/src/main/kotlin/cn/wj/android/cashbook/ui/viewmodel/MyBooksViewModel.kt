package cn.wj.android.cashbook.ui.viewmodel

import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore

/**
 * 我的账本 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于2021/5/25
 */
class MyBooksViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 返回按钮点击 */
    val onBackClick:()->Unit={
        // 退出当前界面
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }
}