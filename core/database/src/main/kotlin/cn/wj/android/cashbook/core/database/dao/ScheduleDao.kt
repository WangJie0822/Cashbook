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

package cn.wj.android.cashbook.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cn.wj.android.cashbook.core.database.table.ScheduleTable
import kotlinx.coroutines.flow.Flow

/**
 * 周期记账规则数据库操作类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/4/20
 */
@Dao
interface ScheduleDao {

    @Query("SELECT * FROM db_schedule")
    fun queryAllSchedules(): Flow<List<ScheduleTable>>

    @Query("SELECT * FROM db_schedule WHERE id = :scheduleId")
    suspend fun queryById(scheduleId: Long): ScheduleTable?

    @Insert
    suspend fun insert(schedule: ScheduleTable): Long

    @Update
    suspend fun update(schedule: ScheduleTable)

    @Query("UPDATE db_schedule SET enabled = :enabled WHERE id = :scheduleId")
    suspend fun updateEnabled(scheduleId: Long, enabled: Int)

    @Query("UPDATE db_schedule SET last_executed_date = :lastExecutedDate WHERE id = :scheduleId")
    suspend fun updateLastExecutedDate(scheduleId: Long, lastExecutedDate: Long)

    @Query("DELETE FROM db_schedule WHERE id = :scheduleId")
    suspend fun deleteById(scheduleId: Long)
}
