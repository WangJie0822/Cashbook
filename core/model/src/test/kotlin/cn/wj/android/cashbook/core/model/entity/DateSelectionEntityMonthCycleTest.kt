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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * [DateSelectionEntity] 可配置月周期相关纯函数测试。
 */
class DateSelectionEntityMonthCycleTest {

    private fun ms(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun normalizeMonthStartDay_clampsOutOfRangeToOne() {
        assertThat(normalizeMonthStartDay(0)).isEqualTo(1)
        assertThat(normalizeMonthStartDay(-5)).isEqualTo(1)
        assertThat(normalizeMonthStartDay(29)).isEqualTo(1)
        assertThat(normalizeMonthStartDay(31)).isEqualTo(1)
        assertThat(normalizeMonthStartDay(999)).isEqualTo(1)
        assertThat(normalizeMonthStartDay(1)).isEqualTo(1)
        assertThat(normalizeMonthStartDay(15)).isEqualTo(15)
        assertThat(normalizeMonthStartDay(28)).isEqualTo(28)
    }

    @Test
    fun toDateRange_d1_isByteEquivalentToLegacy_allBranches() {
        val cases = listOf(
            DateSelectionEntity.ByDay(LocalDate.of(2024, 1, 15)),
            DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)),
            DateSelectionEntity.ByYear(2024),
            DateSelectionEntity.DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)),
            DateSelectionEntity.All,
        )
        for (sel in cases) {
            assertThat(sel.toDateRange(1)).isEqualTo(sel.toDateRange())
        }
    }

    @Test
    fun toDateRange_byMonth_d15_spansAcrossMonths() {
        val range = DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)).toDateRange(15)
        assertThat(range.first).isEqualTo(ms(2024, 1, 15))
        assertThat(range.second).isEqualTo(ms(2024, 2, 15))
    }

    @Test
    fun toDateRange_byMonth_d28_decemberCrossesYear() {
        val range = DateSelectionEntity.ByMonth(YearMonth.of(2024, 12)).toDateRange(28)
        assertThat(range.first).isEqualTo(ms(2024, 12, 28))
        assertThat(range.second).isEqualTo(ms(2025, 1, 28))
    }

    @Test
    fun toDateRange_byMonth_illegalDIsNormalized() {
        assertThat(DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)).toDateRange(0))
            .isEqualTo(DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)).toDateRange(1))
        assertThat(DateSelectionEntity.ByMonth(YearMonth.of(2024, 2)).toDateRange(29))
            .isEqualTo(DateSelectionEntity.ByMonth(YearMonth.of(2024, 2)).toDateRange(1))
    }

    @Test
    fun toDateRange_nonByMonthBranchesIgnoreD() {
        val day = DateSelectionEntity.ByDay(LocalDate.of(2024, 1, 15))
        assertThat(day.toDateRange(15)).isEqualTo(day.toDateRange(1))
        assertThat(DateSelectionEntity.All.toDateRange(15)).isEqualTo(0L to Long.MAX_VALUE)
    }

    @Test
    fun currentMonthPeriod_dayGteD_usesThisMonth() {
        val p = DateSelectionEntity.currentMonthPeriod(LocalDate.of(2024, 3, 20), 15)
        assertThat(p.yearMonth).isEqualTo(YearMonth.of(2024, 3))
    }

    @Test
    fun currentMonthPeriod_dayLtD_usesPreviousMonth() {
        val p = DateSelectionEntity.currentMonthPeriod(LocalDate.of(2024, 3, 5), 15)
        assertThat(p.yearMonth).isEqualTo(YearMonth.of(2024, 2))
    }

    @Test
    fun currentMonthPeriod_january_dayLtD_crossesToPreviousYear() {
        val p = DateSelectionEntity.currentMonthPeriod(LocalDate.of(2024, 1, 5), 15)
        assertThat(p.yearMonth).isEqualTo(YearMonth.of(2023, 12))
    }

    @Test
    fun currentMonthPeriod_d1_isAlwaysThisMonth() {
        val p = DateSelectionEntity.currentMonthPeriod(LocalDate.of(2024, 3, 1), 1)
        assertThat(p.yearMonth).isEqualTo(YearMonth.of(2024, 3))
    }
}
