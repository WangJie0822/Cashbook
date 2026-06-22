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

class DateSelectionEntityTest {

    @Test
    fun fromDisplayTextOrNull_month() {
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("2024-06"))
            .isEqualTo(DateSelectionEntity.ByMonth(YearMonth.of(2024, 6)))
    }

    @Test
    fun fromDisplayTextOrNull_year() {
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("2024"))
            .isEqualTo(DateSelectionEntity.ByYear(2024))
    }

    @Test
    fun fromDisplayTextOrNull_range() {
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("2024-01-01~2024-03-31"))
            .isEqualTo(
                DateSelectionEntity.DateRange(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 3, 31),
                ),
            )
    }

    @Test
    fun fromDisplayTextOrNull_all() {
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("全部")).isEqualTo(DateSelectionEntity.All)
    }

    @Test
    fun fromDisplayTextOrNull_blank_returns_null() {
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("")).isNull()
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("   ")).isNull()
    }

    @Test
    fun fromDisplayTextOrNull_invalid_returns_null() {
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("2024-13")).isNull()
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("abc")).isNull()
    }
}
