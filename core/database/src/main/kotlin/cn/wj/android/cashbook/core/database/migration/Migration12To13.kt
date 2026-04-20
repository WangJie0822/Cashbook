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
import org.intellij.lang.annotations.Language

/**
 * 数据库升级 12 -> 13
 * - 新增 db_schedule 周期记账规则表
 *
 * > 创建于 2026/4/20
 */
object Migration12To13 : Migration(12, 13) {

    override fun migrate(db: SupportSQLiteDatabase) {
        logger().i("migrate(db)")
        with(db) {
            createScheduleTable()
            createScheduleIndices()
        }
    }

    // region Schedule 表创建

    @Language("SQL")
    private const val SQL_CREATE_TABLE_SCHEDULE = """
        CREATE TABLE IF NOT EXISTS `db_schedule` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT,
            `books_id` INTEGER NOT NULL,
            `type_id` INTEGER NOT NULL,
            `asset_id` INTEGER NOT NULL,
            `amount` INTEGER NOT NULL,
            `charge` INTEGER NOT NULL,
            `concessions` INTEGER NOT NULL,
            `remark` TEXT NOT NULL,
            `type_category` INTEGER NOT NULL,
            `frequency` INTEGER NOT NULL,
            `start_date` INTEGER NOT NULL,
            `end_date` INTEGER,
            `record_time` INTEGER NOT NULL,
            `last_executed_date` INTEGER,
            `enabled` INTEGER NOT NULL
        )
    """

    private fun SupportSQLiteDatabase.createScheduleTable() {
        execSQL(SQL_CREATE_TABLE_SCHEDULE)
    }

    // endregion

    // region 索引创建

    @Language("SQL")
    private const val SQL_INDEX_SCHEDULE_BOOKS_ID =
        "CREATE INDEX IF NOT EXISTS `index_db_schedule_books_id` ON `db_schedule`(`books_id`)"

    @Language("SQL")
    private const val SQL_INDEX_SCHEDULE_ENABLED =
        "CREATE INDEX IF NOT EXISTS `index_db_schedule_enabled` ON `db_schedule`(`enabled`)"

    private fun SupportSQLiteDatabase.createScheduleIndices() {
        execSQL(SQL_INDEX_SCHEDULE_BOOKS_ID)
        execSQL(SQL_INDEX_SCHEDULE_ENABLED)
    }

    // endregion
}
