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
import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.database.table.TABLE_TAG
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE
import org.intellij.lang.annotations.Language

/**
 * 数据库升级 7 -> 8
 * - db_tag：新增 invisible 字段
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2024/3/27
 */
object Migration7To8 : Migration(7, 8) {

    override fun migrate(db: SupportSQLiteDatabase) {
        logger().i("migrate(db)")
        with(db) {
            migrateTag()
            migrateType()
        }
    }

    /** 增加 invisible 字段 */
    @Language("SQL")
    private const val SQL_ALTER_TABLE_TAG_ADD_INVISIBLE = """
        ALTER TABLE `$TABLE_TAG` ADD `invisible` INTEGER DEFAULT $SWITCH_INT_OFF NOT NULL
    """

    /**
     * 升级标签表
     * - db_tag：新增 invisible 字段
     */
    private fun SupportSQLiteDatabase.migrateTag() {
        // 添加 invisible 字段
        execSQL(SQL_ALTER_TABLE_TAG_ADD_INVISIBLE)
    }

    /** 创建类型表，版本 8 */
    @Language("SQL")
    private const val SQL_CREATE_TABLE_TYPE_8 = """
        CREATE TABLE IF NOT EXISTS `$TABLE_TYPE` 
        (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT,
            `parent_id` INTEGER NOT NULL, 
            `name` TEXT NOT NULL, 
            `icon_name` TEXT NOT NULL, 
            `type_level` INTEGER NOT NULL, 
            `type_category` INTEGER NOT NULL, 
            `protected` INTEGER NOT NULL, 
            `sort` INTEGER NOT NULL
        )
    """

    /** 从类型临时表复制数据到新表 */
    @Language("SQL")
    private const val SQL_COPY_TABLE_TYPE_FROM_7_8 = """
        INSERT INTO `$TABLE_TYPE` 
        SELECT *
        FROM `${TABLE_TYPE}_temp`
    """

    /**
     * 升级类型表
     * - 处理非规范流程导致的字段类型错误问题
     */
    private fun SupportSQLiteDatabase.migrateType() {
        // 重命名旧表
        execSQL(SQL_RENAME_TABLE_TYPE_TO_TEMP)
        // 创建新表
        execSQL(SQL_CREATE_TABLE_TYPE_8)
        // 复制数据到新表
        execSQL(SQL_COPY_TABLE_TYPE_FROM_7_8)
        // 删除旧表
        execSQL(SQL_DROP_TABLE_TYPE_TEMP)
    }
}
