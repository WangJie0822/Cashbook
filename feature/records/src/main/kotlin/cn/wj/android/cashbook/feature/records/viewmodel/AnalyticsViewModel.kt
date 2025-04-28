/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

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
            crossYear = date.crossYear,
            titleText = date.titleText,
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

/**
 * 日期数据
 *
 * @param from 开始时间
 * @param to 结束时间，为空时为开始时间当月
 * @param year 是否为全年
 */
data class DateData(
    val from: LocalDate,
    val to: LocalDate? = null,
    val year: Boolean = false,
) {
    val titleText: String
        get() = when {
            year -> {
                from.year.toString()
            }

            to != null -> {
                "${from.year}-${from.monthValue.completeZero()}-${from.dayOfMonth.completeZero()}\n" +
                    "${to.year}-${to.monthValue.completeZero()}-${to.dayOfMonth.completeZero()}"
            }

            else -> {
                "${from.year}-${from.monthValue.completeZero()}"
            }
        }

    val dateStr: String
        get() = titleText.replace("\n", "~")

    val crossYear: Boolean
        get() = to != null && from.year != to.year
}

data class ShowSheetData(
    val typeId: Long,
    val typeName: String,
    val dataList: List<AnalyticsRecordPieEntity>,
)

sealed class ShowSelectDateDialogData(open val date: DateData) {
    data class SelectDate(override val date: DateData) : ShowSelectDateDialogData(date)
    data class SelectRangeDate(override val date: DateData) : ShowSelectDateDialogData(date)
}
