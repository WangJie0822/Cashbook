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
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GetDefaultRecordUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var useCase: GetDefaultRecordUseCase

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        typeRepository.addType(createRecordTypeModel(id = 1L, name = "餐饮"))
        useCase = GetDefaultRecordUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_record_exists_then_returns_existing_record() = runTest {
        val existingRecord = createRecordModel(id = 10L, amount = "50", remark = "午餐")
        recordRepository.addRecord(existingRecord)

        val result = useCase(10L)

        assertThat(result).isEqualTo(existingRecord)
    }

    @Test
    fun when_record_not_exists_then_returns_default_record() = runTest {
        val result = useCase(-1L)

        // 使用默认类型 id
        assertThat(result.typeId).isEqualTo(1L)
        assertThat(result.id).isEqualTo(-1L)
    }
}
