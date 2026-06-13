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

package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GetReimbursableUnrelatedRecordViewsUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var useCase: GetReimbursableUnrelatedRecordViewsUseCase

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        typeRepository.addType(
            createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE),
        )
        val transUseCase = RecordModelTransToViewsUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            assetRepository = FakeAssetRepository(),
            tagRepository = FakeTagRepository(),
            coroutineContext = UnconfinedTestDispatcher(),
        )
        useCase = GetReimbursableUnrelatedRecordViewsUseCase(
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = transUseCase,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_empty_then_zero_count_and_total() = runTest {
        val result = useCase()

        assertThat(result.records).isEmpty()
        assertThat(result.count).isEqualTo(0)
        assertThat(result.totalAmount).isEqualTo(0L)
    }

    @Test
    fun when_multiple_reimbursable_unrelated_then_aggregates_count_and_sum_finalAmount() = runTest {
        recordRepository.addRecord(
            createRecordModel(id = 1L, typeId = 1L, reimbursable = true, finalAmount = 10000L),
        )
        recordRepository.addRecord(
            createRecordModel(id = 2L, typeId = 1L, reimbursable = true, finalAmount = 25000L),
        )
        // 不可报销 → 不计入
        recordRepository.addRecord(
            createRecordModel(id = 3L, typeId = 1L, reimbursable = false, finalAmount = 9999L),
        )

        val result = useCase()

        assertThat(result.count).isEqualTo(2)
        assertThat(result.totalAmount).isEqualTo(35000L)
        assertThat(result.records.map { it.id }).containsExactly(1L, 2L)
    }

    @Test
    fun when_record_related_either_direction_then_excluded() = runTest {
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L, reimbursable = true, finalAmount = 10000L))
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = 1L, reimbursable = true, finalAmount = 20000L))
        recordRepository.setRelatedIds(1L, listOf(99L)) // 作为吸收者
        recordRepository.setRelatedFromIds(2L, listOf(88L)) // 作为被吸收支出

        val result = useCase()

        assertThat(result.records).isEmpty()
        assertThat(result.totalAmount).isEqualTo(0L)
    }
}
