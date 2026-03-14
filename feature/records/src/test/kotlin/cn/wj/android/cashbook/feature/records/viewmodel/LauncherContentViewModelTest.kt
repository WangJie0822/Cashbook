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
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordViewSummaryModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeBooksRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeSettingRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.domain.usecase.RecordModelTransToViewsUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.YearMonth

class LauncherContentViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var booksRepository: FakeBooksRepository
    private lateinit var settingRepository: FakeSettingRepository
    private lateinit var viewModel: LauncherContentViewModel

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        booksRepository = FakeBooksRepository()
        settingRepository = FakeSettingRepository()

        val typeRepository = FakeTypeRepository()
        val assetRepository = FakeAssetRepository()
        val tagRepository = FakeTagRepository()

        val recordModelTransToViewsUseCase = RecordModelTransToViewsUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            assetRepository = assetRepository,
            tagRepository = tagRepository,
            coroutineContext = dispatcherRule.testDispatcher,
        )

        viewModel = LauncherContentViewModel(
            booksRepository = booksRepository,
            settingRepository = settingRepository,
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = recordModelTransToViewsUseCase,
        )
    }

    @Test
    fun when_initialized_then_uiState_is_loading() {
        // 初始状态应为 Loading（在迁移完成前）
        // 注意：由于使用 UnconfinedTestDispatcher，init 块会立即执行，
        // 迁移完成后状态会变为 Success
        assertThat(viewModel.uiState.value).isInstanceOf(LauncherContentUiState::class.java)
    }

    @Test
    fun when_migration_completed_then_uiState_is_success() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(LauncherContentUiState.Success::class.java)

        collectJob.cancel()
    }

    @Test
    fun when_migration_completed_with_no_records_then_totals_are_zero() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as LauncherContentUiState.Success
        assertThat(state.totalIncome).isEqualTo("0.00")
        assertThat(state.totalExpand).isEqualTo("0.00")
        assertThat(state.totalBalance).isEqualTo("0.00")

        collectJob.cancel()
    }

    @Test
    fun when_has_expenditure_records_then_totals_calculated_correctly() = runTest {
        // 设置汇总数据：一条支出 100 元
        recordRepository.setSummaryData(
            listOf(
                RecordViewSummaryModel(
                    id = 1L,
                    typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
                    typeName = "餐饮",
                    amount = 10000L,
                    finalAmount = 10000L,
                    charges = 0L,
                    concessions = 0L,
                    recordTime = 1704067200000L,
                ),
            ),
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as LauncherContentUiState.Success
        assertThat(state.totalExpand).isEqualTo("100.00")
        assertThat(state.totalIncome).isEqualTo("0.00")
        assertThat(state.totalBalance).isEqualTo("-100.00")

        collectJob.cancel()
    }

    @Test
    fun when_has_income_records_then_totals_calculated_correctly() = runTest {
        // 设置汇总数据：一条收入 200 元
        recordRepository.setSummaryData(
            listOf(
                RecordViewSummaryModel(
                    id = 1L,
                    typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
                    typeName = "工资",
                    amount = 20000L,
                    finalAmount = 20000L,
                    charges = 0L,
                    concessions = 0L,
                    recordTime = 1704067200000L,
                ),
            ),
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as LauncherContentUiState.Success
        assertThat(state.totalIncome).isEqualTo("200.00")
        assertThat(state.totalExpand).isEqualTo("0.00")
        assertThat(state.totalBalance).isEqualTo("200.00")

        collectJob.cancel()
    }

    @Test
    fun when_has_mixed_records_then_balance_calculated_correctly() = runTest {
        // 设置汇总数据：收入 500 元 + 支出 300 元
        recordRepository.setSummaryData(
            listOf(
                RecordViewSummaryModel(
                    id = 1L,
                    typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
                    typeName = "工资",
                    amount = 50000L,
                    finalAmount = 50000L,
                    charges = 0L,
                    concessions = 0L,
                    recordTime = 1704067200000L,
                ),
                RecordViewSummaryModel(
                    id = 2L,
                    typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
                    typeName = "餐饮",
                    amount = 30000L,
                    finalAmount = 30000L,
                    charges = 0L,
                    concessions = 0L,
                    recordTime = 1704067200000L,
                ),
            ),
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as LauncherContentUiState.Success
        assertThat(state.totalIncome).isEqualTo("500.00")
        assertThat(state.totalExpand).isEqualTo("300.00")
        assertThat(state.totalBalance).isEqualTo("200.00")

        collectJob.cancel()
    }

    @Test
    fun when_has_transfer_records_then_charges_counted_as_expenditure() = runTest {
        // 转账手续费 5 元，优惠 2 元，净支出 = 5 - 2 = 3 元
        recordRepository.setSummaryData(
            listOf(
                RecordViewSummaryModel(
                    id = 1L,
                    typeCategory = RecordTypeCategoryEnum.TRANSFER.ordinal,
                    typeName = "转账",
                    amount = 10000L,
                    finalAmount = 10000L,
                    charges = 500L,
                    concessions = 200L,
                    recordTime = 1704067200000L,
                ),
            ),
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as LauncherContentUiState.Success
        assertThat(state.totalExpand).isEqualTo("3.00")
        assertThat(state.totalBalance).isEqualTo("-3.00")

        collectJob.cancel()
    }

    @Test
    fun when_has_balance_expenditure_record_then_skipped_in_totals() = runTest {
        // 平账记录应被跳过，RECORD_TYPE_BALANCE_EXPENDITURE.name 为 "平账"
        recordRepository.setSummaryData(
            listOf(
                RecordViewSummaryModel(
                    id = 1L,
                    typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
                    typeName = "平账",
                    amount = 10000L,
                    finalAmount = 10000L,
                    charges = 0L,
                    concessions = 0L,
                    recordTime = 1704067200000L,
                ),
            ),
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as LauncherContentUiState.Success
        assertThat(state.totalExpand).isEqualTo("0.00")
        assertThat(state.totalIncome).isEqualTo("0.00")

        collectJob.cancel()
    }

    @Test
    fun when_displayRecordDetailsSheet_then_viewRecord_is_set() {
        val record = createTestRecordViewsEntity(id = 1L)

        viewModel.displayRecordDetailsSheet(record)

        assertThat(viewModel.viewRecord).isEqualTo(record)
    }

    @Test
    fun when_dismissSheet_then_viewRecord_is_null() {
        val record = createTestRecordViewsEntity(id = 1L)
        viewModel.displayRecordDetailsSheet(record)
        assertThat(viewModel.viewRecord).isNotNull()

        viewModel.dismissSheet()

        assertThat(viewModel.viewRecord).isNull()
    }

    @Test
    fun when_initialized_then_viewRecord_is_null() {
        assertThat(viewModel.viewRecord).isNull()
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
    fun when_initialized_then_showDatePopup_is_false() {
        assertThat(viewModel.showDatePopup).isFalse()
    }

    @Test
    fun when_initialized_then_dateSelection_is_current_month() {
        val selection = viewModel.dateSelection.value
        assertThat(selection).isInstanceOf(DateSelectionEntity.ByMonth::class.java)
        val byMonth = selection as DateSelectionEntity.ByMonth
        assertThat(byMonth.yearMonth).isEqualTo(YearMonth.now())
    }

    @Test
    fun when_updateDateSelection_then_dateSelection_updated() {
        val newSelection = DateSelectionEntity.ByYear(2024)

        viewModel.updateDateSelection(newSelection)

        assertThat(viewModel.dateSelection.value).isEqualTo(newSelection)
    }

    @Test
    fun when_updateDateSelection_to_all_then_dateSelection_is_all() {
        viewModel.updateDateSelection(DateSelectionEntity.All)

        assertThat(viewModel.dateSelection.value).isEqualTo(DateSelectionEntity.All)
    }

    @Test
    fun when_initialized_then_shouldDisplayDeleteFailedBookmark_is_zero() {
        assertThat(viewModel.shouldDisplayDeleteFailedBookmark).isEqualTo(0)
    }

    @Test
    fun when_dismissBookmark_then_shouldDisplayDeleteFailedBookmark_is_zero() {
        viewModel.dismissBookmark()

        assertThat(viewModel.shouldDisplayDeleteFailedBookmark).isEqualTo(0)
    }

    @Test
    fun when_initialized_then_dailySummaries_is_empty() {
        assertThat(viewModel.dailySummaries.value).isEmpty()
    }

    @Test
    fun when_has_records_then_dailySummaries_computed() = runTest {
        // 设置同一天的两条记录
        val recordTime = 1704067200000L // 2024-01-01
        recordRepository.setSummaryData(
            listOf(
                RecordViewSummaryModel(
                    id = 1L,
                    typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
                    typeName = "餐饮",
                    amount = 5000L,
                    finalAmount = 5000L,
                    charges = 0L,
                    concessions = 0L,
                    recordTime = recordTime,
                ),
                RecordViewSummaryModel(
                    id = 2L,
                    typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
                    typeName = "工资",
                    amount = 10000L,
                    finalAmount = 10000L,
                    charges = 0L,
                    concessions = 0L,
                    recordTime = recordTime,
                ),
            ),
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.dailySummaries.collect()
        }

        val summaries = viewModel.dailySummaries.value
        assertThat(summaries).isNotEmpty()
        // 验证包含日期键
        val entry = summaries.values.first()
        assertThat(entry.dayIncome).isEqualTo(10000L)
        assertThat(entry.dayExpand).isEqualTo(5000L)

        collectJob.cancel()
    }

    /** 创建测试用的 RecordViewsEntity */
    private fun createTestRecordViewsEntity(id: Long): RecordViewsEntity {
        return RecordViewsEntity(
            id = id,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            typeName = "餐饮",
            typeIconResName = "vector_eating",
            assetId = null,
            assetName = null,
            assetIconResId = null,
            relatedAssetId = null,
            relatedAssetName = null,
            relatedAssetIconResId = null,
            amount = 10000L,
            finalAmount = 10000L,
            charges = 0L,
            concessions = 0L,
            remark = "测试备注",
            reimbursable = false,
            relatedTags = emptyList(),
            relatedImage = emptyList(),
            relatedRecord = emptyList(),
            relatedAmount = 0L,
            recordTime = 1704067200000L,
        )
    }
}
