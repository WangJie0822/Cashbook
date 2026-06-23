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
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeSettingRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.domain.usecase.GetTagRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.GetTypeRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.GetTypedMonthSummaryUseCase
import cn.wj.android.cashbook.domain.usecase.RecordModelTransToViewsUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.YearMonth

class TypedAnalyticsViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var tagRepository: FakeTagRepository
    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var settingRepository: FakeSettingRepository
    private lateinit var viewModel: TypedAnalyticsViewModel

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        settingRepository = FakeSettingRepository()
        typeRepository = FakeTypeRepository()
        val assetRepository = FakeAssetRepository()
        tagRepository = FakeTagRepository()

        val recordModelTransToViewsUseCase = RecordModelTransToViewsUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            assetRepository = assetRepository,
            tagRepository = tagRepository,
            coroutineContext = dispatcherRule.testDispatcher,
        )
        val getTypeRecordViewsUseCase = GetTypeRecordViewsUseCase(
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = recordModelTransToViewsUseCase,
            coroutineContext = dispatcherRule.testDispatcher,
        )
        val getTagRecordViewsUseCase = GetTagRecordViewsUseCase(
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = recordModelTransToViewsUseCase,
            coroutineContext = dispatcherRule.testDispatcher,
        )
        val getTypedMonthSummaryUseCase = GetTypedMonthSummaryUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            coroutineContext = dispatcherRule.testDispatcher,
        )

        viewModel = TypedAnalyticsViewModel(
            typeRepository = typeRepository,
            tagRepository = tagRepository,
            settingRepository = settingRepository,
            getTypeRecordViewsUseCase = getTypeRecordViewsUseCase,
            getTagRecordViewsUseCase = getTagRecordViewsUseCase,
            getTypedMonthSummaryUseCase = getTypedMonthSummaryUseCase,
        )
    }

    private fun expenditureType(id: Long, name: String) = RecordTypeModel(
        id = id, parentId = -1L, name = name, iconName = "vector_eating",
        typeLevel = TypeLevelEnum.FIRST, typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        protected = false, sort = 0, needRelated = false,
    )

    @Test
    fun when_initialized_then_viewRecord_is_null() {
        assertThat(viewModel.viewRecord).isNull()
    }

    @Test
    fun when_showRecordDetailsSheet_then_viewRecord_is_set() {
        val record = createTestRecordViewsEntity(id = 1L)
        viewModel.showRecordDetailsSheet(record)
        assertThat(viewModel.viewRecord).isEqualTo(record)
    }

    @Test
    fun when_dismissRecordDetailSheet_then_viewRecord_is_null() {
        val record = createTestRecordViewsEntity(id = 1L)
        viewModel.showRecordDetailsSheet(record)
        assertThat(viewModel.viewRecord).isNotNull()
        viewModel.dismissRecordDetailSheet()
        assertThat(viewModel.viewRecord).isNull()
    }

    @Test
    fun when_initialized_then_uiState_is_loading() {
        assertThat(viewModel.uiState.value).isEqualTo(TypedAnalyticsUiState.Loading)
    }

    @Test
    fun when_updateData_with_typeId_then_uiState_updates_to_success() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        typeRepository.addType(expenditureType(1L, "餐饮"))

        viewModel.updateData(tagId = -1L, typeId = 1L, date = "")

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TypedAnalyticsUiState.Success::class.java)
        val successState = state as TypedAnalyticsUiState.Success
        assertThat(successState.isType).isTrue()
        assertThat(successState.titleText).isEqualTo("餐饮")
        assertThat(successState.isTransferType).isFalse()
    }

    @Test
    fun when_updateData_with_tagId_then_uiState_updates_to_success() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        val tag = TagModel(id = 2L, name = "日常", invisible = false)
        tagRepository.addTag(tag)

        viewModel.updateData(tagId = 2L, typeId = -1L, date = "")

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TypedAnalyticsUiState.Success::class.java)
        val successState = state as TypedAnalyticsUiState.Success
        assertThat(successState.isType).isFalse()
        assertThat(successState.titleText).isEqualTo("日常")
    }

    @Test
    fun when_updateData_with_yearMonth_then_dateSelection_is_byMonth() = runTest {
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "2024-06")
        assertThat(viewModel.dateSelection.value)
            .isEqualTo(DateSelectionEntity.ByMonth(YearMonth.of(2024, 6)))
    }

    @Test
    fun when_updateData_with_year_then_dateSelection_is_byYear() = runTest {
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "2024")
        assertThat(viewModel.dateSelection.value).isEqualTo(DateSelectionEntity.ByYear(2024))
    }

    @Test
    fun when_updateData_with_empty_then_default_current_month() = runTest {
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "")
        assertThat(viewModel.dateSelection.value).isInstanceOf(DateSelectionEntity.ByMonth::class.java)
    }

    @Test
    fun when_updateMonth_then_dateSelection_updates() = runTest {
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "2024-06")
        viewModel.updateMonth(YearMonth.of(2024, 8))
        assertThat(viewModel.dateSelection.value)
            .isEqualTo(DateSelectionEntity.ByMonth(YearMonth.of(2024, 8)))
    }

    @Test
    fun when_updateData_repeated_with_same_args_after_updateMonth_then_month_preserved() = runTest {
        // 模拟 Route 重组导致的重复 updateData（相同入口参数）：翻月结果不应被重置
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "2024-06")
        viewModel.updateMonth(YearMonth.of(2024, 8))
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "2024-06")
        assertThat(viewModel.dateSelection.value)
            .isEqualTo(DateSelectionEntity.ByMonth(YearMonth.of(2024, 8)))
    }

    @Test
    fun when_transfer_type_then_isTransferType_true() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        typeRepository.addType(
            RecordTypeModel(
                id = 5L, parentId = -1L, name = "转账", iconName = "vector_transfer",
                typeLevel = TypeLevelEnum.FIRST, typeCategory = RecordTypeCategoryEnum.TRANSFER,
                protected = false, sort = 0, needRelated = false,
            ),
        )
        viewModel.updateData(tagId = -1L, typeId = 5L, date = "")
        val state = viewModel.uiState.value as TypedAnalyticsUiState.Success
        assertThat(state.isTransferType).isTrue()
    }

    @Test
    fun when_monthStartDay_15_then_summary_uses_period_includes_cross_month_record() = runTest {
        // C1/周期：D=15 时 ByMonth(2024-01) = [2024-01-15, 2024-02-15) 含 2024-02-03 记录；
        // 汇总走 toDateRange(D)（Fake queryRecordsByTypeIdInRange 忠实过滤区间）→ 支出=5000；
        // 列表路径毫秒区间正确性由 GetTypeRecordViewsUseCaseTest.when_millis_range_then_filters_half_open 覆盖
        typeRepository.addType(expenditureType(1L, "餐饮"))
        recordRepository.addRecord(
            createRecordModel(id = 1L, typeId = 1L, amount = 5000L, finalAmount = 5000L, recordTime = ms(2024, 2, 3)),
        )
        settingRepository.updateMonthStartDay(15)
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "2024-01")

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.summary.collect {}
        }

        assertThat(viewModel.summary.value.expenditure).isEqualTo(5000L)
    }

    private fun ms(y: Int, m: Int, d: Int): Long =
        java.time.LocalDate.of(y, m, d).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** 创建测试用的 RecordViewsEntity */
    private fun createTestRecordViewsEntity(id: Long): RecordViewsEntity {
        return RecordViewsEntity(
            id = id,
            typeId = 1L,
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
            recordTime = 1704110400000L,
        )
    }
}
