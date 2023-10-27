package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.completeZero
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordBarEntity
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordPieEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.ProgressDialogManager
import cn.wj.android.cashbook.core.ui.runCatchWithProgress
import cn.wj.android.cashbook.domain.usecase.GetRecordViewsBetweenDateUseCase
import cn.wj.android.cashbook.domain.usecase.TransRecordViewsToAnalyticsBarUseCase
import cn.wj.android.cashbook.domain.usecase.TransRecordViewsToAnalyticsPieSecondUseCase
import cn.wj.android.cashbook.domain.usecase.TransRecordViewsToAnalyticsPieUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 数据分析 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/23
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val typeRepository: TypeRepository,
    getRecordViewsBetweenDateUseCase: GetRecordViewsBetweenDateUseCase,
    transRecordViewsToAnalyticsBarUseCase: TransRecordViewsToAnalyticsBarUseCase,
    transRecordViewsToAnalyticsPieUseCase: TransRecordViewsToAnalyticsPieUseCase,
    private val transRecordViewsToAnalyticsPieSecondUseCase: TransRecordViewsToAnalyticsPieSecondUseCase,
) : ViewModel() {

    var dialogState: DialogState by mutableStateOf(DialogState.Dismiss)
        private set

    var sheetData: ShowSheetData? by mutableStateOf(null)
        private set

    /** 当前选择时间 */
    private val _dateData = MutableStateFlow(DateData(LocalDate.now()))

    private val _recordListData = _dateData.mapLatest { date ->
        getRecordViewsBetweenDateUseCase(date.from, date.to, date.year)
    }

    val uiState = _recordListData.mapLatest { list ->
        val date = _dateData.first()
        val crossYear: Boolean
        val titleText: String
        when {
            date.year -> {
                crossYear = false
                titleText = date.from.year.toString()
            }

            date.to != null -> {
                crossYear = date.from.year != date.to.year
                titleText =
                    "${date.from.year}-${date.from.monthValue.completeZero()}-${date.from.dayOfMonth.completeZero()}\n" +
                            "${date.to.year}-${date.to.monthValue.completeZero()}-${date.to.dayOfMonth.completeZero()}"
            }

            else -> {
                crossYear = false
                titleText = "${date.from.year}-${date.from.monthValue.completeZero()}"
            }
        }
        var totalIncome = BigDecimal.ZERO
        var totalExpenditure = BigDecimal.ZERO
        var totalBalance = BigDecimal.ZERO
        val barList = transRecordViewsToAnalyticsBarUseCase(date.from, date.to, date.year, list)
        barList.forEach {
            totalIncome += it.income.toBigDecimalOrZero()
            totalExpenditure += it.expenditure.toBigDecimalOrZero()
            totalBalance += it.balance.toBigDecimalOrZero()
        }
        val expenditurePieDataList =
            transRecordViewsToAnalyticsPieUseCase(RecordTypeCategoryEnum.EXPENDITURE, list)
        val incomePieDataList =
            transRecordViewsToAnalyticsPieUseCase(RecordTypeCategoryEnum.INCOME, list)
        val transferPieDataList =
            transRecordViewsToAnalyticsPieUseCase(RecordTypeCategoryEnum.TRANSFER, list)
        val success = AnalyticsUiState.Success(
            year = date.year,
            crossYear = crossYear,
            titleText = titleText,
            noData = list.isEmpty(),
            totalIncome = totalIncome.decimalFormat(),
            totalExpenditure = totalExpenditure.decimalFormat(),
            totalBalance = totalBalance.decimalFormat(),
            barDataList = barList,
            expenditurePieDataList = expenditurePieDataList,
            incomePieDataList = incomePieDataList,
            transferPieDataList = transferPieDataList,
        )
        ProgressDialogManager.dismiss()
        success
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AnalyticsUiState.Loading,
        )

    fun showSelectDateDialog() {
        viewModelScope.launch {
            dialogState = DialogState.Shown(ShowSelectDateDialogData.SelectDate(_dateData.first()))
        }
    }

    fun showSelectDateRangeDialog() {
        viewModelScope.launch {
            dialogState =
                DialogState.Shown(ShowSelectDateDialogData.SelectRangeDate(_dateData.first()))
        }
    }

    fun selectDate(date: DateData) {
        dismissDialog()
        viewModelScope.launch {
            if (date != _dateData.first()) {
                _dateData.tryEmit(date)
                ProgressDialogManager.show()
            }
        }
    }

    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }

    fun showSheet(typeId: Long) {
        viewModelScope.launch {
            runCatchWithProgress {
                val type = typeRepository.getRecordTypeById(typeId) ?: return@runCatchWithProgress
                val ls =
                    transRecordViewsToAnalyticsPieSecondUseCase(typeId, _recordListData.first())
                if (ls.isNotEmpty()) {
                    sheetData = ShowSheetData(
                        typeId = typeId,
                        typeName = type.name,
                        dataList = ls,
                    )
                }
            }
        }
    }

    fun dismissSheet() {
        sheetData = null
    }
}

sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data class Success(
        val year: Boolean,
        val crossYear: Boolean,
        val titleText: String,
        val totalIncome: String,
        val totalExpenditure: String,
        val totalBalance: String,
        val noData: Boolean,
        val barDataList: List<AnalyticsRecordBarEntity>,
        val expenditurePieDataList: List<AnalyticsRecordPieEntity>,
        val incomePieDataList: List<AnalyticsRecordPieEntity>,
        val transferPieDataList: List<AnalyticsRecordPieEntity>,
    ) : AnalyticsUiState
}

data class DateData(
    val from: LocalDate,
    val to: LocalDate? = null,
    val year: Boolean = false,
)

data class ShowSheetData(
    val typeId: Long,
    val typeName: String,
    val dataList: List<AnalyticsRecordPieEntity>,
)

sealed class ShowSelectDateDialogData(open val date: DateData) {
    data class SelectDate(override val date: DateData) : ShowSelectDateDialogData(date)
    data class SelectRangeDate(override val date: DateData) : ShowSelectDateDialogData(date)
}