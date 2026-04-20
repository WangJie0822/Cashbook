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
 * 数据库升级 13 -> 14
 * - db_schedule 表新增 reimbursable、tag_ids 字段
 *
 * > 创建于 2026/4/21
 */
object Migration13To14 : Migration(13, 14) {

    override fun migrate(db: SupportSQLiteDatabase) {
        logger().i("migrate(db)")
        with(db) {
            addScheduleReimbursableColumn()
            addScheduleTagIdsColumn()
        }
    }

    private fun SupportSQLiteDatabase.addScheduleReimbursableColumn() {
        execSQL("ALTER TABLE `db_schedule` ADD COLUMN `reimbursable` INTEGER NOT NULL DEFAULT 0")
    }

    private fun SupportSQLiteDatabase.addScheduleTagIdsColumn() {
        execSQL("ALTER TABLE `db_schedule` ADD COLUMN `tag_ids` TEXT NOT NULL DEFAULT ''")
    }
}
