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

package cn.wj.android.cashbook.core.model.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn

/**
 * [DateSelectionEntity] 可配置月周期相关纯函数测试。
 */
class DateSelectionEntityMonthCycleTest {

    private fun ms(y: Int, m: Int, d: Int): Long =
        LocalDate(y, m, d).atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    @Test
    fun normalizeMonthStartDay_clampsOutOfRangeToOne() {
        assertEquals(1, normalizeMonthStartDay(0))
        assertEquals(1, normalizeMonthStartDay(-5))
        assertEquals(1, normalizeMonthStartDay(29))
        assertEquals(1, normalizeMonthStartDay(31))
        assertEquals(1, normalizeMonthStartDay(999))
        assertEquals(1, normalizeMonthStartDay(1))
        assertEquals(15, normalizeMonthStartDay(15))
        assertEquals(28, normalizeMonthStartDay(28))
    }

    @Test
    fun toDateRange_d1_isByteEquivalentToLegacy_allBranches() {
        val cases = listOf(
            DateSelectionEntity.ByDay(LocalDate(2024, 1, 15)),
            DateSelectionEntity.ByMonth(YearMonth(2024, 1)),
            DateSelectionEntity.ByYear(2024),
            DateSelectionEntity.DateRange(LocalDate(2024, 1, 1), LocalDate(2024, 1, 31)),
            DateSelectionEntity.All,
        )
        for (sel in cases) {
            assertEquals(sel.toDateRange(), sel.toDateRange(1))
        }
    }

    @Test
    fun toDateRange_byMonth_d15_spansAcrossMonths() {
        val range = DateSelectionEntity.ByMonth(YearMonth(2024, 1)).toDateRange(15)
        assertEquals(ms(2024, 1, 15), range.first)
        assertEquals(ms(2024, 2, 15), range.second)
    }

    @Test
    fun toDateRange_byMonth_d28_decemberCrossesYear() {
        val range = DateSelectionEntity.ByMonth(YearMonth(2024, 12)).toDateRange(28)
        assertEquals(ms(2024, 12, 28), range.first)
        assertEquals(ms(2025, 1, 28), range.second)
    }

    @Test
    fun toDateRange_byMonth_illegalDIsNormalized() {
        assertEquals(
            DateSelectionEntity.ByMonth(YearMonth(2024, 1)).toDateRange(1),
            DateSelectionEntity.ByMonth(YearMonth(2024, 1)).toDateRange(0),
        )
        assertEquals(
            DateSelectionEntity.ByMonth(YearMonth(2024, 2)).toDateRange(1),
            DateSelectionEntity.ByMonth(YearMonth(2024, 2)).toDateRange(29),
        )
    }

    @Test
    fun toDateRange_nonByMonthBranchesIgnoreD() {
        val day = DateSelectionEntity.ByDay(LocalDate(2024, 1, 15))
        assertEquals(day.toDateRange(1), day.toDateRange(15))
        assertEquals(0L to Long.MAX_VALUE, DateSelectionEntity.All.toDateRange(15))
    }

    @Test
    fun currentMonthPeriod_dayGteD_usesThisMonth() {
        val p = DateSelectionEntity.currentMonthPeriod(LocalDate(2024, 3, 20), 15)
        assertEquals(YearMonth(2024, 3), p.yearMonth)
    }

    @Test
    fun currentMonthPeriod_dayLtD_usesPreviousMonth() {
        val p = DateSelectionEntity.currentMonthPeriod(LocalDate(2024, 3, 5), 15)
        assertEquals(YearMonth(2024, 2), p.yearMonth)
    }

    @Test
    fun currentMonthPeriod_january_dayLtD_crossesToPreviousYear() {
        val p = DateSelectionEntity.currentMonthPeriod(LocalDate(2024, 1, 5), 15)
        assertEquals(YearMonth(2023, 12), p.yearMonth)
    }

    @Test
    fun currentMonthPeriod_d1_isAlwaysThisMonth() {
        val p = DateSelectionEntity.currentMonthPeriod(LocalDate(2024, 3, 1), 1)
        assertEquals(YearMonth(2024, 3), p.yearMonth)
    }
}
