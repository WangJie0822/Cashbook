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

import cn.wj.android.cashbook.core.model.enums.DateSelectionTypeEnum
import java.time.LocalDate
import java.time.YearMonth

/**
 * 日期选择结果密封类
 */
sealed class DateSelectionEntity(val type: DateSelectionTypeEnum) {

    /** 按日选择 */
    data class ByDay(val date: LocalDate) : DateSelectionEntity(DateSelectionTypeEnum.BY_DAY)

    /** 按月选择 */
    data class ByMonth(val yearMonth: YearMonth) : DateSelectionEntity(DateSelectionTypeEnum.BY_MONTH)

    /** 按年选择 */
    data class ByYear(val year: Int) : DateSelectionEntity(DateSelectionTypeEnum.BY_YEAR)

    /** 时间范围选择 */
    data class DateRange(
        val from: LocalDate,
        val to: LocalDate,
    ) : DateSelectionEntity(DateSelectionTypeEnum.DATE_RANGE)

    /** 全部 */
    data object All : DateSelectionEntity(DateSelectionTypeEnum.ALL)

    /** 获取显示文本 */
    fun getDisplayText(): String = when (this) {
        is ByDay -> "${date.year}-${date.monthValue.toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}"
        is ByMonth -> "${yearMonth.year}-${yearMonth.monthValue.toString().padStart(2, '0')}"
        is ByYear -> "$year"
        is DateRange -> "${from.year}-${from.monthValue.toString().padStart(2, '0')}-${from.dayOfMonth.toString().padStart(2, '0')}~${to.year}-${to.monthValue.toString().padStart(2, '0')}-${to.dayOfMonth.toString().padStart(2, '0')}"
        is All -> "全部"
    }
}
