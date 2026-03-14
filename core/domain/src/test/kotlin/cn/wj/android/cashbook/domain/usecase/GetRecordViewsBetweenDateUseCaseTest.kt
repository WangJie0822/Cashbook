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

import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
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
import java.time.YearMonth

class GetRecordViewsBetweenDateUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var useCase: GetRecordViewsBetweenDateUseCase

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
        useCase = GetRecordViewsBetweenDateUseCase(
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = transUseCase,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_has_records_then_transforms_to_views() = runTest {
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L, amount = 10000L))
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = 1L, amount = 20000L))

        val result = useCase(
            dateSelection = DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)),
        )

        assertThat(result).hasSize(2)
    }

    @Test
    fun when_year_selected_then_queries_full_year() = runTest {
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L))

        val result = useCase(
            dateSelection = DateSelectionEntity.ByYear(2024),
        )

        assertThat(result).hasSize(1)
    }
}
