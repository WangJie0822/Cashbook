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
import cn.wj.android.cashbook.domain.usecase.GetSearchRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.RecordModelTransToViewsUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
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
        val getSearchRecordViewsUseCase = GetSearchRecordViewsUseCase(
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = recordModelTransToViewsUseCase,
            coroutineContext = dispatcherRule.testDispatcher,
        )

        viewModel = SearchViewModel(
            recordRepository = recordRepository,
            getSearchRecordViewsUseCase = getSearchRecordViewsUseCase,
        )
    }

    @Test
    fun when_initialized_then_viewRecordData_is_null() {
        // 初始状态下 viewRecordData 应为 null
        assertThat(viewModel.viewRecordData).isNull()
    }

    @Test
    fun when_showRecordDetailSheet_then_viewRecordData_is_set() {
        val record = createTestRecordViewsEntity(id = 1L)

        // 显示记录详情
        viewModel.showRecordDetailSheet(record)

        // 验证 viewRecordData 已设置
        assertThat(viewModel.viewRecordData).isEqualTo(record)
    }

    @Test
    fun when_dismissRecordDetailSheet_then_viewRecordData_is_null() {
        val record = createTestRecordViewsEntity(id = 1L)

        // 先显示记录详情
        viewModel.showRecordDetailSheet(record)
        assertThat(viewModel.viewRecordData).isNotNull()

        // 隐藏记录详情
        viewModel.dismissRecordDetailSheet()

        // 验证 viewRecordData 已置空
        assertThat(viewModel.viewRecordData).isNull()
    }

    @Test
    fun when_clearSearchHistory_then_repository_history_cleared() = runTest {
        // 先添加搜索历史
        recordRepository.addSearchHistory("测试关键词")
        assertThat(recordRepository.searchHistoryListData).isNotNull()

        // 清空搜索历史
        viewModel.clearSearchHistory()

        // 验证搜索历史已清空
        assertThat(viewModel.searchHistoryListData.value).isEmpty()
    }

    @Test
    fun when_initialized_then_searchHistoryListData_is_empty() {
        // 初始状态下搜索历史应为空
        assertThat(viewModel.searchHistoryListData.value).isEmpty()
    }

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
            recordTime = 1704067200000L,
        )
    }
}
