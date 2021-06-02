package cn.wj.android.cashbook.ui.asset.viewmodel

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore

/**
 * 编辑资产 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/2
 */
class EditAssetViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 资产分类大类 */
    val classificationType: MutableLiveData<ClassificationTypeEnum> = MutableLiveData(ClassificationTypeEnum.CAPITAL_ACCOUNT)

    /** 资产分类 */
    val assetClassification: MutableLiveData<AssetClassificationEnum> = MutableLiveData(AssetClassificationEnum.CASH)

    /** 显示选择资产类型弹窗 */
    val showSelectAssetClassificationData: MutableLiveData<Int> = MutableLiveData()

    /** 标题文本 */
    val titleStr: ObservableField<String> = ObservableField()

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 资产类型点击 */
    val onAssetClassificationClick: () -> Unit = {
        showSelectAssetClassificationData.value = 0
    }
}