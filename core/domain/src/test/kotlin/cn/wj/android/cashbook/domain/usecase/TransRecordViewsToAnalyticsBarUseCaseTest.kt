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
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_INCOME
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.data.createRecordViewsModel
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class TransRecordViewsToAnalyticsBarUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var useCase: TransRecordViewsToAnalyticsBarUseCase

    @Before
    fun setup() {
        useCase = TransRecordViewsToAnalyticsBarUseCase(
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_year_selected_then_generates_12_entries() = runTest {
        val result = useCase(
            fromDate = LocalDate.of(2024, 1, 1),
            toDate = null,
            yearSelected = true,
            recordViewsList = emptyList(),
        )

        assertThat(result).hasSize(12)
        assertThat(result.first().date).isEqualTo("2024-01")
        assertThat(result.last().date).isEqualTo("2024-12")
    }

    @Test
    fun when_month_selected_then_generates_entries_for_each_day() = runTest {
        val result = useCase(
            fromDate = LocalDate.of(2024, 2, 1),
            toDate = null,
            yearSelected = false,
            recordViewsList = emptyList(),
        )

        // 2024年2月有29天（闰年）
        assertThat(result).hasSize(29)
        assertThat(result.first().date).isEqualTo("2024-02-01")
        assertThat(result.last().date).isEqualTo("2024-02-29")
    }

    @Test
    fun when_date_range_then_generates_entries_for_range() = runTest {
        val result = useCase(
            fromDate = LocalDate.of(2024, 3, 1),
            toDate = LocalDate.of(2024, 3, 5),
            yearSelected = false,
            recordViewsList = emptyList(),
        )

        assertThat(result).hasSize(5)
        assertThat(result.first().date).isEqualTo("2024-03-01")
        assertThat(result.last().date).isEqualTo("2024-03-05")
    }

    @Test
    fun when_has_expenditure_records_then_sums_expenditure() = runTest {
        val expenditureType = createRecordTypeModel(
            id = 1L,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )
        val records = listOf(
            createRecordViewsModel(
                type = expenditureType,
                finalAmount = 10000L,
                recordTime = 1705291200000L, // 2024-01-15 12:00
            ),
            createRecordViewsModel(
                type = expenditureType,
                finalAmount = 5000L,
                recordTime = 1705312800000L, // 2024-01-15 18:00
            ),
        )

        val result = useCase(
            fromDate = LocalDate.of(2024, 1, 1),
            toDate = null,
            yearSelected = false,
            recordViewsList = records,
        )

        val jan15 = result.find { it.date == "2024-01-15" }
        assertThat(jan15).isNotNull()
        assertThat(jan15!!.expenditure).isEqualTo(15000L)
    }

    @Test
    fun when_has_transfer_records_then_concessions_offset_charges() = runTest {
        val transferType = createRecordTypeModel(
            id = 1L,
            typeCategory = RecordTypeCategoryEnum.TRANSFER,
        )
        val records = listOf(
            createRecordViewsModel(
                type = transferType,
                charges = 1000L,
                concessions = 300L,
                recordTime = 1704081600000L, // 2024-01-01 12:00
            ),
        )

        val result = useCase(
            fromDate = LocalDate.of(2024, 1, 1),
            toDate = null,
            yearSelected = false,
            recordViewsList = records,
        )

        val jan1 = result.find { it.date == "2024-01-01" }
        assertThat(jan1).isNotNull()
        // 转账优惠冲减支出：1000 - 300 = 700（分）
        assertThat(jan1!!.expenditure).isEqualTo(700L)
        assertThat(jan1.income).isEqualTo(0L)
    }

    @Test
    fun when_balance_records_then_excluded_from_statistics() = runTest {
        val balanceExpType = createRecordTypeModel(
            id = RECORD_TYPE_BALANCE_EXPENDITURE.id,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )
        val balanceIncType = createRecordTypeModel(
            id = RECORD_TYPE_BALANCE_INCOME.id,
            typeCategory = RecordTypeCategoryEnum.INCOME,
        )
        val normalType = createRecordTypeModel(
            id = 1L,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )
        val records = listOf(
            createRecordViewsModel(
                type = balanceExpType,
                finalAmount = 10000L,
                recordTime = 1704074400000L, // 2024-01-01 10:00
            ),
            createRecordViewsModel(
                type = balanceIncType,
                finalAmount = 20000L,
                recordTime = 1704078000000L, // 2024-01-01 11:00
            ),
            createRecordViewsModel(
                type = normalType,
                finalAmount = 5000L,
                recordTime = 1704081600000L, // 2024-01-01 12:00
            ),
        )

        val result = useCase(
            fromDate = LocalDate.of(2024, 1, 1),
            toDate = null,
            yearSelected = false,
            recordViewsList = records,
        )

        val jan1 = result.find { it.date == "2024-01-01" }
        assertThat(jan1).isNotNull()
        // 平账记录不计入统计，只有 5000（分）支出
        assertThat(jan1!!.expenditure).isEqualTo(5000L)
        assertThat(jan1.income).isEqualTo(0L)
    }

    @Test
    fun when_year_selected_then_aggregates_by_month() = runTest {
        val expenditureType = createRecordTypeModel(
            id = 1L,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )
        val records = listOf(
            createRecordViewsModel(
                type = expenditureType,
                finalAmount = 10000L,
                recordTime = 1710475200000L, // 2024-03-15 12:00
            ),
            createRecordViewsModel(
                type = expenditureType,
                finalAmount = 20000L,
                recordTime = 1710907200000L, // 2024-03-20 12:00
            ),
        )

        val result = useCase(
            fromDate = LocalDate.of(2024, 1, 1),
            toDate = null,
            yearSelected = true,
            recordViewsList = records,
        )

        val march = result.find { it.date == "2024-03" }
        assertThat(march).isNotNull()
        assertThat(march!!.expenditure).isEqualTo(30000L)
    }
}
