package cn.wj.android.cashbook.ui.asset.viewmodel

import androidx.core.math.MathUtils
import androidx.databinding.ObservableFloat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.formatToNumber
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.interfaces.AssetListClickListener
import kotlinx.coroutines.launch

/**
 * 我的资产 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/3
 */
class MyAssetViewModel(private val local: LocalDataStore) : BaseViewModel(), AssetListClickListener {

    /** 显示资产菜单数据 */
    val showLongClickMenuData: MutableLiveData<AssetEntity> = MutableLiveData()

    /** 显示更多菜单数据 */
    val showMoreMenuData: MutableLiveData<Int> = MutableLiveData()

    /** 标记 - 是否允许标题 */
    val titleEnable: MutableLiveData<Boolean> = MutableLiveData(false)

    /** 刷新状态 */
    val refreshing: MutableLiveData<Boolean> = object : MutableLiveData<Boolean>(true) {
        override fun onActive() {
            // 进入自动加载数据
            loadAssetData()
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
                loadAssetData()
            }
        }
    }

    /** 资产数据 */
    private val assetListData: MutableLiveData<List<AssetEntity>> = MutableLiveData()

    /** 资金账户数据列表 */
    val capitalListData: LiveData<List<AssetEntity>> = assetListData.map {
        it.filter { asset ->
            !asset.invisible && asset.type == ClassificationTypeEnum.CAPITAL_ACCOUNT
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
            !asset.invisible && asset.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT
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
            !asset.invisible && asset.type == ClassificationTypeEnum.TOP_UP_ACCOUNT
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
            !asset.invisible && asset.type == ClassificationTypeEnum.INVESTMENT_FINANCIAL_ACCOUNT
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
            !asset.invisible && asset.type == ClassificationTypeEnum.DEBT_ACCOUNT
        }
    }

    /** 标记 - 是否存在债务账户 */
    val hasDebtAccount: LiveData<Boolean> = debtListData.map {
        it.isNotEmpty()
    }

    /** 是否隐藏债务账户列表 */
    val hideDebtAccountList: MutableLiveData<Boolean> = MutableLiveData(false)

    /** 净资产 */
    val netAssets: LiveData<String> = assetListData.map {
        // 净资产为总资产-总借入-总信用卡欠款
        if (it.isEmpty()) {
            R.string.nothing.string
        } else {
            var total = "0".toBigDecimal()
            var totalBorrow = "0".toBigDecimal()
            var totalCreditCard = "0".toBigDecimal()
            it.forEach { asset ->
                if (asset.type != ClassificationTypeEnum.CREDIT_CARD_ACCOUNT && asset.classification != AssetClassificationEnum.BORROW) {
                    // 总资产
                    total += asset.balance.toBigDecimal()
                }
                if (asset.classification == AssetClassificationEnum.BORROW) {
                    // 借入
                    totalBorrow += asset.balance.toBigDecimal()
                }
                if (asset.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT && asset.balance.toFloatOrNull().orElse(0f) > 0) {
                    // 信用卡且有欠款
                    totalCreditCard += asset.balance.toBigDecimal()
                }
            }
            "${CurrentBooksLiveData.currency.symbol}${(total - totalBorrow - totalCreditCard).formatToNumber()}"
        }
    }

    /** 总资产 */
    val totalAssets: LiveData<String> = assetListData.map {
        if (it.isEmpty()) {
            R.string.nothing.string
        } else {
            // 总资产为除去信用卡、借入，所有资产总额
            var total = "0".toBigDecimal()
            it.filter { asset ->
                asset.type != ClassificationTypeEnum.CREDIT_CARD_ACCOUNT && asset.classification != AssetClassificationEnum.BORROW
            }.forEach { asset ->
                total += asset.balance.toBigDecimal()
            }
            "${CurrentBooksLiveData.currency.symbol}${total.formatToNumber()}"
        }
    }

    /** 总负债 */
    val totalLiabilities: LiveData<String> = assetListData.map {
        if (it.isEmpty()) {
            R.string.nothing.string
        } else {
            // 总负债为所有信用卡欠款、借入总额
            var total = "0".toBigDecimal()
            it.filter { asset ->
                (asset.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT && asset.balance.toFloatOrNull().orElse(0f) > 0) || asset.classification == AssetClassificationEnum.BORROW
            }.forEach { asset ->
                total += asset.balance.toBigDecimal()
            }
            "${CurrentBooksLiveData.currency.symbol}${total.formatToNumber()}"
        }
    }

    /** 净资产透明度 */
    val netAssetsAlpha = ObservableFloat(1f)

    /** 总资产、总负债透明度 */
    val totalAlpha = ObservableFloat(1f)

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 状态栏折叠进度监听 */
    val onCollapsingChanged: (Float) -> Unit = { percent ->
        // 完全折叠时才显示标题文本
        titleEnable.value = percent <= 0.13f
        // 净资产透明度
        netAssetsAlpha.set(MathUtils.clamp((1 - (0.9f - percent) / 0.3f), 0f, 1f))
        // 总资产、总负债透明度
        totalAlpha.set(MathUtils.clamp((1 - (0.4f - percent) / 0.3f), 0f, 1f))
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
        // TODO
        snackbarData.value = item.name.toSnackbarModel()
    }

    /** 资产列表 item 长点击 */
    override val onAssetItemLongClick: (AssetEntity) -> Unit = { item ->
        showLongClickMenuData.value = item
    }

    /** 更多菜单点击 */
    val onMoreClick: () -> Unit = {
        showMoreMenuData.value = 0
    }

    /** 加载所有资产数据 */
    private fun loadAssetData() {
        viewModelScope.launch {
            try {
                assetListData.value = local.getAssetListByBooksId(CurrentBooksLiveData.booksId)
            } catch (throwable: Throwable) {
                logger().e(throwable, "loadAssetData")
            } finally {
                refreshing.value = false
            }
        }
    }

    /** 隐藏资产 */
    fun hideAsset(asset: AssetEntity) {
        viewModelScope.launch {
            try {
                local.updateAsset(asset.copy(invisible = true))
                // 隐藏资产成功，更新列表
                loadAssetData()
            } catch (throwable: Throwable) {
                logger().e(throwable, "hideAsset")
            }
        }
    }
}