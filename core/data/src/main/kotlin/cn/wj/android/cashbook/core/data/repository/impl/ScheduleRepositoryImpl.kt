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

package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.model.scheduleDataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.data.repository.ScheduleRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.database.dao.ScheduleDao
import cn.wj.android.cashbook.core.model.model.ScheduleModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleDao: ScheduleDao,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : ScheduleRepository {

    override val scheduleListData: Flow<List<ScheduleModel>> =
        scheduleDao.queryAllSchedules()
            .mapLatest { list ->
                list.map { it.asModel() }
            }

    override suspend fun queryById(scheduleId: Long): ScheduleModel? =
        withContext(coroutineContext) {
            scheduleDao.queryById(scheduleId)?.asModel()
        }

    override suspend fun saveSchedule(schedule: ScheduleModel): Long =
        withContext(coroutineContext) {
            val id = if (schedule.id == -1L) {
                scheduleDao.insert(schedule.asTable())
            } else {
                scheduleDao.update(schedule.asTable())
                schedule.id
            }
            scheduleDataVersion.updateVersion()
            id
        }

    override suspend fun updateScheduleEnabled(scheduleId: Long, enabled: Boolean) =
        withContext(coroutineContext) {
            scheduleDao.updateEnabled(
                scheduleId = scheduleId,
                enabled = if (enabled) cn.wj.android.cashbook.core.common.SWITCH_INT_ON else cn.wj.android.cashbook.core.common.SWITCH_INT_OFF,
            )
            scheduleDataVersion.updateVersion()
        }

    override suspend fun updateLastExecutedDate(scheduleId: Long, lastExecutedDate: Long) =
        withContext(coroutineContext) {
            scheduleDao.updateLastExecutedDate(scheduleId, lastExecutedDate)
        }

    override suspend fun deleteSchedule(scheduleId: Long) =
        withContext(coroutineContext) {
            scheduleDao.deleteById(scheduleId)
            scheduleDataVersion.updateVersion()
        }
}
