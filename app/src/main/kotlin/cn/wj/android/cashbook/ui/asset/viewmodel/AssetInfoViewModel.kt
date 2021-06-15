package cn.wj.android.cashbook.ui.asset.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.paging.PagingData
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.entity.DateRecordEntity
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.interfaces.RecordListClickListener

/**
 * 资产信息 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/7
 */
class AssetInfoViewModel(private val local: LocalDataStore) : BaseViewModel(), RecordListClickListener {

    /** 显示记录详情弹窗数据 */
    val showRecordDetailsDialogData: MutableLiveData<RecordEntity> = MutableLiveData()

    /** 资产信息 */
    val assetData: MutableLiveData<AssetEntity> = MutableLiveData()

    /** 标题文本 */
    val titleStr: LiveData<String> = assetData.map {
        it.name
    }

    /** 金额标签文本 */
    val amountLabelStr: LiveData<String> = assetData.map {
        if (it.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
            R.string.current_arrears
        } else {
            R.string.asset_balance
        }.string
    }

    /** 金额文本 */
    val amountStr: LiveData<String> = assetData.map {
        if (it.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
            "-${CurrentBooksLiveData.currency.symbol}${it.balance}"
        } else {
            "${CurrentBooksLiveData.currency.symbol}${it.balance}"
        }
    }

    /** 标记 - 是否显示信用卡信息 */
    val showCreditCardInfo: LiveData<Boolean> = assetData.map {
        it.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT
    }

    /** 总额度 */
    val totalAmount: LiveData<String> = assetData.map {
        "${CurrentBooksLiveData.currency.symbol}${it.totalAmount}"
    }

    /** 账单日 */
    val billingDate: LiveData<String> = assetData.map {
        if (it.billingDate.isBlank()) {
            R.string.not_set.string
        } else {
            it.billingDate
        }
    }

    /** 还款日 */
    val repaymentDate: LiveData<String> = assetData.map {
        if (it.repaymentDate.isBlank()) {
            R.string.not_set.string
        } else {
            it.repaymentDate
        }
    }

    /** 当前资产记录列表 */
    val recordListData: LiveData<PagingData<DateRecordEntity>> by lazy {
        local.getRecordListByAssetIdPagerData(assetData.value?.id.orElse(-1L))
    }

    /** 标记 - 是否正在刷新 */
    val refreshing: MutableLiveData<Boolean> = MutableLiveData<Boolean>(true)

    /** 返回点击 */
    val onBackClick: () -> Unit = {
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 记录 item 点击 */
    override val onRecordItemClick: (RecordEntity) -> Unit = { item ->
        showRecordDetailsDialogData.value = item
    }
}