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
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class GetTypedMonthSummaryUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val typeRepository = FakeTypeRepository()

    private fun useCase(repo: FakeRecordRepository) =
        GetTypedMonthSummaryUseCase(repo, typeRepository, dispatcherRule.testDispatcher)

    @Test
    fun expenditure_type_uses_finalAmount_net() = runTest {
        typeRepository.addType(createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        val repo = FakeRecordRepository()
        repo.addRecord(createRecordModel(id = 1L, typeId = 1L, amount = 500L, finalAmount = 0L, recordTime = 1_000L)) // 被报销净自付 0
        repo.addRecord(createRecordModel(id = 2L, typeId = 1L, amount = 300L, finalAmount = 300L, recordTime = 1_000L))

        val s = useCase(repo)(isType = true, id = 1L, startDate = 0L, endDate = Long.MAX_VALUE, includeChildTypes = true)

        assertThat(s.expenditure).isEqualTo(300L)
        assertThat(s.income).isEqualTo(0L)
        assertThat(s.balance).isEqualTo(-300L)
    }

    @Test
    fun transfer_type_excluded_returns_zero() = runTest {
        typeRepository.addType(createRecordTypeModel(id = 2L, typeCategory = RecordTypeCategoryEnum.TRANSFER))
        val repo = FakeRecordRepository()
        repo.addRecord(createRecordModel(id = 1L, typeId = 2L, amount = 1000L, finalAmount = 1000L, recordTime = 1_000L))

        val s = useCase(repo)(isType = true, id = 2L, startDate = 0L, endDate = Long.MAX_VALUE, includeChildTypes = true)

        assertThat(s.income).isEqualTo(0L)
        assertThat(s.expenditure).isEqualTo(0L)
        assertThat(s.balance).isEqualTo(0L)
    }

    @Test
    fun tag_mixed_income_and_expenditure_both_nonzero() = runTest {
        typeRepository.addType(createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        typeRepository.addType(createRecordTypeModel(id = 3L, typeCategory = RecordTypeCategoryEnum.INCOME))
        val repo = FakeRecordRepository()
        repo.addRecord(createRecordModel(id = 1L, typeId = 1L, amount = 200L, finalAmount = 200L, recordTime = 1_000L))
        repo.addRecord(createRecordModel(id = 2L, typeId = 3L, amount = 500L, finalAmount = 500L, recordTime = 1_000L))
        repo.addTagRelation(10L, 1L)
        repo.addTagRelation(10L, 2L)

        val s = useCase(repo)(isType = false, id = 10L, startDate = 0L, endDate = Long.MAX_VALUE, includeChildTypes = true)

        assertThat(s.income).isEqualTo(500L)
        assertThat(s.expenditure).isEqualTo(200L)
        assertThat(s.balance).isEqualTo(300L)
    }

    @Test
    fun range_filters_out_records() = runTest {
        typeRepository.addType(createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        val repo = FakeRecordRepository()
        repo.addRecord(createRecordModel(id = 1L, typeId = 1L, amount = 100L, finalAmount = 100L, recordTime = 1_000L))
        repo.addRecord(createRecordModel(id = 2L, typeId = 1L, amount = 999L, finalAmount = 999L, recordTime = 9_000L))

        val s = useCase(repo)(isType = true, id = 1L, startDate = 0L, endDate = 5_000L, includeChildTypes = true)

        assertThat(s.expenditure).isEqualTo(100L)
    }
}
