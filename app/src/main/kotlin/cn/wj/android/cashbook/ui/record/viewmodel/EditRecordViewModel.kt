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
import cn.wj.android.cashbook.base.ext.base.toNewList
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_MONTH_DAY
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.getSharedLong
import cn.wj.android.cashbook.base.tools.maps
import cn.wj.android.cashbook.base.tools.setSharedLong
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.EVENT_RECORD_CHANGE
import cn.wj.android.cashbook.data.constants.SHARED_KEY_LAST_ASSET_ID
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.entity.TagEntity
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.enums.CurrencyEnum
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.enums.TypeEnum
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.record.RecordRepository
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.manager.DatabaseManager
import cn.wj.android.cashbook.widget.calculator.SYMBOL_ZERO
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch

/**
 * 编辑记录 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/28
 */
class EditRecordViewModel(private val repository: RecordRepository) : BaseViewModel() {

    init {
        // 该界面用于快捷启动，需要单独初始化
        viewModelScope.launch {
            DatabaseManager.initDatabase(repository.database)
        }
    }

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
                associatedRecord.value = record
                this@EditRecordViewModel.chargeStr.value = charge
                this@EditRecordViewModel.remarkStr.value = remark
                reimbursableChecked.value = reimbursable
                when (currentItem.value.orElse(RecordTypeEnum.EXPENDITURE.position)) {
                    RecordTypeEnum.EXPENDITURE.position -> {
                        // 支出
                        expenditureType
                    }
                    RecordTypeEnum.INCOME.position -> {
                        // 收入
                        incomeType
                    }
                    else -> {
                        // 转账
                        transferType
                    }
                }.value = type
            }
        }

    /** 显示选择标签弹窗事件 */
    val showSelectTagDialogEvent: LifecycleEvent<List<TagEntity>> = LifecycleEvent()

    /** 跳转选择关联记录事件 */
    val jumpSelectAssociatedRecordEvent: LifecycleEvent<Boolean> = LifecycleEvent()

    /** 显示计算器弹窗事件 */
    val showCalculatorEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 显示选择账号弹窗事件 */
    val showSelectAssetEvent: LifecycleEvent<Boolean> = LifecycleEvent()

    /** 选择日期弹窗事件 */
    val showSelectDateEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 按钮是否允许点击 */
    val buttonEnable: MutableLiveData<Boolean> = MutableLiveData(true)

    /** 当前界面下标 */
    val currentItem: MutableLiveData<Int> = MutableLiveData(RecordTypeEnum.EXPENDITURE.position)

    /** 支出类型 */
    val expenditureType: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 支出类型 */
    val secondExpenditureType: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 收入类型 */
    val incomeType: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 收入类型 */
    val secondIncomeType: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 转账类型 */
    val transferType: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 转账类型 */
    val secondTransferType: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 账户信息 */
    val accountData: MutableLiveData<AssetEntity> = object : MutableLiveData<AssetEntity>() {
        private var firstLoad = true
        override fun onActive() {
            if (firstLoad) {
                if (null == record) {
                    viewModelScope.launch {
                        value = repository.findAssetById(getSharedLong(SHARED_KEY_LAST_ASSET_ID))
                    }
                }
                firstLoad = false
            }
        }
    }

    /** 转账转入账户信息 */
    val transferAccountData: MutableLiveData<AssetEntity> = MutableLiveData(null)

    /** 标签数据 */
    val tagsData: MutableLiveData<List<TagEntity>> = MutableLiveData(arrayListOf())

    /** 选中时间 */
    val dateData: MutableLiveData<Long> = MutableLiveData(System.currentTimeMillis())

    /** 显示时间 */
    val dateStr: LiveData<String> = dateData.map {
        it.dateFormat(DATE_FORMAT_NO_SECONDS)
    }

    /** 账户文本 */
    val accountStr: LiveData<String> = accountData.map {
        it?.showStr ?: R.string.account.string
    }

    /** 账户选中状态 */
    val accountChecked: LiveData<Boolean> = accountData.map {
        null != it
    }

    /** 是否显示转入账户 */
    val showTransferAccount: LiveData<Boolean> = currentItem.map {
        it == RecordTypeEnum.TRANSFER.position
    }

    /** 转账转入账户文本 */
    val transferAccountStr: LiveData<String> = transferAccountData.map {
        if (null == it) {
            R.string.into_account.string
        } else {
            R.string.into_with_colon.string + it.showStr
        }
    }

    /** 转账转入账户选中状态 */
    val transferAccountChecked: LiveData<Boolean> = transferAccountData.map {
        null != it
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
                    append(tag.name)
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

    /** 是否显示关联记录 */
    val showAssociatedRecord: LiveData<Boolean> = maps(currentItem, incomeType) {
        currentItem.value == RecordTypeEnum.INCOME.position && (incomeType.value?.refund.condition || incomeType.value?.reimburse.condition)
    }

    /** 关联记录数据 */
    val associatedRecord: MutableLiveData<RecordEntity> = MutableLiveData(null)

    /** 关联记录数据选中状态 */
    val associatedRecordChecked: LiveData<Boolean> = associatedRecord.map {
        null != it
    }

    /** 关联记录显示文本 */
    val associatedRecordStr: LiveData<String> = associatedRecord.map {
        if (null == it) {
            R.string.associated_expenditure_record.string
        } else {
            "${R.string.associated_with_colon.string}${it.recordTime.dateFormat(DATE_FORMAT_MONTH_DAY)} ${it.typeStr} ${it.amountStr}"
        }
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
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 账户点击 */
    val onAccountClick: () -> Unit = {
        showSelectAssetEvent.value = true
    }

    /** 转账转入账户点击 */
    val onTransferAccountClick: () -> Unit = {
        showSelectAssetEvent.value = false
    }

    /** 标签点击 */
    val onTagsClick: () -> Unit = {
        // 显示选择标签弹窗
        showSelectTagDialogEvent.value = tagsData.value.orEmpty()
    }

    /** 日期点击 */
    val onDateClick: () -> Unit = {
        // 以当前选中时间显示弹窗
        showSelectDateEvent.value = 0
    }

    /** 关联记录点击 */
    val onAssociatedRecordClick: () -> Unit = {
        jumpSelectAssociatedRecordEvent.value = incomeType.value?.refund.condition
    }

    /** 关联记录清除点击 */
    val onAssociateRecordCloseClick: () -> Unit = {
        // 清除关联记录
        associatedRecord.value = null
    }

    /** 金额点击 */
    val onAmountClick: () -> Unit = {
        showCalculatorEvent.value = 0
    }

    /** 保存点击 */
    val onSaveClick: () -> Unit = {
        checkToSave()
    }

    /** 检查并保存 */
    private fun checkToSave() {
        if (calculatorStr.get()?.toFloatOrNull().orElse(0f) == 0f) {
            // 金额不能为 0
            snackbarEvent.value = R.string.amount_can_not_be_zero.string.toSnackbarModel()
            return
        }
        val amount = calculatorStr.get().moneyFormat()
        val typeValue = when (currentItem.value.orElse(RecordTypeEnum.EXPENDITURE.position)) {
            RecordTypeEnum.EXPENDITURE.position -> {
                // 支出
                expenditureType
            }
            RecordTypeEnum.INCOME.position -> {
                // 收入
                incomeType
            }
            else -> {
                // 转账
                transferType
            }
        }.value
        if (null == typeValue) {
            // 未选择类型
            snackbarEvent.value = R.string.please_select_type.string.toSnackbarModel()
            return
        }
        val childType = typeValue.childList.firstOrNull { it.selected.get() }
        val type = if (typeValue.type == TypeEnum.FIRST && typeValue.expand.get() && null != childType) {
            // 一级菜单，展开且子类型选中
            childType
        } else {
            typeValue
        }
        val asset = accountData.value
        val intoAsset = if (currentItem.value == RecordTypeEnum.TRANSFER.position) {
            transferAccountData.value
        } else {
            null
        }
        if (currentItem.value == RecordTypeEnum.TRANSFER.position) {
            // 转账类型，转出转入资产不能为空
            if (null == asset) {
                snackbarEvent.value = R.string.please_select_asset.string.toSnackbarModel()
                return
            }
            if (null == intoAsset) {
                snackbarEvent.value = R.string.please_select_into_asset.string.toSnackbarModel()
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
                    repository.insertRecord(
                        RecordEntity(
                            id = -1,
                            typeEnum = RecordTypeEnum.fromPosition(currentItem.value.orElse(RecordTypeEnum.EXPENDITURE.position)).orElse(RecordTypeEnum.EXPENDITURE),
                            type = type,
                            asset = asset,
                            intoAsset = intoAsset,
                            booksId = CurrentBooksLiveData.booksId,
                            record = associatedRecord.value,
                            beAssociated = null,
                            amount = amount,
                            charge = charge,
                            remark = remarkStr.value.orEmpty(),
                            tags = tagsData.value.orEmpty(),
                            reimbursable = reimbursableChecked.value.condition,
                            system = false,
                            recordTime = dateData.value.orElse(System.currentTimeMillis()),
                            createTime = currentDate,
                            modifyTime = currentDate,
                            showDate = false
                        )
                    )
                } else {
                    // 修改
                    repository.updateRecord(
                        record!!.copy(
                            typeEnum = RecordTypeEnum.fromPosition(currentItem.value.orElse(RecordTypeEnum.EXPENDITURE.position)).orElse(RecordTypeEnum.EXPENDITURE),
                            type = type,
                            asset = accountData.value,
                            intoAsset = intoAsset,
                            record = associatedRecord.value,
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
                uiNavigationEvent.value = UiNavigationModel.builder {
                    close()
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "checkToSave")
            } finally {
                buttonEnable.value = true
            }
        }
    }

    /** 通知标签 [tag] 变化 */
    fun notifyTagChanged(tag: TagEntity) {
        val list = tagsData.value.toNewList()
        val index = list.indexOfFirst { it.id == tag.id }
        if (index >= 0) {
            list[index] = tag
        }
        tagsData.value = list
    }

    /** 通知标签 [tag] 删除 */
    fun notifyTagDelete(tag: TagEntity) {
        val list = tagsData.value.toNewList()
        val index = list.indexOfFirst { it.id == tag.id }
        if (index >= 0) {
            list.removeAt(index)
        }
        tagsData.value = list
    }
}