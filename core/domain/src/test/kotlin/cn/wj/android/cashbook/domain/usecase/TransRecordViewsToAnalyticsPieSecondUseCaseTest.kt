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
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.data.createRecordViewsModel
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TransRecordViewsToAnalyticsPieSecondUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var useCase: TransRecordViewsToAnalyticsPieSecondUseCase

    @Before
    fun setup() {
        useCase = TransRecordViewsToAnalyticsPieSecondUseCase(
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_has_second_level_types_then_aggregates_by_each_type() = runTest {
        val parentType = createRecordTypeModel(
            id = 1L,
            name = "餐饮",
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )
        val lunchType = createRecordTypeModel(
            id = 10L,
            parentId = 1L,
            name = "午餐",
            typeLevel = TypeLevelEnum.SECOND,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )
        val dinnerType = createRecordTypeModel(
            id = 11L,
            parentId = 1L,
            name = "晚餐",
            typeLevel = TypeLevelEnum.SECOND,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )

        val records = listOf(
            createRecordViewsModel(type = parentType, amount = 2000L, charges = 0L, concessions = 0L),
            createRecordViewsModel(type = lunchType, amount = 3000L, charges = 0L, concessions = 0L),
            createRecordViewsModel(type = dinnerType, amount = 5000L, charges = 0L, concessions = 0L),
        )

        val result = useCase(1L, records)

        assertThat(result).hasSize(3)
        // 按百分比降序排序
        assertThat(result.first().typeName).isEqualTo("晚餐")
    }

    @Test
    fun when_empty_category_list_then_returns_empty() = runTest {
        val result = useCase(999L, emptyList())

        assertThat(result).isEmpty()
    }

    @Test
    fun when_no_matching_type_then_returns_empty() = runTest {
        val otherType = createRecordTypeModel(
            id = 2L,
            name = "交通",
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )
        val records = listOf(
            createRecordViewsModel(type = otherType, amount = 5000L, charges = 0L, concessions = 0L),
        )

        val result = useCase(1L, records)

        assertThat(result).isEmpty()
    }
}
