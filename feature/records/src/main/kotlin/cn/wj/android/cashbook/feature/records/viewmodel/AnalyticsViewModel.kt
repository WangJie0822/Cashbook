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
import cn.wj.android.cashbook.core.common.ext.toMoneyString
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordBarEntity
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordPieEntity
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.enums.AnalyticsBarGranularity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.ProgressDialogController
import cn.wj.android.cashbook.core.ui.runCatchWithProgress
import cn.wj.android.cashbook.domain.usecase.GetRecordViewsBetweenDateUseCase
import cn.wj.android.cashbook.domain.usecase.TransRecordViewsToAnalyticsBarUseCase
import cn.wj.android.cashbook.domain.usecase.TransRecordViewsToAnalyticsPieSecondUseCase
import cn.wj.android.cashbook.domain.usecase.TransRecordViewsToAnalyticsPieUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
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

    /** 进度弹窗控制器 */
    private var progressDialogController: ProgressDialogController? = null

    /** 设置进度弹窗控制器 */
    fun setProgressDialogController(controller: ProgressDialogController) {
        progressDialogController = controller
    }

    var dialogState: DialogState by mutableStateOf(DialogState.Dismiss)
        private set

    var sheetData: ShowSheetData? by mutableStateOf(null)
        private set

    /** 日期选择 Popup 是否显示 */
    var showDatePopup by mutableStateOf(false)
        private set

    /** 当前日期选择 */
    private val _dateSelection = MutableStateFlow<DateSelectionEntity>(
        DateSelectionEntity.ByMonth(YearMonth.now()),
    )
    val dateSelection: StateFlow<DateSelectionEntity> = _dateSelection

    /** 将 dateSelection 和对应的记录数据绑定在一起，避免 combine 时新旧数据混用 */
    private val _selectionWithRecords = _dateSelection.mapLatest { selection ->
        selection to getRecordViewsBetweenDateUseCase(selection)
    }

    val uiState = _selectionWithRecords.mapLatest { (selection, list) ->
        var totalIncome = 0L
        var totalExpenditure = 0L
        var totalBalance = 0L
        val barList = transRecordViewsToAnalyticsBarUseCase(selection, list)
        barList.forEach {
            totalIncome += it.income
            totalExpenditure += it.expenditure
            totalBalance += it.balance
        }
        val expenditurePieDataList =
            transRecordViewsToAnalyticsPieUseCase(RecordTypeCategoryEnum.EXPENDITURE, list)
        val incomePieDataList =
            transRecordViewsToAnalyticsPieUseCase(RecordTypeCategoryEnum.INCOME, list)
        val transferPieDataList =
            transRecordViewsToAnalyticsPieUseCase(RecordTypeCategoryEnum.TRANSFER, list)
        val granularity = when (selection) {
            is DateSelectionEntity.ByYear -> AnalyticsBarGranularity.MONTH
            is DateSelectionEntity.All -> AnalyticsBarGranularity.YEAR
            else -> AnalyticsBarGranularity.DAY
        }
        val success = AnalyticsUiState.Success(
            granularity = granularity,
            titleText = selection.getDisplayText(),
            noData = list.isEmpty(),
            totalIncome = totalIncome.toMoneyString(),
            totalExpenditure = totalExpenditure.toMoneyString(),
            totalBalance = totalBalance.toMoneyString(),
            barDataList = barList,
            expenditurePieDataList = expenditurePieDataList,
            incomePieDataList = incomePieDataList,
            transferPieDataList = transferPieDataList,
        )
        progressDialogController?.dismiss()
        success
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AnalyticsUiState.Loading,
        )

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

    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }

    fun showSheet(controller: ProgressDialogController, typeId: Long) {
        viewModelScope.launch {
            runCatchWithProgress(controller) {
                val type = typeRepository.getRecordTypeById(typeId) ?: return@runCatchWithProgress
                val ls =
                    transRecordViewsToAnalyticsPieSecondUseCase(typeId, _selectionWithRecords.first().second)
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
        val granularity: AnalyticsBarGranularity,
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

data class ShowSheetData(
    val typeId: Long,
    val typeName: String,
    val dataList: List<AnalyticsRecordPieEntity>,
)
