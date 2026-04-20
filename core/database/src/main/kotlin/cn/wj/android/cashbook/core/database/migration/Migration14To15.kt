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

package cn.wj.android.cashbook.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.wj.android.cashbook.core.common.ext.logger

/**
 * 数据库升级 14 -> 15
 * - db_record 表新增 schedule_id 字段
 *
 * > 创建于 2026/4/21
 */
object Migration14To15 : Migration(14, 15) {

    override fun migrate(db: SupportSQLiteDatabase) {
        logger().i("migrate(db)")
        db.addScheduleIdColumn()
        db.createScheduleIdIndex()
    }

    private fun SupportSQLiteDatabase.addScheduleIdColumn() {
        execSQL("ALTER TABLE `db_record` ADD COLUMN `schedule_id` INTEGER NOT NULL DEFAULT -1")
    }

    private fun SupportSQLiteDatabase.createScheduleIdIndex() {
        execSQL("CREATE INDEX IF NOT EXISTS `index_db_record_schedule_id` ON `db_record` (`schedule_id`)")
    }
}
