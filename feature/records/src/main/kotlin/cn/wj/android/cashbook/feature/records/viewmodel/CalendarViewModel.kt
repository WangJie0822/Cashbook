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
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.common.ext.yearMonth
import cn.wj.android.cashbook.core.model.entity.RecordDayEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.domain.usecase.GetCurrentMonthRecordViewsMapUseCase
import cn.wj.android.cashbook.domain.usecase.GetCurrentMonthRecordViewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

/**
 * 日历 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/4
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    getCurrentMonthRecordViewsUseCase: GetCurrentMonthRecordViewsUseCase,
    getCurrentMonthRecordViewsMapUseCase: GetCurrentMonthRecordViewsMapUseCase,
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
            dialogState = DialogState.Shown(_dateData.first().yearMonth)
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
        val recordList: List<RecordViewsEntity>,
    ) : CalendarUiState
}
