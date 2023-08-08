package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.common.ext.yearMonth
import cn.wj.android.cashbook.core.model.entity.RecordDayEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.ResultModel
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.domain.usecase.DeleteRecordUseCase
import cn.wj.android.cashbook.domain.usecase.GetCurrentMonthRecordViewsMapUseCase
import cn.wj.android.cashbook.domain.usecase.GetCurrentMonthRecordViewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 日历 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/4
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    getCurrentMonthRecordViewsUseCase: GetCurrentMonthRecordViewsUseCase,
    getCurrentMonthRecordViewsMapUseCase: GetCurrentMonthRecordViewsMapUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase,
) : ViewModel() {

    /** 删除记录失败错误信息 */
    var shouldDisplayDeleteFailedBookmark by mutableStateOf(0)
        private set

    /** 记录详情数据 */
    var viewRecord by mutableStateOf<RecordViewsEntity?>(null)
        private set

    /** 弹窗状态 */
    var dialogState: DialogState by mutableStateOf(DialogState.Dismiss)
        private set

    /** 日期数据 - yyyy-MM-dd 默认今天 */
    private val _dateData = MutableStateFlow(LocalDate.now())
    val dateData: StateFlow<LocalDate> = _dateData


    /** 当前月记录数据 */
    private val currentMonthRecordListData = _dateData.flatMapLatest { date ->
        getCurrentMonthRecordViewsUseCase(date.year.toString(), date.monthValue.toString())
    }

    val uiState = currentMonthRecordListData.mapLatest { list ->
        val selectedDate = _dateData.first()
        val selectedYearMonth = selectedDate.yearMonth
        val selectedDay = selectedDate.dayOfMonth
        val recordList = arrayListOf<RecordViewsEntity>()
        val schemas = mutableMapOf<LocalDate, RecordDayEntity>()
        var totalIncome = BigDecimal.ZERO
        var totalExpenditure = BigDecimal.ZERO
        getCurrentMonthRecordViewsMapUseCase(list).forEach {
            totalIncome += it.key.dayIncome.toBigDecimalOrZero()
            totalExpenditure += it.key.dayExpand.toBigDecimalOrZero()
            if (selectedDay == it.key.day) {
                recordList.addAll(it.value)
            }
            schemas[selectedYearMonth.atDay(it.key.day)] = it.key
        }
        CalendarUiState.Success(
            monthIncome = totalIncome.decimalFormat(),
            monthExpand = totalExpenditure.decimalFormat(),
            monthBalance = (totalIncome - totalExpenditure).decimalFormat(),
            schemas = schemas,
            recordList = recordList,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CalendarUiState.Loading,
        )

    fun showDateSelectDialog() {
        viewModelScope.launch {
            dialogState = DialogState.Shown(DialogType.SelectDate(_dateData.first().yearMonth))
        }
    }

    fun onDateSelected(date: LocalDate) {
        onDialogDismiss()
        _dateData.tryEmit(date)
    }

    fun onRecordItemClick(record: RecordViewsEntity) {
        viewRecord = record
    }

    fun onSheetDismiss() {
        viewRecord = null
    }

    fun onDialogDismiss() {
        dialogState = DialogState.Dismiss
    }

    fun onRecordItemDeleteClick(recordId: Long) {
        onSheetDismiss()
        dialogState = DialogState.Shown(DialogType.DeleteRecord(recordId))
    }

    fun onDeleteRecordConfirm(recordId: Long) {
        viewModelScope.launch {
            try {
                deleteRecordUseCase(recordId)
                // 删除成功，隐藏弹窗
                onDialogDismiss()
            } catch (throwable: Throwable) {
                this@CalendarViewModel.logger()
                    .e(throwable, "onDeleteRecordConfirm(recordId = <$recordId>) failed")
                // 提示
                shouldDisplayDeleteFailedBookmark = ResultModel.Failure.FAILURE_THROWABLE
            }
        }
    }

    fun onBookmarkDismiss() {
        shouldDisplayDeleteFailedBookmark = 0
    }
}

sealed interface CalendarUiState {
    object Loading : CalendarUiState
    data class Success(
        val monthIncome: String,
        val monthExpand: String,
        val monthBalance: String,
        val schemas: Map<LocalDate, RecordDayEntity>,
        val recordList: List<RecordViewsEntity>
    ) : CalendarUiState
}