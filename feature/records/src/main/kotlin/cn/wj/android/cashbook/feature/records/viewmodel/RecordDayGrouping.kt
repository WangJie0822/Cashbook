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

package cn.wj.android.cashbook.feature.records.viewmodel

import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_DATE
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.model.enums.DateSelectionTypeEnum
import java.util.Calendar

/**
 * 记录列表按日分组的分隔逻辑：当 [after] 与 [before] 不在同一天时，在二者之间插入一个日期头 [LauncherListItem.DayHeader]。
 *
 * 供首页 [LauncherContentViewModel] 与资产详情 [AssetInfoContentViewModel] 复用，避免重复实现。
 *
 * @param before 前一项（可能为 null 或日期头）
 * @param after 后一项（可能为 null 或日期头）
 * @return 需要插入的日期头；无需插入时返回 null
 */
internal fun recordDaySeparator(
    before: LauncherListItem?,
    after: LauncherListItem?,
): LauncherListItem.DayHeader? {
    val afterRecord = (after as? LauncherListItem.Record)?.entity ?: return null
    val beforeRecord = (before as? LauncherListItem.Record)?.entity
    val afterDate = afterRecord.recordTime.dateFormat(DATE_FORMAT_DATE)
    val beforeDate = beforeRecord?.recordTime?.dateFormat(DATE_FORMAT_DATE)
    if (afterDate == beforeDate) {
        return null
    }
    val dateArray = afterDate.split("-")
    val day = dateArray.last().toIntOrNull() ?: 0
    val dayType = computeDayType(dateArray)
    return LauncherListItem.DayHeader(
        dateStr = afterDate,
        day = day,
        dayType = dayType,
    )
}

/**
 * 计算日期类型：0=今天，-1=昨天，-2=前天，1=其它。
 *
 * @param dateArray 形如 ["yyyy", "MM", "dd"] 的日期分段
 */
internal fun computeDayType(dateArray: List<String>): Int {
    val calendar = Calendar.getInstance()
    val currentYear = calendar[Calendar.YEAR]
    val currentMonth = calendar[Calendar.MONTH] + 1
    val currentDay = calendar[Calendar.DAY_OF_MONTH]
    val dateDay = dateArray.last().toIntOrNull() ?: return 1
    return if (currentYear == dateArray[0].toIntOrNull() && currentMonth == dateArray[1].toIntOrNull()) {
        when (dateDay) {
            currentDay -> 0
            currentDay - 1 -> -1
            currentDay - 2 -> -2
            else -> 1
        }
    } else {
        1
    }
}

/**
 * 记录日期头文案（首页 [LauncherListItem.DayHeader] 渲染与分类/资产/标签统计页共用，单一真源）。
 *
 * 按周期类型决定是否带月/年上下文；BY_MONTH 在月起始日≠1（周期跨自然月）时也带月份。
 * 纯函数：所有本地化字符串（[dayLabel]/[monthLabel]/[yearLabel]/[dayTypeSuffix]）由 @Composable
 * 调用方解析后传入，故本函数不依赖 Compose、可纯 JVM 单测。
 *
 * 沿用首页既有口径：BY_YEAR 带 [dayTypeSuffix]，DATE_RANGE/ALL 不带（本次不统一）。
 *
 * @param dayTypeSuffix 已解析的「(今天)/(昨天)/(前天)」或空串
 * @param byMonthCrossesNaturalMonth BY_MONTH 周期是否跨自然月（= 归一化后 monthStartDay≠1）
 */
internal fun recordDayHeaderDateText(
    type: DateSelectionTypeEnum,
    year: Int,
    month: Int,
    day: Int,
    dayTypeSuffix: String,
    dayLabel: String,
    monthLabel: String,
    yearLabel: String,
    byMonthCrossesNaturalMonth: Boolean,
): String = when (type) {
    DateSelectionTypeEnum.BY_DAY ->
        "$day$dayLabel$dayTypeSuffix"

    DateSelectionTypeEnum.BY_MONTH ->
        if (byMonthCrossesNaturalMonth) {
            "$month$monthLabel$day$dayLabel$dayTypeSuffix"
        } else {
            "$day$dayLabel$dayTypeSuffix"
        }

    DateSelectionTypeEnum.BY_YEAR ->
        "$month$monthLabel$day$dayLabel$dayTypeSuffix"

    DateSelectionTypeEnum.DATE_RANGE, DateSelectionTypeEnum.ALL ->
        "$year$yearLabel$month$monthLabel$day$dayLabel"
}
