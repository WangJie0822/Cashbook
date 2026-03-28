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
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_INCOME
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.data.createRecordViewsModel
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TransRecordViewsToAnalyticsPieUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var useCase: TransRecordViewsToAnalyticsPieUseCase

    @Before
    fun setup() {
        typeRepository = FakeTypeRepository()
        useCase = TransRecordViewsToAnalyticsPieUseCase(
            typeRepository = typeRepository,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_expenditure_records_then_aggregates_by_first_level_type() = runTest {
        val foodType = createRecordTypeModel(
            id = 1L,
            name = "餐饮",
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )
        val transportType = createRecordTypeModel(
            id = 2L,
            name = "交通",
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )
        typeRepository.addType(foodType)
        typeRepository.addType(transportType)

        val records = listOf(
            createRecordViewsModel(type = foodType, amount = 10000L, charges = 0L, concessions = 0L),
            createRecordViewsModel(type = foodType, amount = 5000L, charges = 0L, concessions = 0L),
            createRecordViewsModel(type = transportType, amount = 3000L, charges = 0L, concessions = 0L),
        )

        val result = useCase(RecordTypeCategoryEnum.EXPENDITURE, records)

        assertThat(result).hasSize(2)
        // 按百分比降序排序
        assertThat(result.first().typeName).isEqualTo("餐饮")
        assertThat(result.last().typeName).isEqualTo("交通")
    }

    @Test
    fun when_second_level_type_then_aggregates_under_parent() = runTest {
        val foodType = createRecordTypeModel(
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
        typeRepository.addType(foodType)
        typeRepository.addType(lunchType)

        val records = listOf(
            createRecordViewsModel(type = lunchType, amount = 3000L, charges = 0L, concessions = 0L),
            createRecordViewsModel(type = foodType, amount = 2000L, charges = 0L, concessions = 0L),
        )

        val result = useCase(RecordTypeCategoryEnum.EXPENDITURE, records)

        // 应该只有一个一级分类
        assertThat(result).hasSize(1)
        assertThat(result.first().typeName).isEqualTo("餐饮")
        assertThat(result.first().totalAmount).isEqualTo(5000L)
    }

    @Test
    fun when_expenditure_then_calculates_amount_plus_charges_minus_concessions() = runTest {
        val type = createRecordTypeModel(
            id = 1L,
            name = "餐饮",
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )
        typeRepository.addType(type)

        val records = listOf(
            createRecordViewsModel(type = type, amount = 10000L, charges = 500L, concessions = 1000L),
        )

        val result = useCase(RecordTypeCategoryEnum.EXPENDITURE, records)

        // 支出: amount + charges - concessions = 10000 + 500 - 1000 = 9500
        assertThat(result.first().totalAmount).isEqualTo(9500L)
    }

    @Test
    fun when_income_then_calculates_amount_minus_charges() = runTest {
        val type = createRecordTypeModel(
            id = 1L,
            name = "工资",
            typeCategory = RecordTypeCategoryEnum.INCOME,
        )
        typeRepository.addType(type)

        val records = listOf(
            createRecordViewsModel(type = type, amount = 500000L, charges = 10000L, concessions = 0L),
        )

        val result = useCase(RecordTypeCategoryEnum.INCOME, records)

        // 收入: amount - charges = 500000 - 10000 = 490000
        assertThat(result.first().totalAmount).isEqualTo(490000L)
    }

    @Test
    fun when_results_then_sorted_by_percent_descending() = runTest {
        val typeA = createRecordTypeModel(id = 1L, name = "A", typeCategory = RecordTypeCategoryEnum.EXPENDITURE)
        val typeB = createRecordTypeModel(id = 2L, name = "B", typeCategory = RecordTypeCategoryEnum.EXPENDITURE)
        val typeC = createRecordTypeModel(id = 3L, name = "C", typeCategory = RecordTypeCategoryEnum.EXPENDITURE)
        typeRepository.addType(typeA)
        typeRepository.addType(typeB)
        typeRepository.addType(typeC)

        val records = listOf(
            createRecordViewsModel(type = typeA, amount = 1000L, charges = 0L, concessions = 0L),
            createRecordViewsModel(type = typeB, amount = 5000L, charges = 0L, concessions = 0L),
            createRecordViewsModel(type = typeC, amount = 3000L, charges = 0L, concessions = 0L),
        )

        val result = useCase(RecordTypeCategoryEnum.EXPENDITURE, records)

        assertThat(result[0].typeName).isEqualTo("B")
        assertThat(result[1].typeName).isEqualTo("C")
        assertThat(result[2].typeName).isEqualTo("A")
    }

    @Test
    fun when_empty_records_then_returns_empty() = runTest {
        val result = useCase(RecordTypeCategoryEnum.EXPENDITURE, emptyList())

        assertThat(result).isEmpty()
    }

    @Test
    fun when_has_balance_records_then_excluded_from_pie() = runTest {
        val foodType = createRecordTypeModel(
            id = 1L,
            name = "餐饮",
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )
        typeRepository.addType(foodType)

        val records = listOf(
            createRecordViewsModel(type = foodType, amount = 10000L, charges = 0L, concessions = 0L),
            // 平账支出记录
            createRecordViewsModel(type = RECORD_TYPE_BALANCE_EXPENDITURE, amount = 5000L, charges = 0L, concessions = 0L),
            // 平账收入记录
            createRecordViewsModel(type = RECORD_TYPE_BALANCE_INCOME, amount = 3000L, charges = 0L, concessions = 0L),
        )

        val expenditureResult = useCase(RecordTypeCategoryEnum.EXPENDITURE, records)

        // 平账支出记录应被排除，只剩餐饮
        assertThat(expenditureResult).hasSize(1)
        assertThat(expenditureResult.first().typeName).isEqualTo("餐饮")
        assertThat(expenditureResult.first().totalAmount).isEqualTo(10000L)

        val incomeResult = useCase(RecordTypeCategoryEnum.INCOME, records)

        // 平账收入记录应被排除，收入结果为空
        assertThat(incomeResult).isEmpty()
    }
}
