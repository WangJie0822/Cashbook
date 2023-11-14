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
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE
import org.intellij.lang.annotations.Language

/**
 * 数据库升级 2 -> 3
 * - db_record 表 type 重命名为 type_enum，first_type_id 重命名为 type_id，删除了 second_type_id
 * - db_type 表删除了 system
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/1
 */
object Migration2To3 : Migration(2, 3) {

    override fun migrate(db: SupportSQLiteDatabase) = with(db) {
        migrateRecord()
        migrateType()
    }

    /** 创建记录表，版本 3 */
    @Language("SQL")
    private const val SQL_CREATE_TABLE_RECORD_3 = """
        CREATE TABLE IF NOT EXISTS `$TABLE_RECORD` 
        (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT, 
            `type_enum` TEXT NOT NULL, 
            `type_id` INTEGER NOT NULL, 
            `asset_id` INTEGER NOT NULL, 
            `into_asset_id` INTEGER NOT NULL, 
            `books_id` INTEGER NOT NULL, 
            `record_id` INTEGER NOT NULL, 
            `amount` REAL NOT NULL, 
            `charge` REAL NOT NULL, 
            `remark` TEXT NOT NULL, 
            `tag_ids` TEXT NOT NULL, 
            `reimbursable` INTEGER NOT NULL, 
            `system` INTEGER NOT NULL, 
            `record_time` INTEGER NOT NULL, 
            `create_time` INTEGER NOT NULL, 
            `modify_time` INTEGER NOT NULL
        )
    """

    /** 从旧表复制数据到新表 */
    @Language("SQL")
    private const val SQL_COPY_RECORD_FROM_TEMP = """
        INSERT INTO `$TABLE_RECORD` 
        SELECT `id`, `type`, `first_type_id`, `asset_id`, `into_asset_id`, `books_id`, `record_id`, `amount`, `charge`, `remark`, `tag_ids`, `reimbursable`, `system`, `record_time`, `create_time`, `modify_time` 
        FROM `${TABLE_RECORD}_temp`
    """

    private fun SupportSQLiteDatabase.migrateRecord() {
        // 重命名旧表
        execSQL(SQL_RENAME_TABLE_RECORD_TO_TEMP)
        // 创建新表
        execSQL(SQL_CREATE_TABLE_RECORD_3)
        // 复制数据
        execSQL(SQL_COPY_RECORD_FROM_TEMP)
        // 删除旧表
        execSQL(SQL_DROP_TABLE_RECORD_TEMP)
    }

    /** 创建类型表，版本 3 */
    @Language("SQL")
    private const val SQL_CREATE_TABLE_TYPE_3 = """
        CREATE TABLE IF NOT EXISTS `$TABLE_TYPE` 
        (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT, 
            `parent_id` INTEGER NOT NULL, 
            `name` TEXT NOT NULL, 
            `icon_res_name` TEXT NOT NULL, 
            `type` TEXT NOT NULL, 
            `record_type` INTEGER NOT NULL, 
            `child_enable` INTEGER NOT NULL, 
            `refund` INTEGER NOT NULL, 
            `reimburse` INTEGER NOT NULL, 
            `sort` INTEGER NOT NULL
        )
    """

    /** 从旧表复制数据到新表 */
    @Language("SQL")
    private const val SQL_COPY_TYPE_FROM_TEMP = """
        INSERT INTO `$TABLE_TYPE` 
        SELECT `id`, `parent_id`, `name`, `icon_res_name`, `type`, `record_type`, `child_enable`, `refund`, `reimburse`, `sort` 
        FROM `${TABLE_TYPE}_temp`
    """

    private fun SupportSQLiteDatabase.migrateType() {
        // 重命名旧表
        execSQL(SQL_RENAME_TABLE_TYPE_TO_TEMP)
        // 创建新表
        execSQL(SQL_CREATE_TABLE_TYPE_3)
        // 复制数据
        execSQL(SQL_COPY_TYPE_FROM_TEMP)
        // 删除旧表
        execSQL(SQL_DROP_TABLE_TYPE_TEMP)
    }
}
