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
import cn.wj.android.cashbook.core.database.table.TABLE_BOOKS
import cn.wj.android.cashbook.core.database.table.TABLE_BOOKS_BG_URI
import org.intellij.lang.annotations.Language

/**
 * 数据库升级 8 -> 9
 * - db_books：新增 bg_uri 字段
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2024/9/9
 */
object Migration8To9 : Migration(8, 9) {

    override fun migrate(db: SupportSQLiteDatabase) {
        logger().i("migrate(db)")
        with(db) {
            migrateBooks()
        }
    }

    /** 增加 bg_uri 字段 */
    @Language("SQL")
    private const val SQL_ALTER_TABLE_BOOKS_ADD_BG_URI = """
        ALTER TABLE `$TABLE_BOOKS` ADD `$TABLE_BOOKS_BG_URI` TEXT DEFAULT `` NOT NULL
    """

    /**
     * 升级账本表
     * - db_books：新增 bg_uri 字段
     */
    private fun SupportSQLiteDatabase.migrateBooks() {
        // 添加 bg_uri 字段
        execSQL(SQL_ALTER_TABLE_BOOKS_ADD_BG_URI)
    }
}
