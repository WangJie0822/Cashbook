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

import cn.wj.android.cashbook.core.common.NO_ASSET_ID
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.ScheduleRepository
import cn.wj.android.cashbook.core.model.enums.ScheduleFrequencyEnum
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.ScheduleModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class GenerateScheduleRecordsUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val saveRecordUseCase: SaveRecordUseCase,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(currentTimeMs: Long = System.currentTimeMillis()): Int =
        withContext(coroutineContext) {
            val schedules = scheduleRepository.scheduleListData.first()
                .filter { it.enabled }

            var generatedCount = 0

            schedules.forEach { schedule ->
                val dueDates = calculateDueDates(schedule, currentTimeMs)
                dueDates.forEach { dueDate ->
                    val record = RecordModel(
                        id = -1L,
                        booksId = schedule.booksId,
                        typeId = schedule.typeId,
                        assetId = schedule.assetId,
                        relatedAssetId = NO_ASSET_ID,
                        amount = schedule.amount,
                        finalAmount = 0L,
                        charges = schedule.charges,
                        concessions = schedule.concessions,
                        remark = schedule.remark,
                        reimbursable = schedule.reimbursable,
                        recordTime = combineDateAndTime(dueDate, schedule.recordTime),
                        scheduleId = schedule.id,
                    )
                    saveRecordUseCase(
                        recordModel = record,
                        tagIdList = schedule.tagIdList,
                        relatedRecordIdList = emptyList(),
                        relatedImageList = emptyList(),
                    )
                    generatedCount++
                }
                if (dueDates.isNotEmpty()) {
                    scheduleRepository.updateLastExecutedDate(schedule.id, dueDates.last())
                }
            }
            generatedCount
        }

    private fun calculateDueDates(schedule: ScheduleModel, currentTimeMs: Long): List<Long> {
        val dueDates = mutableListOf<Long>()

        val lastExecuted = schedule.lastExecutedDate
        val baseDate = if (lastExecuted != null) {
            // 已执行过，以上次执行日期为基准
            lastExecuted
        } else {
            // 从未执行过，以 startDate 的前一个周期为基准，
            // 这样第一次 add 就得到 startDate 本身
            val cal = Calendar.getInstance().apply { timeInMillis = schedule.startDate }
            when (schedule.frequency) {
                ScheduleFrequencyEnum.DAILY -> cal.add(Calendar.DAY_OF_MONTH, -1)
                ScheduleFrequencyEnum.WEEKLY -> cal.add(Calendar.DAY_OF_MONTH, -7)
                ScheduleFrequencyEnum.MONTHLY -> cal.add(Calendar.MONTH, -1)
                ScheduleFrequencyEnum.YEARLY -> cal.add(Calendar.YEAR, -1)
            }
            cal.timeInMillis
        }

        // 去掉时间部分，只保留日期
        val baseCalendar = Calendar.getInstance().apply {
            timeInMillis = baseDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentCalendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var nextCalendar = Calendar.getInstance().apply { timeInMillis = baseCalendar.timeInMillis }

        while (true) {
            when (schedule.frequency) {
                ScheduleFrequencyEnum.DAILY -> nextCalendar.add(Calendar.DAY_OF_MONTH, 1)
                ScheduleFrequencyEnum.WEEKLY -> nextCalendar.add(Calendar.DAY_OF_MONTH, 7)
                ScheduleFrequencyEnum.MONTHLY -> nextCalendar.add(Calendar.MONTH, 1)
                ScheduleFrequencyEnum.YEARLY -> nextCalendar.add(Calendar.YEAR, 1)
            }

            // 检查是否已超过当前时间
            if (nextCalendar.after(currentCalendar)) {
                break
            }

            // 检查是否超过结束日期
            val endDate = schedule.endDate
            if (endDate != null && nextCalendar.timeInMillis > endDate) {
                break
            }

            dueDates.add(nextCalendar.timeInMillis)
        }

        return dueDates
    }

    private fun combineDateAndTime(date: Long, time: Long): Long {
        val dateCalendar = Calendar.getInstance().apply { timeInMillis = date }
        val timeCalendar = Calendar.getInstance().apply { timeInMillis = time }
        dateCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
        dateCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
        dateCalendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND))
        dateCalendar.set(Calendar.MILLISECOND, timeCalendar.get(Calendar.MILLISECOND))
        return dateCalendar.timeInMillis
    }
}
