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
import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.domain.usecase.GetReimbursableUnrelatedRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.RecordModelTransToViewsUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ReimbursementViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var viewModel: ReimbursementViewModel

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        val typeRepository = FakeTypeRepository()
        typeRepository.addType(createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        val transUseCase = RecordModelTransToViewsUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            assetRepository = FakeAssetRepository(),
            tagRepository = FakeTagRepository(),
            coroutineContext = UnconfinedTestDispatcher(),
        )
        val useCase = GetReimbursableUnrelatedRecordViewsUseCase(
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = transUseCase,
            coroutineContext = UnconfinedTestDispatcher(),
        )
        viewModel = ReimbursementViewModel(useCase)
    }

    @Test
    fun uiState_emits_success_with_count_and_total() = runTest {
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L, reimbursable = true, finalAmount = 10000L))
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = 1L, reimbursable = true, finalAmount = 20000L))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ReimbursementUiState.Success::class.java)
        state as ReimbursementUiState.Success
        assertThat(state.count).isEqualTo(2)
        assertThat(state.totalAmount).isEqualTo(30000L)
    }

    @Test
    fun showAndDismiss_recordDetailsSheet_updates_viewRecord() {
        val entity = RecordViewsEntity(
            id = 1L,
            typeId = 1L,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            typeName = "餐饮",
            typeIconResName = "ic_test",
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
            remark = "",
            reimbursable = true,
            relatedTags = emptyList(),
            relatedImage = emptyList(),
            relatedRecord = emptyList(),
            relatedAmount = 0L,
            recordTime = 1704067200000L,
        )

        viewModel.showRecordDetailsSheet(entity)
        assertThat(viewModel.viewRecord).isEqualTo(entity)

        viewModel.dismissRecordDetailSheet()
        assertThat(viewModel.viewRecord).isNull()
    }
}
