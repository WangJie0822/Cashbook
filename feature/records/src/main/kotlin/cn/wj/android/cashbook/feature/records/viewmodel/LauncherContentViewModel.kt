package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.common.ext.withSymbol
import cn.wj.android.cashbook.core.model.entity.RecordDayEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.ResultModel
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.domain.usecase.DeleteRecordUseCase
import cn.wj.android.cashbook.domain.usecase.GetCurrentBookUseCase
import cn.wj.android.cashbook.domain.usecase.GetCurrentMonthRecordViewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LauncherContentViewModel @Inject constructor(
    getCurrentBookUseCase: GetCurrentBookUseCase,
    getCurrentMonthRecordViewsUseCase: GetCurrentMonthRecordViewsUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase,
) : ViewModel() {

    /** 删除记录失败错误信息 */
    var shouldDisplayDeleteFailedBookmark by mutableStateOf(0)

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)

    /** 记录详情数据 */
    var viewRecord by mutableStateOf<RecordViewsEntity?>(null)

    /** 账本名 */
    val currentBookName = getCurrentBookUseCase()
        .mapLatest { it.name }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "",
        )

    /** 当前月记录数据 */
    val currentMonthRecordListMapData: StateFlow<Map<RecordDayEntity, List<RecordViewsEntity>>> =
        getCurrentMonthRecordViewsUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = mapOf(),
            )

    private val currentMonthRecordListData: Flow<List<RecordViewsEntity>> =
        currentMonthRecordListMapData
            .mapLatest { map ->
                val result = arrayListOf<RecordViewsEntity>()
                map.forEach {
                    result.addAll(it.value)
                }
                result
            }

    val monthIncomeText: StateFlow<String> = currentMonthRecordListData
        .mapLatest {
            it.calculateIncome().withSymbol()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "${Symbol.rmb}0",
        )

    val monthExpandText: StateFlow<String> = currentMonthRecordListData
        .mapLatest {
            it.calculateExpand().withSymbol()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "${Symbol.rmb}0",
        )

    val monthBalanceText: StateFlow<String> = currentMonthRecordListData
        .mapLatest {
            it.calculateBalance().withSymbol()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "${Symbol.rmb}0",
        )

    fun onBookmarkDismiss() {
        shouldDisplayDeleteFailedBookmark = 0
    }

    fun onRecordDetailsSheetDismiss() {
        viewRecord = null
    }

    fun onRecordItemClick(record: RecordViewsEntity) {
        viewRecord = record
    }

    fun onRecordItemDeleteClick(recordId: Long) {
        viewRecord = null
        dialogState = DialogState.Shown(recordId)
    }

    fun onDeleteRecordConfirm(recordId: Long) {
        viewModelScope.launch {
            try {
                deleteRecordUseCase(recordId)
                // 删除成功，隐藏弹窗
                onDialogDismiss()
            } catch (throwable: Throwable) {
                this@LauncherContentViewModel.logger()
                    .e(throwable, "tryDeleteRecord(recordId = <$recordId>) failed")
                // 提示
                shouldDisplayDeleteFailedBookmark = ResultModel.Failure.FAILURE_THROWABLE
            }
        }
    }

    fun onDialogDismiss() {
        dialogState = DialogState.Dismiss
    }
}

private fun List<RecordViewsEntity>.calculateIncome(): String {
    var totalIncome = BigDecimal.ZERO
    this.forEach { record ->
        if (record.typeCategory == RecordTypeCategoryEnum.INCOME) {
            // 收入
            totalIncome += (record.amount.toBigDecimalOrZero() - record.charges.toBigDecimalOrZero())
        } else if (record.typeCategory == RecordTypeCategoryEnum.TRANSFER) {
            // 转账
            totalIncome += record.concessions.toBigDecimalOrZero()
        }
    }
    return totalIncome.decimalFormat()
}

private fun List<RecordViewsEntity>.calculateExpand(): String {
    var totalExpenditure = BigDecimal.ZERO
    this.forEach { record ->
        if (record.typeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
            // 支出
            totalExpenditure += (record.amount.toBigDecimalOrZero() + record.charges.toBigDecimalOrZero() - record.concessions.toBigDecimalOrZero())
        } else if (record.typeCategory == RecordTypeCategoryEnum.TRANSFER) {
            // 转账
            totalExpenditure += record.charges.toBigDecimalOrZero()
        }
    }
    return totalExpenditure.decimalFormat()
}

private fun List<RecordViewsEntity>.calculateBalance(): String {
    val income = this.calculateIncome()
    val expand = this.calculateExpand()
    return (income.toBigDecimalOrZero() - expand.toBigDecimalOrZero()).decimalFormat()
}