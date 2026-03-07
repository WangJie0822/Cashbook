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

import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetCurrentMonthRecordViewsMapUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var useCase: GetCurrentMonthRecordViewsMapUseCase

    @Before
    fun setup() {
        useCase = GetCurrentMonthRecordViewsMapUseCase()
    }

    @Test
    fun when_empty_list_then_returns_empty_map() {
        val result = useCase(emptyList())

        assertThat(result).isEmpty()
    }

    @Test
    fun when_same_day_records_then_grouped_together() {
        val entities = listOf(
            createRecordViewsEntity(id = 1L, recordTime = "2024-01-15 10:00"),
            createRecordViewsEntity(id = 2L, recordTime = "2024-01-15 14:00"),
        )

        val result = useCase(entities)

        assertThat(result).hasSize(1)
        val values = result.values.first()
        assertThat(values).hasSize(2)
    }

    @Test
    fun when_different_day_records_then_grouped_separately() {
        val entities = listOf(
            createRecordViewsEntity(id = 1L, recordTime = "2024-01-15 10:00"),
            createRecordViewsEntity(id = 2L, recordTime = "2024-01-16 14:00"),
        )

        val result = useCase(entities)

        assertThat(result).hasSize(2)
    }

    @Test
    fun when_expenditure_records_then_day_entity_has_correct_expand() {
        val entities = listOf(
            createRecordViewsEntity(
                id = 1L,
                recordTime = "2024-01-15 10:00",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                finalAmount = "100",
            ),
            createRecordViewsEntity(
                id = 2L,
                recordTime = "2024-01-15 14:00",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                finalAmount = "200",
            ),
        )

        val result = useCase(entities)
        val dayEntity = result.keys.first()

        assertThat(dayEntity.dayExpand).isEqualTo("300")
    }

    @Test
    fun when_income_records_then_day_entity_has_correct_income() {
        val entities = listOf(
            createRecordViewsEntity(
                id = 1L,
                recordTime = "2024-01-15 10:00",
                typeCategory = RecordTypeCategoryEnum.INCOME,
                finalAmount = "500",
            ),
        )

        val result = useCase(entities)
        val dayEntity = result.keys.first()

        assertThat(dayEntity.dayIncome).isEqualTo("500")
    }

    @Test
    fun when_transfer_records_then_charges_and_concessions_counted() {
        val entities = listOf(
            createRecordViewsEntity(
                id = 1L,
                recordTime = "2024-01-15 10:00",
                typeCategory = RecordTypeCategoryEnum.TRANSFER,
                charges = "5",
                concessions = "10",
            ),
        )

        val result = useCase(entities)
        val dayEntity = result.keys.first()

        assertThat(dayEntity.dayExpand).isEqualTo("5")
        assertThat(dayEntity.dayIncome).isEqualTo("10")
    }

    private fun createRecordViewsEntity(
        id: Long = 1L,
        recordTime: String = "2024-01-15 10:00",
        typeCategory: RecordTypeCategoryEnum = RecordTypeCategoryEnum.EXPENDITURE,
        finalAmount: String = "0",
        charges: String = "0",
        concessions: String = "0",
    ): RecordViewsEntity {
        return RecordViewsEntity(
            id = id,
            typeCategory = typeCategory,
            typeName = "test",
            typeIconResName = "test_icon",
            assetId = null,
            assetName = null,
            assetIconResId = null,
            relatedAssetId = null,
            relatedAssetName = null,
            relatedAssetIconResId = null,
            amount = finalAmount,
            finalAmount = finalAmount,
            charges = charges,
            concessions = concessions,
            remark = "",
            reimbursable = false,
            relatedTags = emptyList(),
            relatedImage = emptyList(),
            relatedRecord = emptyList(),
            relatedAmount = "0",
            recordTime = recordTime,
        )
    }
}
