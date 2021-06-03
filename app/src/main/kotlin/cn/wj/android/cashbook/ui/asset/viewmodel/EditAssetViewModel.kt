package cn.wj.android.cashbook.ui.asset.viewmodel

import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.data.transform.toSnackbarModel

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

    /** 标记 - 是否是信用卡 */
    val creditCardType: LiveData<Boolean> = classificationType.map {
        it == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT
    }

    /** 余额提示文本 */
    val balanceHint: LiveData<String> = creditCardType.map {
        if (it) {
            // 信用卡类型，提示欠款
            R.string.current_arrears
        } else {
            // 其他类型，提示余额
            R.string.asset_balance
        }.string
    }

    /** 资产名称 */
    val assetName: MutableLiveData<String> = MutableLiveData()

    /** 信用卡总额度 */
    val totalAmount: MutableLiveData<String> = MutableLiveData()

    /** 余额、信用卡欠款 */
    val balance: MutableLiveData<String> = MutableLiveData()

    /** 账单日 */
    val billingDate: MutableLiveData<String> = MutableLiveData()

    /** 还款日 */
    val repaymentDate: MutableLiveData<String> = MutableLiveData()

    /** 标记 - 是否隐藏资产 */
    val invisibleAsset: MutableLiveData<Boolean> = MutableLiveData()

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

    /** 账单日点击 */
    val onBillingDateClick: () -> Unit = {
        // TODO
        snackbarData.value = "账单日".toSnackbarModel()
    }

    /** 还款日点击 */
    val onRepaymentDateClick: () -> Unit = {
        // TODO
        snackbarData.value = "还款日".toSnackbarModel()
    }

    /** 保存点击 */
    val onSaveClick: () -> Unit = {
        // TODO
        snackbarData.value = "保存".toSnackbarModel()
    }
}