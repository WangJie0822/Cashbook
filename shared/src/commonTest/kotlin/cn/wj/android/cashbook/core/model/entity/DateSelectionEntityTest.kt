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

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DateSelectionEntityTest {

    @Test
    fun fromDisplayTextOrNull_month() {
        assertEquals(
            DateSelectionEntity.ByMonth(YearMonth(2024, 6)),
            DateSelectionEntity.fromDisplayTextOrNull("2024-06"),
        )
    }

    @Test
    fun fromDisplayTextOrNull_year() {
        assertEquals(
            DateSelectionEntity.ByYear(2024),
            DateSelectionEntity.fromDisplayTextOrNull("2024"),
        )
    }

    @Test
    fun fromDisplayTextOrNull_range() {
        assertEquals(
            DateSelectionEntity.DateRange(
                LocalDate(2024, 1, 1),
                LocalDate(2024, 3, 31),
            ),
            DateSelectionEntity.fromDisplayTextOrNull("2024-01-01~2024-03-31"),
        )
    }

    @Test
    fun fromDisplayTextOrNull_all() {
        assertEquals(DateSelectionEntity.All, DateSelectionEntity.fromDisplayTextOrNull("全部"))
    }

    @Test
    fun fromDisplayTextOrNull_blank_returns_null() {
        assertNull(DateSelectionEntity.fromDisplayTextOrNull(""))
        assertNull(DateSelectionEntity.fromDisplayTextOrNull("   "))
    }

    @Test
    fun fromDisplayTextOrNull_invalid_returns_null() {
        assertNull(DateSelectionEntity.fromDisplayTextOrNull("2024-13"))
        assertNull(DateSelectionEntity.fromDisplayTextOrNull("abc"))
    }

    /** 单日 `YYYY-MM-DD` → ByDay（3-seg 分支，此前仅经 `~` DateRange 间接触及）。 */
    @Test
    fun fromDisplayTextOrNull_day() {
        assertEquals(
            DateSelectionEntity.ByDay(LocalDate(2024, 6, 15)),
            DateSelectionEntity.fromDisplayTextOrNull("2024-06-15"),
        )
    }

    /** getDisplayText 各分支格式（补零、无分隔），守护迁移的 month.number/day 访问器改写。 */
    @Test
    fun getDisplayText_formats_zeroPadded() {
        assertEquals("2024-06", DateSelectionEntity.ByMonth(YearMonth(2024, 6)).getDisplayText())
        assertEquals("2024-06-05", DateSelectionEntity.ByDay(LocalDate(2024, 6, 5)).getDisplayText())
        assertEquals("2024", DateSelectionEntity.ByYear(2024).getDisplayText())
        assertEquals(
            "2024-01-01~2024-03-31",
            DateSelectionEntity.DateRange(LocalDate(2024, 1, 1), LocalDate(2024, 3, 31)).getDisplayText(),
        )
        assertEquals("全部", DateSelectionEntity.All.getDisplayText())
    }

    /** getDisplayText ↔ fromDisplayTextOrNull 往返（scope 指定的"往返"维度，此前全仓未断言）。 */
    @Test
    fun displayText_roundTrips() {
        val cases = listOf(
            DateSelectionEntity.ByDay(LocalDate(2024, 6, 5)),
            DateSelectionEntity.ByMonth(YearMonth(2024, 6)),
            DateSelectionEntity.ByYear(2024),
            DateSelectionEntity.DateRange(LocalDate(2024, 1, 1), LocalDate(2024, 3, 31)),
            DateSelectionEntity.All,
        )
        for (sel in cases) {
            assertEquals(sel, DateSelectionEntity.fromDisplayTextOrNull(sel.getDisplayText()))
        }
    }
}
