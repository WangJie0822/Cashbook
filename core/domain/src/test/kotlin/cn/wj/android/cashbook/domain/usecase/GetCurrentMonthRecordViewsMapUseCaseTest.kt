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
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_INCOME
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
            createRecordViewsEntity(id = 1L, recordTime = 1705284000000L),
            createRecordViewsEntity(id = 2L, recordTime = 1705298400000L),
        )

        val result = useCase(entities)

        assertThat(result).hasSize(1)
        val values = result.values.first()
        assertThat(values).hasSize(2)
    }

    @Test
    fun when_different_day_records_then_grouped_separately() {
        val entities = listOf(
            createRecordViewsEntity(id = 1L, recordTime = 1705284000000L),
            createRecordViewsEntity(id = 2L, recordTime = 1705384800000L),
        )

        val result = useCase(entities)

        assertThat(result).hasSize(2)
    }

    @Test
    fun when_expenditure_records_then_day_entity_has_correct_expand() {
        val entities = listOf(
            createRecordViewsEntity(
                id = 1L,
                recordTime = 1705284000000L,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                finalAmount = 10000L,
            ),
            createRecordViewsEntity(
                id = 2L,
                recordTime = 1705298400000L,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                finalAmount = 20000L,
            ),
        )

        val result = useCase(entities)
        val dayEntity = result.keys.first()

        assertThat(dayEntity.dayExpand).isEqualTo(30000L)
    }

    @Test
    fun when_income_records_then_day_entity_has_correct_income() {
        val entities = listOf(
            createRecordViewsEntity(
                id = 1L,
                recordTime = 1705284000000L,
                typeCategory = RecordTypeCategoryEnum.INCOME,
                finalAmount = 50000L,
            ),
        )

        val result = useCase(entities)
        val dayEntity = result.keys.first()

        assertThat(dayEntity.dayIncome).isEqualTo(50000L)
    }

    @Test
    fun when_transfer_records_then_concessions_offset_charges() {
        val entities = listOf(
            createRecordViewsEntity(
                id = 1L,
                recordTime = 1705284000000L,
                typeCategory = RecordTypeCategoryEnum.TRANSFER,
                charges = 1000L,
                concessions = 300L,
            ),
        )

        val result = useCase(entities)
        val dayEntity = result.keys.first()

        // 转账优惠冲减支出：1000 - 300 = 700（分）
        assertThat(dayEntity.dayExpand).isEqualTo(700L)
        assertThat(dayEntity.dayIncome).isEqualTo(0L)
    }

    @Test
    fun when_balance_records_then_excluded_from_statistics() {
        val entities = listOf(
            createRecordViewsEntity(
                id = 1L,
                recordTime = 1705284000000L,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                typeName = RECORD_TYPE_BALANCE_EXPENDITURE.name,
                finalAmount = 10000L,
            ),
            createRecordViewsEntity(
                id = 2L,
                recordTime = 1705287600000L,
                typeCategory = RecordTypeCategoryEnum.INCOME,
                typeName = RECORD_TYPE_BALANCE_INCOME.name,
                finalAmount = 20000L,
            ),
            createRecordViewsEntity(
                id = 3L,
                recordTime = 1705291200000L,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                finalAmount = 5000L,
            ),
        )

        val result = useCase(entities)
        val dayEntity = result.keys.first()

        // 平账记录不计入统计，只有 id=3 的 5000（分）支出
        assertThat(dayEntity.dayExpand).isEqualTo(5000L)
        assertThat(dayEntity.dayIncome).isEqualTo(0L)
    }

    private fun createRecordViewsEntity(
        id: Long = 1L,
        recordTime: Long = 1705284000000L,
        typeCategory: RecordTypeCategoryEnum = RecordTypeCategoryEnum.EXPENDITURE,
        typeName: String = "test",
        finalAmount: Long = 0L,
        charges: Long = 0L,
        concessions: Long = 0L,
    ): RecordViewsEntity {
        return RecordViewsEntity(
            id = id,
            typeCategory = typeCategory,
            typeName = typeName,
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
            relatedAmount = 0L,
            recordTime = recordTime,
        )
    }
}
