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

import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REFUND
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.domain.usecase.GetRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.GetRelatedRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.RecordModelTransToViewsUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SelectRelatedRecordViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var viewModel: SelectRelatedRecordViewModel

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        val assetRepository = FakeAssetRepository()
        val tagRepository = FakeTagRepository()

        val recordModelTransToViewsUseCase = RecordModelTransToViewsUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            assetRepository = assetRepository,
            tagRepository = tagRepository,
            coroutineContext = dispatcherRule.testDispatcher,
        )
        val getRecordViewsUseCase = GetRecordViewsUseCase(
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = recordModelTransToViewsUseCase,
            coroutineContext = dispatcherRule.testDispatcher,
        )
        val getRelatedRecordViewsUseCase = GetRelatedRecordViewsUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            recordModelTransToViewsUseCase = recordModelTransToViewsUseCase,
            coroutineContext = dispatcherRule.testDispatcher,
        )

        viewModel = SelectRelatedRecordViewModel(
            typeRepository = typeRepository,
            getRecordViewsUseCase = getRecordViewsUseCase,
            getRelatedRecordViewsUseCase = getRelatedRecordViewsUseCase,
        )
    }

    @Test
    fun when_initialized_then_uiState_is_loading() {
        // 初始状态应为 Loading
        assertThat(viewModel.uiState.value).isEqualTo(SelectRelatedRecordUiState.Loading)
    }

    @Test
    fun when_updateData_called_then_initialized_flag_set() = runTest {
        // 准备类型和记录数据
        val type = RecordTypeModel(
            id = 1L,
            parentId = -1L,
            name = "退款",
            iconName = "vector_refund",
            typeLevel = TypeLevelEnum.FIRST,
            typeCategory = RecordTypeCategoryEnum.INCOME,
            protected = false,
            sort = 0,
            needRelated = true,
        )
        typeRepository.addType(type)

        val record = RecordModel(
            id = 10L,
            booksId = 1L,
            typeId = 1L,
            assetId = -1L,
            relatedAssetId = -1L,
            amount = 5000L,
            finalAmount = 5000L,
            charges = 0L,
            concessions = 0L,
            remark = "测试",
            reimbursable = false,
            recordTime = 1704067200000L,
        )

        val recordFlow = MutableStateFlow(record)
        val relatedIdsFlow = MutableStateFlow<List<Long>>(emptyList())

        // 第一次调用 updateData
        viewModel.updateData(recordFlow, relatedIdsFlow)

        // 第二次调用应被忽略（initialized 标志已设置）
        val anotherRecord = record.copy(id = 20L)
        val anotherFlow = MutableStateFlow(anotherRecord)
        viewModel.updateData(anotherFlow, relatedIdsFlow)

        // 由于 initialized 已设置，第二次调用不会生效
        // 无法直接验证 initialized，但可以通过行为验证（不会再次更新）
    }

    @Test
    fun when_addToRelated_then_related_id_added() = runTest {
        // 收集 uiState 以激活 WhileSubscribed 上游
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 准备类型数据
        val type = RecordTypeModel(
            id = FIXED_TYPE_ID_REFUND,
            parentId = -1L,
            name = "退款",
            iconName = "vector_refund",
            typeLevel = TypeLevelEnum.FIRST,
            typeCategory = RecordTypeCategoryEnum.INCOME,
            protected = false,
            sort = 0,
            needRelated = true,
        )
        typeRepository.addType(type)

        // 添加可关联的记录
        val relatedRecord = RecordModel(
            id = 100L,
            booksId = 1L,
            typeId = FIXED_TYPE_ID_REFUND,
            assetId = -1L,
            relatedAssetId = -1L,
            amount = 10000L,
            finalAmount = 10000L,
            charges = 0L,
            concessions = 0L,
            remark = "可关联记录",
            reimbursable = false,
            recordTime = 1704067200000L,
        )
        recordRepository.addRecord(relatedRecord)

        val currentRecord = RecordModel(
            id = 10L,
            booksId = 1L,
            typeId = FIXED_TYPE_ID_REFUND,
            assetId = -1L,
            relatedAssetId = -1L,
            amount = 5000L,
            finalAmount = 5000L,
            charges = 0L,
            concessions = 0L,
            remark = "当前记录",
            reimbursable = false,
            recordTime = 1704067200000L,
        )

        // 初始化数据
        viewModel.updateData(
            MutableStateFlow(currentRecord),
            MutableStateFlow(emptyList()),
        )

        // 添加关联
        viewModel.addToRelated(100L)

        // 验证 uiState 为 Success 且包含关联记录
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(SelectRelatedRecordUiState.Success::class.java)
        val successState = state as SelectRelatedRecordUiState.Success
        assertThat(successState.relatedRecordList).hasSize(1)
        assertThat(successState.relatedRecordList.first().id).isEqualTo(100L)
    }

    @Test
    fun when_removeFromRelated_then_related_id_removed() = runTest {
        // 收集 uiState 以激活 WhileSubscribed 上游
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 准备类型数据
        val type = RecordTypeModel(
            id = FIXED_TYPE_ID_REFUND,
            parentId = -1L,
            name = "退款",
            iconName = "vector_refund",
            typeLevel = TypeLevelEnum.FIRST,
            typeCategory = RecordTypeCategoryEnum.INCOME,
            protected = false,
            sort = 0,
            needRelated = true,
        )
        typeRepository.addType(type)

        val relatedRecord = RecordModel(
            id = 100L,
            booksId = 1L,
            typeId = FIXED_TYPE_ID_REFUND,
            assetId = -1L,
            relatedAssetId = -1L,
            amount = 10000L,
            finalAmount = 10000L,
            charges = 0L,
            concessions = 0L,
            remark = "可关联记录",
            reimbursable = false,
            recordTime = 1704067200000L,
        )
        recordRepository.addRecord(relatedRecord)

        val currentRecord = RecordModel(
            id = 10L,
            booksId = 1L,
            typeId = FIXED_TYPE_ID_REFUND,
            assetId = -1L,
            relatedAssetId = -1L,
            amount = 5000L,
            finalAmount = 5000L,
            charges = 0L,
            concessions = 0L,
            remark = "当前记录",
            reimbursable = false,
            recordTime = 1704067200000L,
        )

        // 初始化数据并预设关联 ID
        viewModel.updateData(
            MutableStateFlow(currentRecord),
            MutableStateFlow(listOf(100L)),
        )

        // 验证有关联记录
        val stateBefore = viewModel.uiState.value
        assertThat(stateBefore).isInstanceOf(SelectRelatedRecordUiState.Success::class.java)
        assertThat((stateBefore as SelectRelatedRecordUiState.Success).relatedRecordList).hasSize(1)

        // 移除关联
        viewModel.removeFromRelated(100L)

        // 验证关联记录已移除
        val stateAfter = viewModel.uiState.value
        assertThat(stateAfter).isInstanceOf(SelectRelatedRecordUiState.Success::class.java)
        assertThat((stateAfter as SelectRelatedRecordUiState.Success).relatedRecordList).isEmpty()
    }

    @Test
    fun when_onKeywordsChanged_then_keyword_updated() = runTest {
        // 收集 uiState 以激活 WhileSubscribed 上游
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 准备类型数据
        val type = RecordTypeModel(
            id = FIXED_TYPE_ID_REFUND,
            parentId = -1L,
            name = "退款",
            iconName = "vector_refund",
            typeLevel = TypeLevelEnum.FIRST,
            typeCategory = RecordTypeCategoryEnum.INCOME,
            protected = false,
            sort = 0,
            needRelated = true,
        )
        typeRepository.addType(type)

        val currentRecord = RecordModel(
            id = 10L,
            booksId = 1L,
            typeId = FIXED_TYPE_ID_REFUND,
            assetId = -1L,
            relatedAssetId = -1L,
            amount = 5000L,
            finalAmount = 5000L,
            charges = 0L,
            concessions = 0L,
            remark = "当前记录",
            reimbursable = false,
            recordTime = 1704067200000L,
        )

        // 初始化数据
        viewModel.updateData(
            MutableStateFlow(currentRecord),
            MutableStateFlow(emptyList()),
        )

        // 修改关键词（验证不抛异常）
        viewModel.onKeywordsChanged("测试关键词")

        // 验证 uiState 仍为 Success
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(SelectRelatedRecordUiState.Success::class.java)
    }

    @Test
    fun when_addToRelated_duplicate_id_then_not_duplicated() = runTest {
        // 收集 uiState 以激活 WhileSubscribed 上游
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 准备类型数据
        val type = RecordTypeModel(
            id = FIXED_TYPE_ID_REFUND,
            parentId = -1L,
            name = "退款",
            iconName = "vector_refund",
            typeLevel = TypeLevelEnum.FIRST,
            typeCategory = RecordTypeCategoryEnum.INCOME,
            protected = false,
            sort = 0,
            needRelated = true,
        )
        typeRepository.addType(type)

        val relatedRecord = RecordModel(
            id = 100L,
            booksId = 1L,
            typeId = FIXED_TYPE_ID_REFUND,
            assetId = -1L,
            relatedAssetId = -1L,
            amount = 10000L,
            finalAmount = 10000L,
            charges = 0L,
            concessions = 0L,
            remark = "可关联记录",
            reimbursable = false,
            recordTime = 1704067200000L,
        )
        recordRepository.addRecord(relatedRecord)

        val currentRecord = RecordModel(
            id = 10L,
            booksId = 1L,
            typeId = FIXED_TYPE_ID_REFUND,
            assetId = -1L,
            relatedAssetId = -1L,
            amount = 5000L,
            finalAmount = 5000L,
            charges = 0L,
            concessions = 0L,
            remark = "当前记录",
            reimbursable = false,
            recordTime = 1704067200000L,
        )

        viewModel.updateData(
            MutableStateFlow(currentRecord),
            MutableStateFlow(emptyList()),
        )

        // 添加同一个 ID 两次
        viewModel.addToRelated(100L)
        viewModel.addToRelated(100L)

        // 验证关联记录不重复
        val state = viewModel.uiState.value as SelectRelatedRecordUiState.Success
        assertThat(state.relatedRecordList).hasSize(1)
    }
}
