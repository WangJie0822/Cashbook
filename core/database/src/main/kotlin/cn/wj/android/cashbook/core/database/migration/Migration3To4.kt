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
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.table.TABLE_TAG
import org.intellij.lang.annotations.Language

/**
 * 数据库升级 3 -> 4
 * - db_tag 表新增 books_id、shared
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/1
 */
object Migration3To4 : Migration(3, 4) {

    /** 增加 books_id 字段 */
    @Language("SQL")
    private const val SQL_ALTER_TABLE_TAG_ADD_BOOKS_ID = """
        ALTER TABLE `$TABLE_TAG` ADD `books_id` INTEGER DEFAULT -1 NOT NULL
    """

    /** 增加 books_id 字段 */
    @Language("SQL")
    private const val SQL_ALTER_TABLE_TAG_ADD_SHARED = """
        ALTER TABLE `$TABLE_TAG` ADD `shared` INTEGER DEFAULT $SWITCH_INT_ON NOT NULL
    """

    override fun migrate(db: SupportSQLiteDatabase) = with(db) {
        execSQL(SQL_ALTER_TABLE_TAG_ADD_BOOKS_ID)
        execSQL(SQL_ALTER_TABLE_TAG_ADD_SHARED)
    }
}
