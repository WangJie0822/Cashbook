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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import cn.wj.android.cashbook.core.common.ext.toMoneyString
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_DATE
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.entity.RecordDayEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RecordViewSummaryModel
import cn.wj.android.cashbook.core.model.transfer.asEntity
import cn.wj.android.cashbook.domain.usecase.RecordModelTransToViewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class LauncherContentViewModel @Inject constructor(
    booksRepository: BooksRepository,
    settingRepository: SettingRepository,
    private val recordRepository: RecordRepository,
    private val recordModelTransToViewsUseCase: RecordModelTransToViewsUseCase,
) : ViewModel() {

    /** 标记 - 数据迁移是否已完成 */
    private val _migrationCompleted = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val tempKeys = settingRepository.tempKeysModel.first()
            if (!tempKeys.db9To10DataMigrated) {
                recordRepository.migrateAfter9To10()
            }
            _migrationCompleted.value = true
        }
    }

    /** 删除记录失败错误信息 */
    var shouldDisplayDeleteFailedBookmark by mutableIntStateOf(0)
        private set

    /** 记录详情数据 */
    var viewRecord by mutableStateOf<RecordViewsEntity?>(null)
        private set

    /** 日期选择 Popup 是否显示 */
    var showDatePopup by mutableStateOf(false)
        private set

    /** 当前日期选择 */
    private val _dateSelection = MutableStateFlow<DateSelectionEntity>(
        DateSelectionEntity.ByMonth(YearMonth.now()),
    )
    val dateSelection: StateFlow<DateSelectionEntity> = _dateSelection

    /** 轻量汇总数据（用于计算收支总额和每日汇总） */
    private val _summaryData: Flow<List<RecordViewSummaryModel>> = _dateSelection.flatMapLatest { selection ->
        val (startDate, endDate) = selection.toDateRange()
        recordRepository.queryRecordViewSummariesFlow(startDate, endDate)
    }

    /** 每日汇总 Map（日期字符串 -> RecordDayEntity）*/
    val dailySummaries: StateFlow<Map<String, RecordDayEntity>> = _summaryData
        .map { list -> computeDailySummaries(list) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    /** 分页记录数据 */
    val recordPagingData: Flow<PagingData<LauncherListItem>> = _dateSelection.flatMapLatest { selection ->
        val (startDate, endDate) = selection.toDateRange()
        recordRepository.getRecordPagingData(startDate, endDate)
            .map { pagingData ->
                pagingData.map { recordModel ->
                    val views = recordModelTransToViewsUseCase(recordModel).asEntity()
                    LauncherListItem.Record(views) as LauncherListItem
                }
            }
            .map { pagingData ->
                pagingData.insertSeparators { before, after ->
                    val afterRecord = (after as? LauncherListItem.Record)?.entity
                    val beforeRecord = (before as? LauncherListItem.Record)?.entity
                    if (afterRecord != null) {
                        val afterDate = afterRecord.recordTime.dateFormat(DATE_FORMAT_DATE)
                        val beforeDate = beforeRecord?.recordTime?.dateFormat(DATE_FORMAT_DATE)
                        if (afterDate != beforeDate) {
                            val dateArray = afterDate.split("-")
                            val day = dateArray.last().toIntOrNull() ?: 0
                            val dayType = computeDayType(dateArray)
                            LauncherListItem.DayHeader(
                                dateStr = afterDate,
                                day = day,
                                dayType = dayType,
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            }
    }.cachedIn(viewModelScope)

    /** 界面 UI 状态 */
    val uiState: StateFlow<LauncherContentUiState> = combine(
        _summaryData,
        booksRepository.currentBook,
        _migrationCompleted,
    ) { summaryList, book, migrationCompleted ->
        if (!migrationCompleted) {
            LauncherContentUiState.Loading
        } else {
            var totalIncome = 0L
            var totalExpenditure = 0L
            summaryList.forEach { record ->
                if (record.typeName == RECORD_TYPE_BALANCE_EXPENDITURE.name) {
                    return@forEach
                }
                when (RecordTypeCategoryEnum.ordinalOf(record.typeCategory)) {
                    RecordTypeCategoryEnum.EXPENDITURE -> {
                        totalExpenditure += record.finalAmount
                    }

                    RecordTypeCategoryEnum.INCOME -> {
                        totalIncome += record.finalAmount
                    }

                    RecordTypeCategoryEnum.TRANSFER -> {
                        totalExpenditure += record.charges - record.concessions
                    }
                }
            }
            LauncherContentUiState.Success(
                topBgUri = book.bgUri,
                totalIncome = totalIncome.toMoneyString(),
                totalExpand = totalExpenditure.toMoneyString(),
                totalBalance = (totalIncome - totalExpenditure).toMoneyString(),
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LauncherContentUiState.Loading,
        )

    /** 隐藏提示 */
    fun dismissBookmark() {
        shouldDisplayDeleteFailedBookmark = 0
    }

    /** 显示记录 [record] 详情 sheet */
    fun displayRecordDetailsSheet(record: RecordViewsEntity) {
        viewRecord = record
    }

    /** 隐藏 sheet */
    fun dismissSheet() {
        viewRecord = null
    }

    /** 显示日期选择 Popup */
    fun displayDatePopup() {
        showDatePopup = true
    }

    /** 隐藏日期选择 Popup */
    fun dismissDatePopup() {
        showDatePopup = false
    }

    /** 更新日期选择 */
    fun updateDateSelection(selection: DateSelectionEntity) {
        _dateSelection.tryEmit(selection)
    }

    /** 计算每日汇总 */
    private fun computeDailySummaries(
        list: List<RecordViewSummaryModel>,
    ): Map<String, RecordDayEntity> {
        val dayMap = linkedMapOf<String, MutableList<RecordViewSummaryModel>>()
        list.sortedByDescending { it.recordTime }
            .forEach { record ->
                val dateStr = record.recordTime.dateFormat(DATE_FORMAT_DATE)
                dayMap.getOrPut(dateStr) { mutableListOf() }.add(record)
            }
        return dayMap.mapValues { (dateStr, records) ->
            var dayIncome = 0L
            var dayExpenditure = 0L
            records.forEach { record ->
                if (record.typeName == RECORD_TYPE_BALANCE_EXPENDITURE.name) {
                    return@forEach
                }
                when (RecordTypeCategoryEnum.ordinalOf(record.typeCategory)) {
                    RecordTypeCategoryEnum.EXPENDITURE -> {
                        dayExpenditure += record.finalAmount
                    }

                    RecordTypeCategoryEnum.INCOME -> {
                        dayIncome += record.finalAmount
                    }

                    RecordTypeCategoryEnum.TRANSFER -> {
                        dayExpenditure += record.charges - record.concessions
                    }
                }
            }
            val dateArray = dateStr.split("-")
            val day = dateArray.last().toIntOrNull() ?: 0
            val dayType = computeDayType(dateArray)
            RecordDayEntity(
                day = day,
                dayType = dayType,
                dayIncome = dayIncome,
                dayExpand = dayExpenditure,
            )
        }
    }

    /** 计算 dayType：0=今天, -1=昨天, -2=前天, 1=其它 */
    private fun computeDayType(dateArray: List<String>): Int {
        val calendar = Calendar.getInstance()
        val currentYear = calendar[Calendar.YEAR]
        val currentMonth = calendar[Calendar.MONTH] + 1
        val currentDay = calendar[Calendar.DAY_OF_MONTH]
        val dateDay = dateArray.last().toIntOrNull() ?: return 1
        return if (currentYear == dateArray[0].toIntOrNull() && currentMonth == dateArray[1].toIntOrNull()) {
            when (dateDay) {
                currentDay -> 0
                currentDay - 1 -> -1
                currentDay - 2 -> -2
                else -> 1
            }
        } else {
            1
        }
    }
}

/** 首页列表项密封接口 */
sealed interface LauncherListItem {
    /** 日期头 */
    data class DayHeader(
        val dateStr: String,
        val day: Int,
        val dayType: Int,
    ) : LauncherListItem

    /** 记录项 */
    data class Record(val entity: RecordViewsEntity) : LauncherListItem
}

/**
 * 界面 UI 状态
 */
sealed interface LauncherContentUiState {
    /** 加载中 */
    data object Loading : LauncherContentUiState

    /**
     * 加载完成
     *
     * @param totalIncome 收入
     * @param totalExpand 支出
     * @param totalBalance 结余
     */
    data class Success(
        val topBgUri: String,
        val totalIncome: String,
        val totalExpand: String,
        val totalBalance: String,
    ) : LauncherContentUiState
}
