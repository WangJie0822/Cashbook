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
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.coroutines.EmptyCoroutineContext

/**
 * [TransRecordViewsToAnalyticsBarUseCase] 可配置月周期建桶测试（C2）。
 */
class TransRecordViewsToAnalyticsBarUseCaseMonthCycleTest {

    private val useCase = TransRecordViewsToAnalyticsBarUseCase(EmptyCoroutineContext)

    private fun ms(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun expenditureRecord(timeMs: Long, finalAmount: Long): RecordViewsModel =
        RecordViewsModel(
            id = 1L,
            booksId = 1L,
            type = RecordTypeModel(
                id = 100L,
                parentId = -1L,
                name = "餐饮",
                iconName = "",
                typeLevel = TypeLevelEnum.FIRST,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                protected = false,
                sort = 0,
                needRelated = false,
            ),
            asset = null,
            relatedAsset = null,
            amount = finalAmount,
            finalAmount = finalAmount,
            charges = 0L,
            concessions = 0L,
            remark = "",
            reimbursable = false,
            relatedTags = emptyList(),
            relatedImage = emptyList(),
            relatedRecord = emptyList(),
            relatedAmount = 0L,
            recordTime = timeMs,
        )

    @Test
    fun byMonth_d15_includesNextMonthRecordsInBuckets() = runTest {
        // 周期 [2024-01-15, 2024-02-15)，记录落在 2024-02-03（自然 1 月看不到，但属本周期）
        val record = expenditureRecord(ms(2024, 2, 3), 5000L)
        val bars = useCase(DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)), listOf(record), monthStartDay = 15)
        assertThat(bars.any { it.date == "2024-02-03" }).isTrue()
        assertThat(bars.sumOf { it.expenditure }).isEqualTo(5000L)
    }

    @Test
    fun byMonth_d1_isNaturalMonth() = runTest {
        val bars = useCase(DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)), emptyList(), monthStartDay = 1)
        assertThat(bars.first().date).isEqualTo("2024-01-01")
        assertThat(bars.last().date).isEqualTo("2024-01-31")
        assertThat(bars.size).isEqualTo(31)
    }
}
