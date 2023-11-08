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

package cn.wj.android.cashbook.core.common.util

import cn.wj.android.cashbook.core.common.ext.completeZero
import java.time.LocalDate
import java.util.Calendar

/**
 * 农历相关工具类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/7
 */
object LunarUtils {

    /** 获取公历节假日 */
    private fun getGregorianFestival(date: LocalDate): String? {
        val dateText = date.monthValue.completeZero() + date.dayOfMonth.completeZero()
        return GREGORIAN_FESTIVAL.firstOrNull { it.contains(dateText) }?.replace(dateText, "")
    }

    private fun getBitInt(data: Int, length: Int, shift: Int): Int {
        return data and ((1 shl length) - 1 shl shift) shr shift
    }

    private fun solarToInt(y: Int, m: Int, d: Int): Long {
        var yTemp = y
        var mTemp = m
        mTemp = (mTemp + 9) % 12
        yTemp -= mTemp / 10
        return (365 * yTemp + yTemp / 4 - yTemp / 100 + yTemp / 400 + (mTemp * 306 + 5) / 10 + (d - 1)).toLong()
    }

    private fun solarToLunar(date: LocalDate): LunarDate {
        val year = date.year
        val month = date.monthValue
        val day = date.dayOfMonth
        var index: Int = year - SOLAR[0]
        val data = year shl 9 or (month shl 5) or day
        if (SOLAR[index] > data) {
            index--
        }
        val solar11 = SOLAR[index]
        val y: Int = getBitInt(solar11, 12, 9)
        val m: Int = getBitInt(solar11, 4, 5)
        val d: Int = getBitInt(solar11, 5, 0)
        var offset: Long = solarToInt(year, month, day) - solarToInt(y, m, d)
        val days: Int = LUNAR_MONTH_DAYS[index]
        val leap: Int = getBitInt(days, 4, 13)

        val lunarY: Int = index + SOLAR[0]
        var lunarM = 1
        offset += 1

        for (i in 0..12) {
            val dm = if (getBitInt(days, 1, 12 - i) == 1) 30 else 29
            offset -= if (offset > dm) {
                lunarM++
                dm.toLong()
            } else {
                break
            }
        }

        var lunarMonth = lunarM
        val lunarDay = offset.toInt()
        var leapMonth = false

        if (leap != 0 && lunarM > leap) {
            lunarMonth = lunarM - 1
            if (lunarM == leap + 1) {
                leapMonth = true
            }
        }
        return LunarDate(lunarY, lunarMonth, lunarDay, leapMonth)
    }

    /**
     * 农历 year年month月的总天数，总共有13个月包括闰月
     *
     * @param year  将要计算的年份
     * @param month 将要计算的月份
     * @return 传回农历 year年month月的总天数
     */
    private fun daysInLunarMonth(year: Int, month: Int): Int {
        return if (LUNAR_INFO[year - MIN_YEAR] and (0x10000 shr month) == 0) 29 else 30
    }

    /** 获取农历节假日 */
    private fun getTraditionFestival(date: LunarDate): String? {
        if (date.month == 12) {
            val count: Int = daysInLunarMonth(date.year, date.month)
            if (date.day == count) {
                return TRADITION_FESTIVAL_STR[0] // 除夕
            }
        }
        val dateText = date.month.completeZero() + date.day.completeZero()
        return TRADITION_FESTIVAL_STR.firstOrNull {
            it.contains(dateText)
        }?.replace(dateText, "")
    }

    private fun getChineseDate(date: LunarDate): String {
        return if (date.day == 1) {
            if (date.leapMonth) {
                "闰${MONTH_STR[date.month - 1]}"
            } else {
                MONTH_STR[date.month - 1]
            }
        } else {
            DAY_STR[date.day - 1]
        }
    }

    private fun dateToString(year: Int, month: Int, day: Int): String {
        return "$year${month.completeZero()}${day.completeZero()}"
    }

    private val specialFestival: MutableMap<Int, Array<String?>> by lazy {
        mutableMapOf()
    }

    /**
     * 获取特殊计算方式的节日
     * 如：每年五月的第二个星期日为母亲节，六月的第三个星期日为父亲节
     * 每年11月第四个星期四定为"感恩节"
     *
     * @param date [LocalDate]
     * @return 获取西方节日
     */
    private fun getSpecialFestival(date: LocalDate): String? {
        if (!specialFestival.containsKey(date.year)) {
            specialFestival[date.year] = getSpecialFestivals(date.year)
        }
        val specialFestivals: Array<String?> = specialFestival[date.year]!!
        val dateText = dateToString(date.year, date.monthValue, date.dayOfMonth)
        return specialFestivals.firstOrNull {
            it?.contains(dateText) == true
        }?.replace(dateText, "")
    }

    /**
     * 获取每年的母亲节和父亲节和感恩节
     * 特殊计算方式的节日
     *
     * @param year 年
     * @return 获取每年的母亲节和父亲节、感恩节
     */
    private fun getSpecialFestivals(year: Int): Array<String?> {
        val festivals = arrayOfNulls<String>(3)
        val date = Calendar.getInstance()
        date[year, 4] = 1
        var week = date[Calendar.DAY_OF_WEEK]
        var startDiff = 7 - week + 1
        if (startDiff == 7) {
            festivals[0] = dateToString(year, 5, startDiff + 1) + SPECIAL_FESTIVAL_STR[0]
        } else {
            festivals[0] = dateToString(year, 5, startDiff + 7 + 1) + SPECIAL_FESTIVAL_STR[0]
        }
        date[year, 5] = 1
        week = date[Calendar.DAY_OF_WEEK]
        startDiff = 7 - week + 1
        if (startDiff == 7) {
            festivals[1] = dateToString(year, 6, startDiff + 7 + 1) + SPECIAL_FESTIVAL_STR[1]
        } else {
            festivals[1] =
                dateToString(year, 6, startDiff + 7 + 7 + 1) + SPECIAL_FESTIVAL_STR[1]
        }
        date[year, 10] = 1
        week = date[Calendar.DAY_OF_WEEK]
        startDiff = 7 - week + 1
        if (startDiff <= 2) {
            festivals[2] = dateToString(year, 11, startDiff + 21 + 5) + SPECIAL_FESTIVAL_STR[2]
        } else {
            festivals[2] = dateToString(year, 11, startDiff + 14 + 5) + SPECIAL_FESTIVAL_STR[2]
        }
        return festivals
    }

    /**
     * 获取节假日、农历信息
     *
     * 优先级：农历节日 > 农历节气 > 公历特殊节日 > 公历节日 > 农历日期
     */
    fun getLunarTextWithFestival(date: LocalDate): String {
        val lunarDate = solarToLunar(date)
        return getTraditionFestival(lunarDate) ?: getSpecialFestival(date)
            ?: getGregorianFestival(date) ?: getChineseDate(lunarDate)
    }
}

data class LunarDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val leapMonth: Boolean,
)
