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
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.ext.toMoneyString
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_DATE
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.entity.RecordDayEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.entity.normalizeMonthStartDay
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordViewSummaryModel
import cn.wj.android.cashbook.core.model.transfer.asEntity
import cn.wj.android.cashbook.domain.usecase.RecordModelTransToViewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
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
                // final_amount 全为 Migration9To10 DEFAULT 0，首屏会显全 0 → 必须 gate（先迁移后置位，不可调换）
                recordRepository.migrateAfter9To10()
                _migrationCompleted.value = true
            } else {
                // 已迁移：立即放行首屏（finalAmount 为旧吸收模型值——被吸收支出=0/吸收者可负，首屏短暂显旧值）
                _migrationCompleted.value = true
                if (!tempKeys.finalAmountNetRecalcDone) {
                    // 老用户净自付重算后台静默跑；完成后 recalculateAllFinalAmount 内部 bump recordDataVersion，
                    // 汇总流（订阅 version）+ 列表（Room PagingSource 对 db_record UPDATE 自动 invalidate）刷新到净自付值。
                    // try/catch（M-1 节点2）：后台重算失败不连累已放行首屏——异常逃逸会触发全局
                    // UncaughtExceptionHandler 的 finishAllActivity()；finalAmountNetRecalcDone 未置位，下次启动幂等重试。
                    try {
                        recordRepository.recalculateAllFinalAmount()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        this@LauncherContentViewModel.logger()
                            .e(e, "background netRecalc failed, will retry next launch")
                    }
                }
                if (!tempKeys.imagesToFilesMigrated) {
                    // 图片 BLOB→文件 backfill 后台静默跑；逐行幂等、崩溃可重入，
                    // backfillImagesToFiles 内部成功后置位 imagesToFilesMigrated。
                    // try/catch：失败不连累已放行首屏，标志未置位则下次启动幂等重试。
                    try {
                        recordRepository.backfillImagesToFiles()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        this@LauncherContentViewModel.logger()
                            .e(e, "background image backfill failed, will retry next launch")
                    }
                }
            }
            // 启动孤儿图片扫描（每次启动兜底：批量删账本/资产、编辑替换可能留孤儿文件）；
            // grace window 保护刚写入/backfill 在途文件，未 backfill 行无 record_images 文件故不误删。
            try {
                recordRepository.cleanupOrphanImageFiles()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                this@LauncherContentViewModel.logger().e(e, "orphan image cleanup failed")
            }
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

    /** 月起始日（响应式：改设置后各周期流自动重算）。归一化到 1..28，默认 1=自然月 */
    private val _monthStartDay = settingRepository.recordSettingsModel
        .map { normalizeMonthStartDay(it.monthStartDay) }
        .distinctUntilChanged()

    init {
        // 初始化为可配置月周期的当前周期（D=1 等价 ByMonth(YearMonth.now())，无回归）。
        // 置于此处（_dateSelection 声明之后）：Unconfined 下构造时 init 立即执行，须保证 _dateSelection 已初始化。
        viewModelScope.launch {
            val monthStartDay = settingRepository.recordSettingsModel.first().monthStartDay
            _dateSelection.value = DateSelectionEntity.currentMonthPeriod(LocalDate.now(), monthStartDay)
        }
    }

    /** 轻量汇总数据（用于计算收支总额和每日汇总） */
    private val _summaryData: Flow<List<RecordViewSummaryModel>> =
        combine(_dateSelection, _monthStartDay) { selection, d -> selection.toDateRange(d) }
            .flatMapLatest { (startDate, endDate) ->
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
    val recordPagingData: Flow<PagingData<LauncherListItem>> =
        combine(_dateSelection, _monthStartDay) { selection, d -> selection.toDateRange(d) }
            .flatMapLatest { (startDate, endDate) ->
                recordRepository.getRecordPagingData(startDate, endDate)
                    .map { pagingData ->
                        pagingData.map { recordModel ->
                            val views = recordModelTransToViewsUseCase(recordModel).asEntity()
                            LauncherListItem.Record(views) as LauncherListItem
                        }
                    }
                    .map { pagingData ->
                        pagingData.insertSeparators { before, after ->
                            recordDaySeparator(before, after)
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
                if (record.isBalanceRecord) {
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
                if (record.isBalanceRecord) {
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
