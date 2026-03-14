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
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.domain.usecase.GetTagRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.GetTypeRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.RecordModelTransToViewsUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TypedAnalyticsViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var tagRepository: FakeTagRepository
    private lateinit var viewModel: TypedAnalyticsViewModel

    @Before
    fun setup() {
        val recordRepository = FakeRecordRepository()
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

        viewModel = TypedAnalyticsViewModel(
            typeRepository = typeRepository,
            tagRepository = tagRepository,
            getTypeRecordViewsUseCase = getTypeRecordViewsUseCase,
            getTagRecordViewsUseCase = getTagRecordViewsUseCase,
        )
    }

    @Test
    fun when_initialized_then_viewRecord_is_null() {
        // 初始状态下 viewRecord 应为 null
        assertThat(viewModel.viewRecord).isNull()
    }

    @Test
    fun when_showRecordDetailsSheet_then_viewRecord_is_set() {
        val record = createTestRecordViewsEntity(id = 1L)

        // 显示记录详情
        viewModel.showRecordDetailsSheet(record)

        // 验证 viewRecord 已设置
        assertThat(viewModel.viewRecord).isEqualTo(record)
    }

    @Test
    fun when_dismissRecordDetailSheet_then_viewRecord_is_null() {
        val record = createTestRecordViewsEntity(id = 1L)

        // 先显示记录详情
        viewModel.showRecordDetailsSheet(record)
        assertThat(viewModel.viewRecord).isNotNull()

        // 隐藏记录详情
        viewModel.dismissRecordDetailSheet()

        // 验证 viewRecord 已置空
        assertThat(viewModel.viewRecord).isNull()
    }

    @Test
    fun when_initialized_then_uiState_is_loading() {
        // 初始状态下 uiState 应为 Loading
        assertThat(viewModel.uiState.value).isEqualTo(TypedAnalyticsUiState.Loading)
    }

    @Test
    fun when_updateData_with_typeId_then_uiState_updates_to_success() = runTest {
        // 收集 uiState 以激活 WhileSubscribed 上游
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 准备类型数据
        val type = RecordTypeModel(
            id = 1L,
            parentId = -1L,
            name = "餐饮",
            iconName = "vector_eating",
            typeLevel = TypeLevelEnum.FIRST,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            protected = false,
            sort = 0,
            needRelated = false,
        )
        typeRepository.addType(type)

        // 通过 typeId 更新数据
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "")

        // 验证 uiState 为 Success 且是类型模式
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TypedAnalyticsUiState.Success::class.java)
        val successState = state as TypedAnalyticsUiState.Success
        assertThat(successState.isType).isTrue()
        assertThat(successState.titleText).isEqualTo("餐饮")
    }

    @Test
    fun when_updateData_with_tagId_then_uiState_updates_to_success() = runTest {
        // 收集 uiState 以激活 WhileSubscribed 上游
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 准备标签数据
        val tag = TagModel(id = 2L, name = "日常", invisible = false)
        tagRepository.addTag(tag)

        // 通过 tagId 更新数据
        viewModel.updateData(tagId = 2L, typeId = -1L, date = "")

        // 验证 uiState 为 Success 且是标签模式
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TypedAnalyticsUiState.Success::class.java)
        val successState = state as TypedAnalyticsUiState.Success
        assertThat(successState.isType).isFalse()
        assertThat(successState.titleText).isEqualTo("日常")
    }

    @Test
    fun when_updateData_with_yearMonth_date_then_subtitleText_contains_date() = runTest {
        // 收集 uiState 以激活 WhileSubscribed 上游
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 准备类型数据
        val type = RecordTypeModel(
            id = 1L,
            parentId = -1L,
            name = "餐饮",
            iconName = "vector_eating",
            typeLevel = TypeLevelEnum.FIRST,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            protected = false,
            sort = 0,
            needRelated = false,
        )
        typeRepository.addType(type)

        // 使用年月格式日期
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "2024-06")

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TypedAnalyticsUiState.Success::class.java)
        val successState = state as TypedAnalyticsUiState.Success
        assertThat(successState.subTitleText).isEqualTo("2024-06")
    }

    @Test
    fun when_updateData_with_year_date_then_subtitleText_contains_year() = runTest {
        // 收集 uiState 以激活 WhileSubscribed 上游
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 准备类型数据
        val type = RecordTypeModel(
            id = 1L,
            parentId = -1L,
            name = "餐饮",
            iconName = "vector_eating",
            typeLevel = TypeLevelEnum.FIRST,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            protected = false,
            sort = 0,
            needRelated = false,
        )
        typeRepository.addType(type)

        // 使用年格式日期
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "2024")

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TypedAnalyticsUiState.Success::class.java)
        val successState = state as TypedAnalyticsUiState.Success
        assertThat(successState.subTitleText).isEqualTo("2024")
    }

    @Test
    fun when_updateData_with_empty_date_then_subtitleText_is_empty() = runTest {
        // 收集 uiState 以激活 WhileSubscribed 上游
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 准备类型数据
        val type = RecordTypeModel(
            id = 1L,
            parentId = -1L,
            name = "餐饮",
            iconName = "vector_eating",
            typeLevel = TypeLevelEnum.FIRST,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            protected = false,
            sort = 0,
            needRelated = false,
        )
        typeRepository.addType(type)

        // 使用空日期
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "")

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(TypedAnalyticsUiState.Success::class.java)
        val successState = state as TypedAnalyticsUiState.Success
        assertThat(successState.subTitleText).isEmpty()
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
            recordTime = 1704110400000L,
        )
    }
}
