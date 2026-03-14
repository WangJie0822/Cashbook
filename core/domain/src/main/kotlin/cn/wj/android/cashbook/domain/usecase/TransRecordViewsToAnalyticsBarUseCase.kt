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
import cn.wj.android.cashbook.core.common.ext.yearMonth
import cn.wj.android.cashbook.core.common.tools.toLocalDate
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordBarEntity
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.enums.AnalyticsBarGranularity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_INCOME
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TransRecordViewsToAnalyticsBarUseCase @Inject constructor(
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        dateSelection: DateSelectionEntity,
        recordViewsList: List<RecordViewsModel>,
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
                val years = recordViewsList.map { it.recordTime.toLocalDate().year }.distinct().sorted()
                years.forEach { year ->
                    dateList.add("$year")
                }
            }

            is DateSelectionEntity.ByDay -> {
                granularity = AnalyticsBarGranularity.DAY
                val date = dateSelection.date
                dateList.add("${date.year}-${date.monthValue.completeZero()}-${date.dayOfMonth.completeZero()}")
            }

            is DateSelectionEntity.ByMonth -> {
                granularity = AnalyticsBarGranularity.DAY
                val ym = dateSelection.yearMonth
                val dayCount = ym.atEndOfMonth().dayOfMonth
                repeat(dayCount) {
                    dateList.add("${ym.year}-${ym.monthValue.completeZero()}-${(it + 1).completeZero()}")
                }
            }

            is DateSelectionEntity.DateRange -> {
                granularity = AnalyticsBarGranularity.DAY
                var date = dateSelection.from
                val toDate = dateSelection.to
                while (date != toDate) {
                    dateList.add("${date.year}-${date.monthValue.completeZero()}-${date.dayOfMonth.completeZero()}")
                    date = date.plusDays(1L)
                }
                dateList.add("${toDate.year}-${toDate.monthValue.completeZero()}-${toDate.dayOfMonth.completeZero()}")
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
                if (record.type.id == RECORD_TYPE_BALANCE_EXPENDITURE.id || record.type.id == RECORD_TYPE_BALANCE_INCOME.id) {
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
