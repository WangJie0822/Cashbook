package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.color
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.moneyFormat
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.getSharedLong
import cn.wj.android.cashbook.base.tools.setSharedLong
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.EVENT_RECORD_CHANGE
import cn.wj.android.cashbook.data.constants.SHARED_KEY_LAST_ASSET_ID
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.enums.CurrencyEnum
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.widget.calculator.SYMBOL_ZERO
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch

/**
 * 编辑记录 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/28
 */
class EditRecordViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 编辑数据 */
    var record: RecordEntity? = null
        set(value) {
            field = value
            value?.run {
                calculatorStr.set(amount)
                accountData.value = asset
                transferAccountData.value = intoAsset
                tagsData.value = tags
                dateData.value = recordTime
                this@EditRecordViewModel.chargeStr.value = charge
                remarkStr.value = remark
                reimbursableChecked.value = reimbursable
            }
        }

    /** 显示计算器弹窗数据 */
    val showCalculatorData: MutableLiveData<Int> = MutableLiveData()

    /** 显示选择账号弹窗 */
    val showSelectAssetData: MutableLiveData<Boolean> = MutableLiveData()

    /** 选择日期弹窗 */
    val showSelectDateData: MutableLiveData<Int> = MutableLiveData()

    /** 按钮是否允许点击 */
    val buttonEnable: MutableLiveData<Boolean> = MutableLiveData(true)

    /** 当前界面下标 */
    val currentItem: MutableLiveData<Int> = MutableLiveData(RecordTypeEnum.EXPENDITURE.position)

    /** 支出类型 */
    val firstExpenditureType: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 支出类型 */
    val secondExpenditureType: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 收入类型 */
    val firstIncomeType: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 收入类型 */
    val secondIncomeType: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 转账类型 */
    val firstTransferType: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 转账类型 */
    val secondTransferType: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 账户信息 */
    val accountData: MutableLiveData<AssetEntity> = object : MutableLiveData<AssetEntity>() {
        private var firstLoad = true
        override fun onActive() {
            if (firstLoad) {
                viewModelScope.launch {
                    value = local.findAssetById(getSharedLong(SHARED_KEY_LAST_ASSET_ID))
                }
                firstLoad = false
            }
        }
    }

    /** 转账转入账户信息 */
    val transferAccountData: MutableLiveData<AssetEntity> = MutableLiveData(null)

    /** TODO 标签数据 */
    val tagsData: MutableLiveData<List<String>> = MutableLiveData(arrayListOf())

    /** 选中时间 */
    val dateData: MutableLiveData<Long> = MutableLiveData(System.currentTimeMillis())

    /** 显示时间 */
    val dateStr: LiveData<String> = dateData.map {
        it.dateFormat(DATE_FORMAT_NO_SECONDS)
    }

    /** 账户文本 */
    val accountStr: LiveData<String> = accountData.map {
        if (null == it || it.classification == AssetClassificationEnum.NOT_SELECT) {
            R.string.account.string
        } else {
            it.showStr
        }
    }

    /** 账户选中状态 */
    val accountChecked: LiveData<Boolean> = accountData.map {
        null != it && it.classification !== AssetClassificationEnum.NOT_SELECT
    }

    /** 是否显示转入账户 */
    val showTransferAccount: LiveData<Boolean> = currentItem.map {
        it == RecordTypeEnum.TRANSFER.position
    }

    /** 转账转入账户文本 */
    val transferAccountStr: LiveData<String> = transferAccountData.map {
        if (null == it || it.classification == AssetClassificationEnum.NOT_SELECT) {
            R.string.into_account.string
        } else {
            R.string.into_with_colon.string + it.showStr
        }
    }

    /** 转账转入账户选中状态 */
    val transferAccountChecked: LiveData<Boolean> = transferAccountData.map {
        null != it && it.classification !== AssetClassificationEnum.NOT_SELECT
    }

    /** 标签文本 */
    val tagsStr: LiveData<String> = tagsData.map {
        if (it.isEmpty()) {
            R.string.tags.string
        } else {
            with(StringBuilder()) {
                it.forEach { tag ->
                    if (isNotBlank()) {
                        append(",")
                    }
                    append(tag)
                }
                toString()
            }
        }
    }

    /** 标签选中状态 */
    val tagsChecked: LiveData<Boolean> = tagsData.map {
        !it.isNullOrEmpty()
    }

    /** 手续费文本 */
    val chargeStr: MutableLiveData<String> = MutableLiveData()

    /** 备注文本 */
    val remarkStr: MutableLiveData<String> = MutableLiveData()

    /** 手续费选中状态 */
    val chargeChecked: MutableLiveData<Boolean> = MutableLiveData(false)

    /** 是否显示手续费 */
    val showCharge: LiveData<Boolean> = currentItem.map {
        it == RecordTypeEnum.TRANSFER.position
    }

    /** 可报销选中状态 */
    val reimbursableChecked: MutableLiveData<Boolean> = MutableLiveData()

    /** 是否显示可报销 */
    val showReimbursable: LiveData<Boolean> = currentItem.map {
        it == RecordTypeEnum.EXPENDITURE.position
    }

    /** 货币符号 */
    val currencySymbol: LiveData<String> = CurrentBooksLiveData.map {
        (it.currency ?: CurrencyEnum.CNY).symbol
    }

    /** 计算结果显示 */
    val calculatorStr: ObservableField<String> = ObservableField(SYMBOL_ZERO)

    /** 界面主色调 */
    val primaryTint: LiveData<Int> = currentItem.map {
        when (it) {
            RecordTypeEnum.EXPENDITURE.position -> {
                // 支出
                R.color.color_expenditure
            }
            RecordTypeEnum.INCOME.position -> {
                // 收入
                R.color.color_income
            }
            else -> {
                // 转账
                R.color.color_primary
            }
        }.color
    }

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 账户点击 */
    val onAccountClick: () -> Unit = {
        showSelectAssetData.value = true
    }

    /** 转账转入账户点击 */
    val onTransferAccountClick: () -> Unit = {
        showSelectAssetData.value = false
    }

    /** 标签点击 */
    val onTagsClick: () -> Unit = {
        snackbarData.value = "标签点击".toSnackbarModel()
    }

    /** 日期点击 */
    val onDateClick: () -> Unit = {
        // 以当前选中时间显示弹窗
        showSelectDateData.value = 0
    }

    /** 金额点击 */
    val onAmountClick: () -> Unit = {
        showCalculatorData.value = 0
    }

    /** 保存点击 */
    val onSaveClick: () -> Unit = {
        checkToSave()
    }

    /** 检查并保存 */
    private fun checkToSave() {
        if (calculatorStr.get()?.toFloatOrNull().orElse(0f) == 0f) {
            // 金额不能为 0
            snackbarData.value = R.string.amount_can_not_be_zero.string.toSnackbarModel()
            return
        }
        val amount = calculatorStr.get().moneyFormat()
        val firstType = when (currentItem.value.orElse(RecordTypeEnum.EXPENDITURE.position)) {
            RecordTypeEnum.EXPENDITURE.position -> {
                // 支出
                firstExpenditureType
            }
            RecordTypeEnum.INCOME.position -> {
                // 收入
                firstIncomeType
            }
            else -> {
                // 转账
                firstTransferType
            }
        }.value
        if (null == firstType) {
            // 未选择类型
            snackbarData.value = R.string.please_select_type.string.toSnackbarModel()
            return
        }
        val secondType = when (currentItem.value.orElse(RecordTypeEnum.EXPENDITURE.position)) {
            RecordTypeEnum.EXPENDITURE.position -> {
                // 支出
                secondExpenditureType
            }
            RecordTypeEnum.INCOME.position -> {
                // 收入
                secondIncomeType
            }
            else -> {
                // 转账
                secondTransferType
            }
        }.value
        val asset = accountData.value
        val intoAsset = if (currentItem.value == RecordTypeEnum.TRANSFER.position) {
            transferAccountData.value
        } else {
            null
        }
        if (currentItem.value == RecordTypeEnum.TRANSFER.position) {
            // 转账类型，转出转入资产不能为空
            if (null == asset) {
                snackbarData.value = R.string.please_select_asset.string.toSnackbarModel()
                return
            }
            if (null == intoAsset) {
                snackbarData.value = R.string.please_select_into_asset.string.toSnackbarModel()
                return
            }
        }
        val charge = if (currentItem.value == RecordTypeEnum.TRANSFER.position) {
            chargeStr.value.moneyFormat()
        } else {
            ""
        }
        val currentDate = System.currentTimeMillis().dateFormat()
        viewModelScope.launch {
            try {
                buttonEnable.value = false
                if (null == record) {
                    // 新建
                    local.insertRecord(
                        RecordEntity(
                            id = -1,
                            type = RecordTypeEnum.fromPosition(currentItem.value.orElse(RecordTypeEnum.EXPENDITURE.position)).orElse(RecordTypeEnum.EXPENDITURE),
                            firstType = firstType,
                            secondType = secondType,
                            asset = asset,
                            intoAsset = intoAsset,
                            booksId = CurrentBooksLiveData.booksId,
                            amount = amount,
                            charge = charge,
                            remark = remarkStr.value.orEmpty(),
                            tags = tagsData.value.orEmpty(),
                            reimbursable = reimbursableChecked.value.condition,
                            system = false,
                            recordTime = dateData.value.orElse(System.currentTimeMillis()),
                            createTime = currentDate,
                            modifyTime = currentDate
                        )
                    )
                } else {
                    // 修改
                    local.updateRecord(
                        record!!.copy(
                            type = RecordTypeEnum.fromPosition(currentItem.value.orElse(RecordTypeEnum.EXPENDITURE.position)).orElse(RecordTypeEnum.EXPENDITURE),
                            firstType = firstType,
                            secondType = secondType,
                            asset = accountData.value,
                            intoAsset = intoAsset,
                            amount = amount,
                            charge = charge,
                            remark = remarkStr.value.orEmpty(),
                            tags = tagsData.value.orEmpty(),
                            reimbursable = reimbursableChecked.value.condition,
                            recordTime = dateData.value.orElse(System.currentTimeMillis()),
                            modifyTime = System.currentTimeMillis().dateFormat()
                        )
                    )
                }
                // 通知记录变化
                LiveEventBus.get(EVENT_RECORD_CHANGE).post(0)
                // 插入成功，保存本次资产
                setSharedLong(SHARED_KEY_LAST_ASSET_ID, accountData.value?.id)
                // 关闭当前界面
                uiNavigationData.value = UiNavigationModel.builder {
                    close()
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "checkToSave")
            } finally {
                buttonEnable.value = true
            }
        }
    }
}