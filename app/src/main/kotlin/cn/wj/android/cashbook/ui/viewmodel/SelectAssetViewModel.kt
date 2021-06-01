package cn.wj.android.cashbook.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore

/**
 * 选择资产账户 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/1
 */
class SelectAssetViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 显示选择资产类别弹窗 */
    val showSelectAssetTypeData: MutableLiveData<Int> = MutableLiveData()

    /** 底部隐藏 */
    val onBottomSheetHidden: () -> Unit = {
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 添加点击 */
    val onAddClick: () -> Unit = {
        showSelectAssetTypeData.value = 0
    }
}