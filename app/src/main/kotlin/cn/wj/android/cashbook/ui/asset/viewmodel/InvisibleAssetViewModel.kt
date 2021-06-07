package cn.wj.android.cashbook.ui.asset.viewmodel

import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ACTION_ASSET
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_ASSET_INFO
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.interfaces.AssetListClickListener
import kotlinx.coroutines.launch

/**
 * 不可见资产 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/7
 */
class InvisibleAssetViewModel(private val local: LocalDataStore) : BaseViewModel(), AssetListClickListener {

    /** 显示资产菜单数据 */
    val showLongClickMenuData: MutableLiveData<AssetEntity> = MutableLiveData()

    /** 刷新状态 */
    val refreshing: MutableLiveData<Boolean> = object : MutableLiveData<Boolean>(true) {
        override fun onActive() {
            // 进入自动加载数据
            loadInvisibleAssetData()
        }

        override fun setValue(value: Boolean?) {
            super.setValue(value)
            if (value.condition) {
                // 刷新
                hideCapitalAccountList.value = false
                hideCreditCardAccountList.value = false
                hideTopUpAccountList.value = false
                hideInvestmentFinancialAccountList.value = false
                hideDebtAccountList.value = false
                loadInvisibleAssetData()
            }
        }
    }

    /** 资产数据 */
    private val assetListData: MutableLiveData<List<AssetEntity>> = MutableLiveData()

    /** 资金账户数据列表 */
    val capitalListData: LiveData<List<AssetEntity>> = assetListData.map {
        it.filter { asset ->
            asset.type == ClassificationTypeEnum.CAPITAL_ACCOUNT
        }
    }

    /** 标记 - 是否存在资金账户 */
    val hasCapitalAccount: LiveData<Boolean> = capitalListData.map {
        it.isNotEmpty()
    }

    /** 是否隐藏资金账户列表 */
    val hideCapitalAccountList: MutableLiveData<Boolean> = MutableLiveData(false)

    /** 信用卡账户数据列表 */
    val creditCardListData: LiveData<List<AssetEntity>> = assetListData.map {
        it.filter { asset ->
            asset.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT
        }
    }

    /** 标记 - 是否存在信用卡账户 */
    val hasCreditCardAccount: LiveData<Boolean> = creditCardListData.map {
        it.isNotEmpty()
    }

    /** 是否隐藏信用卡账户列表 */
    val hideCreditCardAccountList: MutableLiveData<Boolean> = MutableLiveData(false)

    /** 充值账户数据列表 */
    val topUpListData: LiveData<List<AssetEntity>> = assetListData.map {
        it.filter { asset ->
            asset.type == ClassificationTypeEnum.TOP_UP_ACCOUNT
        }
    }

    /** 标记 - 是否存在充值账户 */
    val hasTopUpAccount: LiveData<Boolean> = topUpListData.map {
        it.isNotEmpty()
    }

    /** 是否隐藏充值账户列表 */
    val hideTopUpAccountList: MutableLiveData<Boolean> = MutableLiveData(false)

    /** 理财账户数据列表 */
    val investmentFinancialListData: LiveData<List<AssetEntity>> = assetListData.map {
        it.filter { asset ->
            asset.type == ClassificationTypeEnum.INVESTMENT_FINANCIAL_ACCOUNT
        }
    }

    /** 标记 - 是否存在理财账户 */
    val hasInvestmentFinancialAccount: LiveData<Boolean> = investmentFinancialListData.map {
        it.isNotEmpty()
    }

    /** 是否隐藏理财账户列表 */
    val hideInvestmentFinancialAccountList: MutableLiveData<Boolean> = MutableLiveData(false)

    /** 债务账户数据列表 */
    val debtListData: LiveData<List<AssetEntity>> = assetListData.map {
        it.filter { asset ->
            asset.type == ClassificationTypeEnum.DEBT_ACCOUNT
        }
    }

    /** 标记 - 是否存在债务账户 */
    val hasDebtAccount: LiveData<Boolean> = debtListData.map {
        it.isNotEmpty()
    }

    /** 是否隐藏债务账户列表 */
    val hideDebtAccountList: MutableLiveData<Boolean> = MutableLiveData(false)


    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 资金账户箭头点击 */
    val onCapitalArrowClick: () -> Unit = {
        hideCapitalAccountList.value = !hideCapitalAccountList.value.condition
    }

    /** 信用卡账户箭头点击 */
    val onCreditCardArrowClick: () -> Unit = {
        hideCreditCardAccountList.value = !hideCreditCardAccountList.value.condition
    }

    /** 充值账户箭头点击 */
    val onTopUpArrowClick: () -> Unit = {
        hideTopUpAccountList.value = !hideTopUpAccountList.value.condition
    }

    /** 理财账户箭头点击 */
    val onInvestmentFinancialArrowClick: () -> Unit = {
        hideInvestmentFinancialAccountList.value = !hideInvestmentFinancialAccountList.value.condition
    }

    /** 债务账户箭头点击 */
    val onDebtArrowClick: () -> Unit = {
        hideDebtAccountList.value = !hideDebtAccountList.value.condition
    }

    /** 资产列表 item 点击 */
    override val onAssetItemClick: (AssetEntity) -> Unit = { item ->
        uiNavigationData.value = UiNavigationModel.builder {
            jump(
                ROUTE_PATH_ASSET_INFO, bundleOf(
                    ACTION_ASSET to item
                )
            )
        }
    }

    /** 资产列表 item 长点击 */
    override val onAssetItemLongClick: (AssetEntity) -> Unit = { item ->
        showLongClickMenuData.value = item
    }

    /** 加载隐藏资产数据 */
    private fun loadInvisibleAssetData() {
        viewModelScope.launch {
            try {
                assetListData.value = local.getInvisibleAssetListByBooksId(CurrentBooksLiveData.booksId)
            } catch (throwable: Throwable) {
                logger().e(throwable, "loadInvisibleAssetData")
            } finally {
                refreshing.value = false
            }
        }
    }

    /** 取消隐藏资产 */
    fun cancelHidden(asset: AssetEntity) {
        viewModelScope.launch {
            try {
                local.updateAsset(asset.copy(invisible = false))
                // 取消隐藏资产成功，更新列表
                loadInvisibleAssetData()
            } catch (throwable: Throwable) {
                logger().e(throwable, "cancelHidden")
            }
        }
    }
}