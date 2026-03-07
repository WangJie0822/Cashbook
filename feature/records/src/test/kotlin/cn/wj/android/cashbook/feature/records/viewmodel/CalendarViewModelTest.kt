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

import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.domain.usecase.GetCurrentMonthRecordViewsMapUseCase
import cn.wj.android.cashbook.domain.usecase.GetCurrentMonthRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.RecordModelTransToViewsUseCase
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.YearMonth

/**
 * CalendarViewModel 的单元测试
 *
 * GetCurrentMonthRecordViewsMapUseCase 内部使用了 ArrayMap（Android 类），
 * 因此需要使用 Robolectric 提供 Android 环境。
 */
@RunWith(RobolectricTestRunner::class)
class CalendarViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setup() {
        val recordRepository = FakeRecordRepository()
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
        val getCurrentMonthRecordViewsUseCase = GetCurrentMonthRecordViewsUseCase(
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = recordModelTransToViewsUseCase,
        )
        val getCurrentMonthRecordViewsMapUseCase = GetCurrentMonthRecordViewsMapUseCase()

        viewModel = CalendarViewModel(
            getCurrentMonthRecordViewsUseCase = getCurrentMonthRecordViewsUseCase,
            getCurrentMonthRecordViewsMapUseCase = getCurrentMonthRecordViewsMapUseCase,
        )
    }

    @Test
    fun when_show_date_select_dialog_then_dialog_shown() {
        // 显示日期选择弹窗
        viewModel.showDateSelectDialog()

        // 验证弹窗状态为显示
        val state = viewModel.dialogState
        assertThat(state).isInstanceOf(DialogState.Shown::class.java)
        // 弹窗数据应为当前月的 YearMonth
        val shownData = (state as DialogState.Shown<*>).data
        assertThat(shownData).isInstanceOf(YearMonth::class.java)
    }

    @Test
    fun when_on_date_selected_then_dialog_dismissed() {
        // 先显示弹窗
        viewModel.showDateSelectDialog()
        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)

        // 选择一个日期
        val selectedDate = LocalDate.of(2024, 6, 15)
        viewModel.onDateSelected(selectedDate)

        // 验证弹窗已关闭
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
        // 验证日期已更新
        assertThat(viewModel.dateData.value).isEqualTo(selectedDate)
    }

    @Test
    fun when_on_record_item_click_then_view_record_set() {
        val record = createTestRecordViewsEntity(id = 1L)

        // 点击记录
        viewModel.onRecordItemClick(record)

        // 验证 viewRecord 已设置
        assertThat(viewModel.viewRecord).isEqualTo(record)
    }

    @Test
    fun when_on_sheet_dismiss_then_view_record_null() {
        val record = createTestRecordViewsEntity(id = 1L)

        // 先设置记录
        viewModel.onRecordItemClick(record)
        assertThat(viewModel.viewRecord).isNotNull()

        // 关闭 sheet
        viewModel.onSheetDismiss()

        // 验证 viewRecord 已置空
        assertThat(viewModel.viewRecord).isNull()
    }

    @Test
    fun when_on_dialog_dismiss_then_dialog_dismissed() {
        // 先显示弹窗
        viewModel.showDateSelectDialog()
        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)

        // 关闭弹窗
        viewModel.onDialogDismiss()

        // 验证弹窗状态为关闭
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_on_bookmark_dismiss_then_bookmark_zero() {
        // 关闭书签提示
        viewModel.onBookmarkDismiss()

        // 验证 shouldDisplayDeleteFailedBookmark 为 0
        assertThat(viewModel.shouldDisplayDeleteFailedBookmark).isEqualTo(0)
    }

    @Test
    fun when_initialized_then_dialog_state_is_dismiss() {
        // 初始状态弹窗应为关闭
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_initialized_then_view_record_is_null() {
        // 初始状态 viewRecord 应为 null
        assertThat(viewModel.viewRecord).isNull()
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
            amount = "100.00",
            finalAmount = "100.00",
            charges = "0",
            concessions = "0",
            remark = "测试备注",
            reimbursable = false,
            relatedTags = emptyList(),
            relatedImage = emptyList(),
            relatedRecord = emptyList(),
            relatedAmount = "0",
            recordTime = "2024-01-01 12:00",
        )
    }
}
