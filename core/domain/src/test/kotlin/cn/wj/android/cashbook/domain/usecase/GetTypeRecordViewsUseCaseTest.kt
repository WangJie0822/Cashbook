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
import java.time.LocalDate
import java.time.ZoneId

class GetTypeRecordViewsUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var useCase: GetTypeRecordViewsUseCase

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        val typeRepository = FakeTypeRepository()
        typeRepository.addType(createRecordTypeModel(id = 1L))
        val transUseCase = RecordModelTransToViewsUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            assetRepository = FakeAssetRepository(),
            tagRepository = FakeTagRepository(),
            coroutineContext = UnconfinedTestDispatcher(),
        )
        useCase = GetTypeRecordViewsUseCase(
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = transUseCase,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_type_id_is_invalid_then_returns_empty() = runTest {
        val result = useCase(-1L, "", 0, 10)

        assertThat(result).isEmpty()
    }

    @Test
    fun when_type_id_valid_then_returns_type_records() = runTest {
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L))
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = 1L))

        val result = useCase(1L, "", 0, 10)

        assertThat(result).hasSize(2)
    }

    @Test
    fun when_date_not_blank_then_queries_with_date() = runTest {
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L))

        val result = useCase(1L, "2024-01", 0, 10)

        assertThat(result).hasSize(1)
    }

    @Test
    fun when_millis_range_then_filters_half_open() = runTest {
        // 记录落在 2024-02-03（属周期 [2024-01-15, 2024-02-15)，不属自然 1 月）
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L, recordTime = ms(2024, 2, 3)))

        val inPeriod = useCase(1L, ms(2024, 1, 15), ms(2024, 2, 15), 0, 10)
        val inNatural = useCase(1L, ms(2024, 1, 1), ms(2024, 2, 1), 0, 10)

        assertThat(inPeriod).hasSize(1)
        assertThat(inNatural).isEmpty()
    }

    private fun ms(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
