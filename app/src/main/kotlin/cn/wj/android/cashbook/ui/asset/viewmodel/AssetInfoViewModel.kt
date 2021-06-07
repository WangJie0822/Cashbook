package cn.wj.android.cashbook.ui.asset.viewmodel

import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore

/**
 * 资产信息 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/7
 */
class AssetInfoViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 标题文本 */
    val titleStr: MutableLiveData<String> = MutableLiveData()

    /** 返回点击 */
    val onBackClick: () -> Unit = {
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }
}