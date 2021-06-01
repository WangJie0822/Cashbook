package cn.wj.android.cashbook.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore

/**
 * 选择资产账户分类 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/1
 */
class SelectAssetClassificationViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 底部隐藏 */
    val onBottomSheetHidden: () -> Unit = {
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }
}