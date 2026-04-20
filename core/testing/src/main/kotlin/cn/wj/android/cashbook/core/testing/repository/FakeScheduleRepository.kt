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

package cn.wj.android.cashbook.core.testing.repository

import cn.wj.android.cashbook.core.data.repository.ScheduleRepository
import cn.wj.android.cashbook.core.model.model.ScheduleModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeScheduleRepository : ScheduleRepository {

    private val schedules = mutableListOf<ScheduleModel>()
    private val _scheduleListData = MutableStateFlow<List<ScheduleModel>>(emptyList())

    override val scheduleListData: Flow<List<ScheduleModel>> = _scheduleListData

    var lastSavedSchedule: ScheduleModel? = null
        private set
    var lastUpdatedEnabledId: Long = -1L
        private set
    var lastUpdatedEnabledValue: Boolean = false
        private set
    var lastUpdatedLastExecutedId: Long = -1L
        private set
    var lastDeletedScheduleId: Long = -1L
        private set

    fun addSchedule(schedule: ScheduleModel) {
        schedules.add(schedule)
        _scheduleListData.value = schedules.toList()
    }

    override suspend fun queryById(scheduleId: Long): ScheduleModel? {
        return schedules.find { it.id == scheduleId }
    }

    override suspend fun saveSchedule(schedule: ScheduleModel): Long {
        lastSavedSchedule = schedule
        val index = schedules.indexOfFirst { it.id == schedule.id }
        return if (index >= 0) {
            schedules[index] = schedule
            schedule.id
        } else {
            val newId = if (schedule.id == -1L) {
                (schedules.maxOfOrNull { it.id } ?: 0L) + 1L
            } else {
                schedule.id
            }
            val newSchedule = schedule.copy(id = newId)
            schedules.add(newSchedule)
            _scheduleListData.value = schedules.toList()
            newId
        }
    }

    override suspend fun updateScheduleEnabled(scheduleId: Long, enabled: Boolean) {
        lastUpdatedEnabledId = scheduleId
        lastUpdatedEnabledValue = enabled
        val index = schedules.indexOfFirst { it.id == scheduleId }
        if (index >= 0) {
            schedules[index] = schedules[index].copy(enabled = enabled)
            _scheduleListData.value = schedules.toList()
        }
    }

    override suspend fun updateLastExecutedDate(scheduleId: Long, lastExecutedDate: Long) {
        lastUpdatedLastExecutedId = scheduleId
        val index = schedules.indexOfFirst { it.id == scheduleId }
        if (index >= 0) {
            schedules[index] = schedules[index].copy(lastExecutedDate = lastExecutedDate)
            _scheduleListData.value = schedules.toList()
        }
    }

    override suspend fun deleteSchedule(scheduleId: Long) {
        lastDeletedScheduleId = scheduleId
        schedules.removeAll { it.id == scheduleId }
        _scheduleListData.value = schedules.toList()
    }
}
