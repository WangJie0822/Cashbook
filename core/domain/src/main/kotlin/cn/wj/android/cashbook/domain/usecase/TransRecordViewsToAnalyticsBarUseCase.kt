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

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.completeZero
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.tools.toLocalDate
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordBarEntity
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.entity.normalizeMonthStartDay
import cn.wj.android.cashbook.core.model.enums.AnalyticsBarGranularity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.number
import kotlinx.datetime.onDay
import kotlinx.datetime.plus
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TransRecordViewsToAnalyticsBarUseCase @Inject constructor(
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        dateSelection: DateSelectionEntity,
        recordViewsList: List<RecordViewsModel>,
        monthStartDay: Int = 1,
    ): List<AnalyticsRecordBarEntity> = withContext(coroutineContext) {
        val result = mutableListOf<AnalyticsRecordBarEntity>()
        val dateList = mutableListOf<String>()
        val granularity: AnalyticsBarGranularity

        when (dateSelection) {
            is DateSelectionEntity.ByYear -> {
                granularity = AnalyticsBarGranularity.MONTH
                repeat(12) {
                    dateList.add("${dateSelection.year}-${(it + 1).completeZero()}")
                }
            }

            is DateSelectionEntity.All -> {
                granularity = AnalyticsBarGranularity.YEAR
                // 从记录中提取年份范围
                val years =
                    recordViewsList.map { it.recordTime.toLocalDate().year }.distinct().sorted()
                years.forEach { year ->
                    dateList.add("$year")
                }
            }

            is DateSelectionEntity.ByDay -> {
                granularity = AnalyticsBarGranularity.DAY
                val date = dateSelection.date
                dateList.add("${date.year}-${date.month.number.completeZero()}-${date.day.completeZero()}")
            }

            is DateSelectionEntity.ByMonth -> {
                granularity = AnalyticsBarGranularity.DAY
                val d = normalizeMonthStartDay(monthStartDay)
                val ym = dateSelection.yearMonth
                var date = ym.onDay(d)
                val endExclusive = ym.plus(1, DateTimeUnit.MONTH).onDay(d)
                while (date < endExclusive) {
                    dateList.add("${date.year}-${date.month.number.completeZero()}-${date.day.completeZero()}")
                    date = date.plus(1, DateTimeUnit.DAY)
                }
            }

            is DateSelectionEntity.DateRange -> {
                granularity = AnalyticsBarGranularity.DAY
                var date = dateSelection.from
                val toDate = dateSelection.to
                while (date != toDate) {
                    dateList.add("${date.year}-${date.month.number.completeZero()}-${date.day.completeZero()}")
                    date = date.plus(1, DateTimeUnit.DAY)
                }
                dateList.add("${toDate.year}-${toDate.month.number.completeZero()}-${toDate.day.completeZero()}")
            }
        }
        dateList.forEach { date ->
            var totalExpenditure = 0L
            var totalIncome = 0L
            recordViewsList.filter {
                val recordDate = it.recordTime.toLocalDate()
                date == when (granularity) {
                    AnalyticsBarGranularity.YEAR -> "${recordDate.year}"
                    AnalyticsBarGranularity.MONTH -> "${recordDate.year}-${recordDate.monthValue.completeZero()}"
                    AnalyticsBarGranularity.DAY -> "${recordDate.year}-${recordDate.monthValue.completeZero()}-${recordDate.dayOfMonth.completeZero()}"
                }
            }.forEach { record ->
                // 跳过平账记录
                if (record.isBalanceRecord) {
                    return@forEach
                }
                when (record.type.typeCategory) {
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
            result.add(
                AnalyticsRecordBarEntity(
                    date = date,
                    income = totalIncome,
                    expenditure = totalExpenditure,
                    balance = totalIncome - totalExpenditure,
                    granularity = granularity,
                ),
            )
        }
        this@TransRecordViewsToAnalyticsBarUseCase.logger().i("result = <$result>")
        result
    }
}
