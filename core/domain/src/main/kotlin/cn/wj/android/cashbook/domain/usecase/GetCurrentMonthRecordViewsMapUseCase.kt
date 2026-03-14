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

package cn.wj.android.cashbook.domain.usecase

import android.util.ArrayMap
import cn.wj.android.cashbook.core.common.tools.toLocalDate
import cn.wj.android.cashbook.core.model.entity.RecordDayEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import java.time.LocalDate
import java.util.Calendar
import javax.inject.Inject

/**
 * 获取当前月显示数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/22
 */
class GetCurrentMonthRecordViewsMapUseCase @Inject constructor() {

    operator fun invoke(list: List<RecordViewsEntity>): Map<RecordDayEntity, List<RecordViewsEntity>> {
        val map = ArrayMap<LocalDate, ArrayList<RecordViewsEntity>>()
        list.sortedByDescending { it.recordTime }
            .forEach {
                val key = it.recordTime.toLocalDate()
                if (map.containsKey(key)) {
                    map[key]?.add(it)
                } else {
                    map[key] = arrayListOf(it)
                }
            }
        return map.mapKeys {
            var totalExpenditure = 0L
            var totalIncome = 0L
            it.value.forEach { record ->
                // 跳过平账记录
                if (record.isBalanceAccount) {
                    return@forEach
                }
                when (record.typeCategory) {
                    RecordTypeCategoryEnum.EXPENDITURE -> {
                        // 支出
                        totalExpenditure += record.finalAmount
                    }

                    RecordTypeCategoryEnum.INCOME -> {
                        // 收入
                        totalIncome += record.finalAmount
                    }

                    RecordTypeCategoryEnum.TRANSFER -> {
                        // 转账，优惠冲减支出
                        totalExpenditure += record.charges - record.concessions
                    }
                }
            }
            val date = it.key
            val calendar = Calendar.getInstance()
            val currentYear = calendar[Calendar.YEAR]
            val currentMonth = (calendar[Calendar.MONTH] + 1)
            val currentDay = calendar[Calendar.DAY_OF_MONTH]
            val dateDay = date.dayOfMonth
            val dayType =
                if (currentYear == date.year && currentMonth == date.monthValue) {
                    when (dateDay) {
                        currentDay -> 0
                        currentDay - 1 -> -1
                        currentDay - 2 -> -2
                        else -> 1
                    }
                } else {
                    1
                }
            RecordDayEntity(
                day = dateDay,
                dayType = dayType,
                dayIncome = totalIncome,
                dayExpand = totalExpenditure,
            )
        }
    }
}
