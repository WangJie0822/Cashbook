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

import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.enums.AnalyticsBarGranularity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.domain.usecase.GetRecordViewsBetweenDateUseCase
import cn.wj.android.cashbook.domain.usecase.RecordModelTransToViewsUseCase
import cn.wj.android.cashbook.domain.usecase.TransRecordViewsToAnalyticsBarUseCase
import cn.wj.android.cashbook.domain.usecase.TransRecordViewsToAnalyticsPieSecondUseCase
import cn.wj.android.cashbook.domain.usecase.TransRecordViewsToAnalyticsPieUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.YearMonth

class AnalyticsViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var viewModel: AnalyticsViewModel

    @Before
    fun setup() {
        typeRepository = FakeTypeRepository()
        recordRepository = FakeRecordRepository()
        val assetRepository = FakeAssetRepository()
        val tagRepository = FakeTagRepository()

        val recordModelTransToViewsUseCase = RecordModelTransToViewsUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            assetRepository = assetRepository,
            tagRepository = tagRepository,
            coroutineContext = dispatcherRule.testDispatcher,
        )

        val getRecordViewsBetweenDateUseCase = GetRecordViewsBetweenDateUseCase(
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = recordModelTransToViewsUseCase,
            coroutineContext = dispatcherRule.testDispatcher,
        )

        val transRecordViewsToAnalyticsBarUseCase = TransRecordViewsToAnalyticsBarUseCase(
            coroutineContext = dispatcherRule.testDispatcher,
        )

        val transRecordViewsToAnalyticsPieUseCase = TransRecordViewsToAnalyticsPieUseCase(
            typeRepository = typeRepository,
            coroutineContext = dispatcherRule.testDispatcher,
        )

        val transRecordViewsToAnalyticsPieSecondUseCase = TransRecordViewsToAnalyticsPieSecondUseCase(
            coroutineContext = dispatcherRule.testDispatcher,
        )

        viewModel = AnalyticsViewModel(
            typeRepository = typeRepository,
            getRecordViewsBetweenDateUseCase = getRecordViewsBetweenDateUseCase,
            transRecordViewsToAnalyticsBarUseCase = transRecordViewsToAnalyticsBarUseCase,
            transRecordViewsToAnalyticsPieUseCase = transRecordViewsToAnalyticsPieUseCase,
            transRecordViewsToAnalyticsPieSecondUseCase = transRecordViewsToAnalyticsPieSecondUseCase,
        )
    }

    @Test
    fun when_initialized_then_uiState_is_loading() {
        assertThat(viewModel.uiState.value).isEqualTo(AnalyticsUiState.Loading)
    }

    @Test
    fun when_initialized_then_dateSelection_is_current_month() {
        val selection = viewModel.dateSelection.value
        assertThat(selection).isInstanceOf(DateSelectionEntity.ByMonth::class.java)
        val byMonth = selection as DateSelectionEntity.ByMonth
        assertThat(byMonth.yearMonth).isEqualTo(YearMonth.now())
    }

    @Test
    fun when_initialized_then_showDatePopup_is_false() {
        assertThat(viewModel.showDatePopup).isFalse()
    }

    @Test
    fun when_initialized_then_sheetData_is_null() {
        assertThat(viewModel.sheetData).isNull()
    }

    @Test
    fun when_displayDatePopup_then_showDatePopup_is_true() {
        viewModel.displayDatePopup()

        assertThat(viewModel.showDatePopup).isTrue()
    }

    @Test
    fun when_dismissDatePopup_then_showDatePopup_is_false() {
        viewModel.displayDatePopup()
        assertThat(viewModel.showDatePopup).isTrue()

        viewModel.dismissDatePopup()

        assertThat(viewModel.showDatePopup).isFalse()
    }

    @Test
    fun when_updateDateSelection_to_year_then_dateSelection_updates() {
        val yearSelection = DateSelectionEntity.ByYear(2024)

        viewModel.updateDateSelection(yearSelection)

        assertThat(viewModel.dateSelection.value).isEqualTo(yearSelection)
    }

    @Test
    fun when_updateDateSelection_to_all_then_dateSelection_updates() {
        viewModel.updateDateSelection(DateSelectionEntity.All)

        assertThat(viewModel.dateSelection.value).isEqualTo(DateSelectionEntity.All)
    }

    @Test
    fun when_dismissSheet_then_sheetData_is_null() {
        viewModel.dismissSheet()

        assertThat(viewModel.sheetData).isNull()
    }

    @Test
    fun when_no_records_then_uiState_shows_noData() = runTest {
        // 收集 uiState 以激活 WhileSubscribed 上游
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AnalyticsUiState.Success::class.java)
        val success = state as AnalyticsUiState.Success
        assertThat(success.noData).isTrue()
    }

    @Test
    fun when_no_records_then_totals_are_zero() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value as AnalyticsUiState.Success
        assertThat(state.totalIncome).isEqualTo("0.00")
        assertThat(state.totalExpenditure).isEqualTo("0.00")
        assertThat(state.totalBalance).isEqualTo("0.00")
    }

    @Test
    fun when_has_records_then_uiState_shows_data() = runTest {
        // 准备类型数据
        val expenditureType = createRecordTypeModel(
            id = 1L,
            name = "餐饮",
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )
        typeRepository.addType(expenditureType)

        // 添加支出记录
        recordRepository.addRecord(
            createRecordModel(
                id = 1L,
                typeId = 1L,
                amount = 5000L,
                finalAmount = 5000L,
                recordTime = 1704067200000L, // 2024-01-01
            ),
        )

        // 切换到 2024-01 月份
        viewModel.updateDateSelection(DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AnalyticsUiState.Success::class.java)
        val success = state as AnalyticsUiState.Success
        assertThat(success.noData).isFalse()
    }

    @Test
    fun when_dateSelection_is_byMonth_then_granularity_is_day() = runTest {
        viewModel.updateDateSelection(DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value as AnalyticsUiState.Success
        assertThat(state.granularity).isEqualTo(AnalyticsBarGranularity.DAY)
    }

    @Test
    fun when_dateSelection_is_byYear_then_granularity_is_month() = runTest {
        viewModel.updateDateSelection(DateSelectionEntity.ByYear(2024))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value as AnalyticsUiState.Success
        assertThat(state.granularity).isEqualTo(AnalyticsBarGranularity.MONTH)
    }

    @Test
    fun when_dateSelection_is_all_then_granularity_is_year() = runTest {
        viewModel.updateDateSelection(DateSelectionEntity.All)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value as AnalyticsUiState.Success
        assertThat(state.granularity).isEqualTo(AnalyticsBarGranularity.YEAR)
    }

    @Test
    fun when_dateSelection_is_byMonth_then_titleText_shows_yearMonth() = runTest {
        viewModel.updateDateSelection(DateSelectionEntity.ByMonth(YearMonth.of(2024, 6)))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value as AnalyticsUiState.Success
        assertThat(state.titleText).isEqualTo("2024-06")
    }

    @Test
    fun when_dateSelection_is_byYear_then_titleText_shows_year() = runTest {
        viewModel.updateDateSelection(DateSelectionEntity.ByYear(2024))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value as AnalyticsUiState.Success
        assertThat(state.titleText).isEqualTo("2024")
    }

    @Test
    fun when_dateSelection_is_all_then_titleText_shows_all() = runTest {
        viewModel.updateDateSelection(DateSelectionEntity.All)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value as AnalyticsUiState.Success
        assertThat(state.titleText).isEqualTo("全部")
    }

    @Test
    fun when_dateSelection_changes_then_uiState_updates() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 初始状态为当前月份
        val initialState = viewModel.uiState.value as AnalyticsUiState.Success
        val initialTitle = initialState.titleText

        // 切换到 2023 年
        viewModel.updateDateSelection(DateSelectionEntity.ByYear(2023))

        val updatedState = viewModel.uiState.value as AnalyticsUiState.Success
        assertThat(updatedState.titleText).isEqualTo("2023")
        assertThat(updatedState.titleText).isNotEqualTo(initialTitle)
    }

    @Test
    fun when_has_expenditure_records_then_pie_data_not_empty() = runTest {
        val expenditureType = createRecordTypeModel(
            id = 1L,
            name = "餐饮",
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )
        typeRepository.addType(expenditureType)

        recordRepository.addRecord(
            createRecordModel(
                id = 1L,
                typeId = 1L,
                amount = 5000L,
                finalAmount = 5000L,
                recordTime = 1704067200000L,
            ),
        )

        viewModel.updateDateSelection(DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value as AnalyticsUiState.Success
        assertThat(state.expenditurePieDataList).isNotEmpty()
        assertThat(state.incomePieDataList).isEmpty()
    }

    @Test
    fun when_has_income_records_then_income_pie_data_not_empty() = runTest {
        val incomeType = createRecordTypeModel(
            id = 2L,
            name = "工资",
            typeCategory = RecordTypeCategoryEnum.INCOME,
        )
        typeRepository.addType(incomeType)

        recordRepository.addRecord(
            createRecordModel(
                id = 1L,
                typeId = 2L,
                amount = 500000L,
                finalAmount = 500000L,
                recordTime = 1704067200000L,
            ),
        )

        viewModel.updateDateSelection(DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value as AnalyticsUiState.Success
        assertThat(state.incomePieDataList).isNotEmpty()
        assertThat(state.expenditurePieDataList).isEmpty()
    }
}
