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
