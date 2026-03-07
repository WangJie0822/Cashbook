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

class GetRelatedRecordViewsUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var useCase: GetRelatedRecordViewsUseCase

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        val expenditureType = createRecordTypeModel(
            id = 1L,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )
        typeRepository.addType(expenditureType)
        val transUseCase = RecordModelTransToViewsUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            assetRepository = FakeAssetRepository(),
            tagRepository = FakeTagRepository(),
            coroutineContext = UnconfinedTestDispatcher(),
        )
        useCase = GetRelatedRecordViewsUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            recordModelTransToViewsUseCase = transUseCase,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_record_type_is_null_then_returns_empty() = runTest {
        val result = useCase("", null)

        assertThat(result).isEmpty()
    }

    @Test
    fun given_reimburse_type_when_no_keyword_then_returns_reimbursable_records() = runTest {
        val reimburseType = createRecordTypeModel(
            id = 10L,
            typeCategory = RecordTypeCategoryEnum.INCOME,
        )
        typeRepository.addType(reimburseType)
        typeRepository.setReimburse(10L)

        recordRepository.addRecord(
            createRecordModel(id = 1L, typeId = 1L, reimbursable = true),
        )
        recordRepository.addRecord(
            createRecordModel(id = 2L, typeId = 1L, reimbursable = false),
        )

        val result = useCase("", reimburseType)

        assertThat(result).hasSize(1)
    }

    @Test
    fun given_refund_type_when_no_keyword_then_returns_refundable_records() = runTest {
        val refundType = createRecordTypeModel(
            id = 11L,
            typeCategory = RecordTypeCategoryEnum.INCOME,
        )
        typeRepository.addType(refundType)
        typeRepository.setRefund(11L)

        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L))
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = 1L))

        val result = useCase("", refundType)

        assertThat(result).hasSize(2)
    }

    @Test
    fun when_record_already_has_related_then_excluded() = runTest {
        val reimburseType = createRecordTypeModel(
            id = 10L,
            typeCategory = RecordTypeCategoryEnum.INCOME,
        )
        typeRepository.addType(reimburseType)
        typeRepository.setReimburse(10L)

        recordRepository.addRecord(
            createRecordModel(id = 1L, typeId = 1L, reimbursable = true),
        )
        // id=1 已经有关联记录
        recordRepository.setRelatedIds(1L, listOf(99L))

        val result = useCase("", reimburseType)

        assertThat(result).isEmpty()
    }
}
