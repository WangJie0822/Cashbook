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
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
import cn.wj.android.cashbook.core.common.ext.yearMonth
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordBarEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TransRecordViewsToAnalyticsBarUseCase @Inject constructor(
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        fromDate: LocalDate,
        toDate: LocalDate?,
        yearSelected: Boolean,
        recordViewsList: List<RecordViewsModel>,
    ): List<AnalyticsRecordBarEntity> = withContext(coroutineContext) {
        val result = mutableListOf<AnalyticsRecordBarEntity>()
        val dateList = mutableListOf<String>()
        when {
            yearSelected -> {
                repeat(12) {
                    dateList.add("${fromDate.year}-${(it + 1).completeZero()}")
                }
            }

            null == toDate -> {
                val dayCount = fromDate.yearMonth.atEndOfMonth().dayOfMonth
                repeat(dayCount) {
                    dateList.add("${fromDate.year}-${fromDate.monthValue.completeZero()}-${(it + 1).completeZero()}")
                }
            }

            else -> {
                var date = fromDate
                while (date != toDate) {
                    dateList.add("${date.year}-${date.monthValue.completeZero()}-${date.dayOfMonth.completeZero()}")
                    date = date.plusDays(1L)
                }
                dateList.add("${toDate.year}-${toDate.monthValue.completeZero()}-${toDate.dayOfMonth.completeZero()}")
            }
        }
        dateList.forEach { date ->
            var totalExpenditure = 0.0
            var totalIncome = 0.0
            recordViewsList.filter {
                date == if (yearSelected) {
                    val dateArray = it.recordTime.split(" ").first().split("-")
                    "${dateArray[0]}-${dateArray[1]}"
                } else {
                    it.recordTime.split(" ").first()
                }
            }.forEach { record ->
                when (record.type.typeCategory) {
                    RecordTypeCategoryEnum.EXPENDITURE -> {
                        // 支出
                        totalExpenditure += record.finalAmount.toDoubleOrZero()
                    }

                    RecordTypeCategoryEnum.INCOME -> {
                        // 收入
                        totalIncome += record.finalAmount.toDoubleOrZero()
                    }

                    RecordTypeCategoryEnum.TRANSFER -> {
                        // 转账
                        totalExpenditure += record.charges.toDoubleOrZero()
                        totalIncome += record.concessions.toDoubleOrZero()
                    }
                }
            }
            result.add(
                AnalyticsRecordBarEntity(
                    date = date,
                    income = totalIncome.decimalFormat(),
                    expenditure = totalExpenditure.decimalFormat(),
                    balance = (totalIncome - totalExpenditure).decimalFormat(),
                    year = yearSelected,
                ),
            )
        }
        this@TransRecordViewsToAnalyticsBarUseCase.logger().i("result = <$result>")
        result
    }
}
