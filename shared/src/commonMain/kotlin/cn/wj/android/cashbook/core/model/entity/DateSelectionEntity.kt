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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.onDay
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth

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

    /**
     * 将日期选择转换为时间戳范围（毫秒），使用半开区间 [start, end)。
     *
     * [monthStartDay] 仅影响 [ByMonth]：周期为 yearMonth.onDay(D) 到 yearMonth.plus(1, MONTH).onDay(D) 的半开区间；
     * D=1（默认）时与自然月一致。非法 D 经 [normalizeMonthStartDay] 归一化为 1。
     *
     * @return Pair<起始时间戳, 结束时间戳>
     */
    fun toDateRange(monthStartDay: Int = 1): Pair<Long, Long> {
        val zone = TimeZone.currentSystemDefault()
        return when (this) {
            is ByDay -> {
                val start = date.atStartOfDayIn(zone).toEpochMilliseconds()
                val end = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()
                start to end
            }

            is ByMonth -> {
                val d = normalizeMonthStartDay(monthStartDay)
                val start = yearMonth.onDay(d).atStartOfDayIn(zone).toEpochMilliseconds()
                val end = yearMonth.plus(1, DateTimeUnit.MONTH).onDay(d).atStartOfDayIn(zone).toEpochMilliseconds()
                start to end
            }

            is ByYear -> {
                val start = LocalDate(year, 1, 1).atStartOfDayIn(zone).toEpochMilliseconds()
                val end = LocalDate(year + 1, 1, 1).atStartOfDayIn(zone).toEpochMilliseconds()
                start to end
            }

            is DateRange -> {
                val start = from.atStartOfDayIn(zone).toEpochMilliseconds()
                val end = to.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()
                start to end
            }

            is All -> {
                0L to Long.MAX_VALUE
            }
        }
    }

    /** 获取显示文本 */
    fun getDisplayText(): String = when (this) {
        is ByDay -> "${date.year}-${date.month.number.toString().padStart(2, '0')}-${date.day.toString().padStart(2, '0')}"
        is ByMonth -> "${yearMonth.year}-${yearMonth.month.number.toString().padStart(2, '0')}"
        is ByYear -> "$year"
        is DateRange -> "${from.year}-${from.month.number.toString().padStart(2, '0')}-${from.day.toString().padStart(2, '0')}~${to.year}-${to.month.number.toString().padStart(2, '0')}-${to.day.toString().padStart(2, '0')}"
        is All -> "全部"
    }

    companion object {
        /**
         * 将 [getDisplayText] 产出的显示文本逆向解析为 [DateSelectionEntity]。
         * 空白/非法 → null；`全部` → [All]；`YYYY-MM` → [ByMonth]；`YYYY` → [ByYear]；
         * `YYYY-MM-DD~YYYY-MM-DD` → [DateRange]；`YYYY-MM-DD` → [ByDay]。
         */
        fun fromDisplayTextOrNull(text: String): DateSelectionEntity? {
            val s = text.trim()
            if (s.isBlank()) return null
            if (s == "全部") return All
            return runCatching {
                if (s.contains("~")) {
                    val parts = s.split("~", limit = 2)
                    DateRange(LocalDate.parse(parts[0].trim()), LocalDate.parse(parts[1].trim()))
                } else {
                    val seg = s.split("-")
                    when (seg.size) {
                        1 -> ByYear(seg[0].toInt())
                        2 -> ByMonth(YearMonth(seg[0].toInt(), seg[1].toInt()))
                        3 -> ByDay(LocalDate(seg[0].toInt(), seg[1].toInt(), seg[2].toInt()))
                        else -> null
                    }
                }
            }.getOrNull()
        }

        /**
         * 推导包含 [today] 的当前月周期（以 [monthStartDay] 为每月起点）。
         * today.day >= D → 本月 label；否则 → 上月 label（跨年由 [YearMonth.minus] 处理）。
         */
        fun currentMonthPeriod(today: LocalDate, monthStartDay: Int): ByMonth {
            val d = normalizeMonthStartDay(monthStartDay)
            val ym = today.yearMonth
            return if (today.day >= d) ByMonth(ym) else ByMonth(ym.minus(1, DateTimeUnit.MONTH))
        }
    }
}

/** 归一化月起始日：合法范围 1..28，否则回落为 1（自然月）。 */
fun normalizeMonthStartDay(raw: Int): Int = if (raw in 1..28) raw else 1
