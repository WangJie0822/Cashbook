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
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GetDefaultRelatedRecordListUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var useCase: GetDefaultRelatedRecordListUseCase

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        useCase = GetDefaultRelatedRecordListUseCase(
            recordRepository = recordRepository,
        )
    }

    @Test
    fun when_has_related_records_then_returns_entities() = runTest {
        val relatedRecord = createRecordModel(id = 2L, amount = "100")
        recordRepository.addRecord(relatedRecord)
        recordRepository.setRelatedIds(1L, listOf(2L))

        val result = useCase(1L)

        assertThat(result).hasSize(1)
    }

    @Test
    fun when_no_related_records_then_returns_empty() = runTest {
        val result = useCase(1L)

        assertThat(result).isEmpty()
    }
}
