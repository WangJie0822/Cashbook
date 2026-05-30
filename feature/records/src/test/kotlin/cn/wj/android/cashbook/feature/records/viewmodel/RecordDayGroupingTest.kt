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
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [recordDaySeparator] 按日分组分隔逻辑单元测试。
 *
 * 断言均基于实际格式化结果（与时区无关），不硬编码日期字符串。
 */
class RecordDayGroupingTest {

    /** 2024-01-15 08:00 CST 附近 */
    private val timeDay1 = 1705276800000L

    /** 2024-01-16 08:00 CST 附近（与 [timeDay1] 相差一天） */
    private val timeDay2 = 1705363200000L

    @Test
    fun when_before_is_null_then_returns_header_for_after() {
        val after = record(id = 1L, recordTime = timeDay1)

        val header = recordDaySeparator(before = null, after = after)

        assertThat(header).isNotNull()
        val expectedDate = timeDay1.dateFormat(DATE_FORMAT_DATE)
        assertThat(header!!.dateStr).isEqualTo(expectedDate)
        assertThat(header.day).isEqualTo(expectedDate.split("-").last().toInt())
    }

    @Test
    fun when_same_day_then_returns_null() {
        val before = record(id = 1L, recordTime = timeDay1)
        val after = record(id = 2L, recordTime = timeDay1)

        assertThat(recordDaySeparator(before = before, after = after)).isNull()
    }

    @Test
    fun when_different_day_then_returns_header_for_after() {
        val before = record(id = 1L, recordTime = timeDay1)
        val after = record(id = 2L, recordTime = timeDay2)

        val header = recordDaySeparator(before = before, after = after)

        assertThat(header).isNotNull()
        assertThat(header!!.dateStr).isEqualTo(timeDay2.dateFormat(DATE_FORMAT_DATE))
    }

    @Test
    fun when_after_is_null_then_returns_null() {
        val before = record(id = 1L, recordTime = timeDay1)

        assertThat(recordDaySeparator(before = before, after = null)).isNull()
    }

    @Test
    fun when_after_is_header_then_returns_null() {
        val before = record(id = 1L, recordTime = timeDay1)
        val after = LauncherListItem.DayHeader(dateStr = "2024-01-15", day = 15, dayType = 1)

        assertThat(recordDaySeparator(before = before, after = after)).isNull()
    }

    @Test
    fun when_before_is_header_then_returns_header_for_after() {
        // before 为日期头（非记录），after 为记录 → 视作新的一天，需要插入日期头
        val before = LauncherListItem.DayHeader(dateStr = "2024-01-14", day = 14, dayType = 1)
        val after = record(id = 1L, recordTime = timeDay1)

        val header = recordDaySeparator(before = before, after = after)

        assertThat(header).isNotNull()
        assertThat(header!!.dateStr).isEqualTo(timeDay1.dateFormat(DATE_FORMAT_DATE))
    }

    private fun record(id: Long, recordTime: Long): LauncherListItem.Record =
        LauncherListItem.Record(
            RecordViewsEntity(
                id = id,
                typeId = 1L,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                typeName = "餐饮",
                typeIconResName = "ic_type_food",
                assetId = 1L,
                assetName = "现金",
                assetIconResId = 0,
                relatedAssetId = null,
                relatedAssetName = null,
                relatedAssetIconResId = null,
                amount = 10000L,
                finalAmount = 10000L,
                charges = 0L,
                concessions = 0L,
                remark = "",
                reimbursable = false,
                relatedTags = emptyList(),
                relatedImage = emptyList(),
                relatedRecord = emptyList(),
                relatedAmount = 0L,
                recordTime = recordTime,
            ),
        )
}
