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
 * - 新增 db_budget 预算表 + (books_id, type_id) 唯一索引
 * - F3 搭车：清理历史 Migration6To7 遗漏的 db_record_temp 临时表（防污染备份恢复）
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/23
 */
object Migration12To13 : Migration(12, 13) {

    @Language("SQL")
    private const val SQL_CREATE_BUDGET = """
        CREATE TABLE IF NOT EXISTS `db_budget` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT,
            `books_id` INTEGER NOT NULL,
            `type_id` INTEGER NOT NULL,
            `amount` INTEGER NOT NULL
        )
    """

    @Language("SQL")
    private const val SQL_INDEX_BUDGET = """
        CREATE UNIQUE INDEX IF NOT EXISTS `index_db_budget_books_id_type_id`
        ON `db_budget`(`books_id`, `type_id`)
    """

    @Language("SQL")
    private const val SQL_DROP_RECORD_TEMP = "DROP TABLE IF EXISTS `db_record_temp`"

    override fun migrate(db: SupportSQLiteDatabase) {
        logger().i("migrate(db)")
        db.execSQL(SQL_CREATE_BUDGET)
        db.execSQL(SQL_INDEX_BUDGET)
        db.execSQL(SQL_DROP_RECORD_TEMP)
    }
}
