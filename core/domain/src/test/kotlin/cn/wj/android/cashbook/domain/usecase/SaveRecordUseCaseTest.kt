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
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SaveRecordUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var useCase: SaveRecordUseCase

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        useCase = SaveRecordUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_save_record_then_needRelated_fetched_from_type_repository() = runTest {
        // 设置类型需要关联
        typeRepository.setNeedRelated(1L)
        val record = createRecordModel(typeId = 1L)

        useCase(record, listOf(1L), listOf(2L), emptyList())

        assertThat(recordRepository.lastUpdatedNeedRelated).isTrue()
        assertThat(recordRepository.lastUpdatedRecord).isEqualTo(record)
        assertThat(recordRepository.lastUpdatedTagIdList).containsExactly(1L)
    }

    @Test
    fun when_save_record_with_no_related_type_then_needRelated_is_false() = runTest {
        val record = createRecordModel(typeId = 2L)

        useCase(record, emptyList(), emptyList(), emptyList())

        assertThat(recordRepository.lastUpdatedNeedRelated).isFalse()
    }

    @Test
    fun when_save_record_then_record_is_passed_to_repository() = runTest {
        val record = createRecordModel(
            id = 10L,
            amount = "100",
            remark = "测试记录",
        )

        useCase(record, listOf(1L, 2L), listOf(3L), emptyList())

        assertThat(recordRepository.lastUpdatedRecord).isEqualTo(record)
        assertThat(recordRepository.lastUpdatedTagIdList).containsExactly(1L, 2L)
    }
}
