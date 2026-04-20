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

package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.table.ScheduleTable
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.ScheduleFrequencyEnum
import cn.wj.android.cashbook.core.model.model.ScheduleModel
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {

    val scheduleListData: Flow<List<ScheduleModel>>

    suspend fun queryById(scheduleId: Long): ScheduleModel?

    suspend fun saveSchedule(schedule: ScheduleModel): Long

    suspend fun updateScheduleEnabled(scheduleId: Long, enabled: Boolean)

    suspend fun updateLastExecutedDate(scheduleId: Long, lastExecutedDate: Long)

    suspend fun deleteSchedule(scheduleId: Long)
}

internal fun ScheduleTable.asModel(): ScheduleModel {
    return ScheduleModel(
        id = this.id ?: -1L,
        booksId = this.booksId,
        typeId = this.typeId,
        assetId = this.assetId,
        amount = this.amount,
        charges = this.charge,
        concessions = this.concessions,
        remark = this.remark,
        typeCategory = RecordTypeCategoryEnum.ordinalOf(this.typeCategory),
        frequency = ScheduleFrequencyEnum.ordinalOf(this.frequency),
        startDate = this.startDate,
        endDate = this.endDate,
        recordTime = this.recordTime,
        lastExecutedDate = this.lastExecutedDate,
        enabled = this.enabled == SWITCH_INT_ON,
        reimbursable = this.reimbursable == SWITCH_INT_ON,
        tagIdList = this.tagIds.split(",").filter { it.isNotBlank() }.map { it.toLong() },
    )
}

internal fun ScheduleModel.asTable(): ScheduleTable {
    return ScheduleTable(
        id = if (this.id == -1L) null else this.id,
        booksId = this.booksId,
        typeId = this.typeId,
        assetId = this.assetId,
        amount = this.amount,
        charge = this.charges,
        concessions = this.concessions,
        remark = this.remark,
        typeCategory = this.typeCategory.ordinal,
        frequency = this.frequency.ordinal,
        startDate = this.startDate,
        endDate = this.endDate,
        recordTime = this.recordTime,
        lastExecutedDate = this.lastExecutedDate,
        enabled = if (this.enabled) SWITCH_INT_ON else SWITCH_INT_OFF,
        reimbursable = if (this.reimbursable) SWITCH_INT_ON else SWITCH_INT_OFF,
        tagIds = this.tagIdList.joinToString(","),
    )
}
