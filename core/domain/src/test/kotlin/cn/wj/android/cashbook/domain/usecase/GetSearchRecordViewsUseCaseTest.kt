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

class GetSearchRecordViewsUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var useCase: GetSearchRecordViewsUseCase

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
        useCase = GetSearchRecordViewsUseCase(
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = transUseCase,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_blank_keyword_then_returns_empty() = runTest {
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L, remark = "午餐"))

        val result = useCase("", 0, 10)

        assertThat(result).isEmpty()
    }

    @Test
    fun when_has_matching_records_then_returns_results() = runTest {
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L, remark = "午餐"))
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = 1L, remark = "晚餐"))
        recordRepository.addRecord(createRecordModel(id = 3L, typeId = 1L, remark = "打车"))

        val result = useCase("餐", 0, 10)

        assertThat(result).hasSize(2)
    }

    @Test
    fun when_paginated_then_returns_correct_page() = runTest {
        repeat(15) { i ->
            recordRepository.addRecord(
                createRecordModel(id = i.toLong(), typeId = 1L, remark = "记录$i"),
            )
        }

        val result = useCase("记录", 0, 10)

        assertThat(result).hasSize(10)
    }
}
