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
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET
import org.intellij.lang.annotations.Language

/**
 * 数据库升级 4 -> 5
 * - db_asset 表新增 open_bank、card_no、remark 字段
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/1
 */
object Migration4To5 : Migration(4, 5) {

    /** 增加 open_bank 字段 */
    @Language("SQL")
    private const val SQL_ALTER_TABLE_ASSET_ADD_OPEN_BACK = """
        ALTER TABLE `$TABLE_ASSET` ADD `open_bank` TEXT DEFAULT '' NOT NULL
    """

    /** 增加 card_no 字段 */
    @Language("SQL")
    private const val SQL_ALTER_TABLE_ASSET_ADD_CARD_NO = """
        ALTER TABLE `$TABLE_ASSET` ADD `card_no` TEXT DEFAULT '' NOT NULL
    """

    /** 增加 remark 字段 */
    @Language("SQL")
    private const val SQL_ALTER_TABLE_ASSET_ADD_REMARK = """
        ALTER TABLE `$TABLE_ASSET` ADD `remark` TEXT DEFAULT '' NOT NULL
    """

    override fun migrate(db: SupportSQLiteDatabase) {
        logger().i("migrate(db)")
        with(db) {
            execSQL(SQL_ALTER_TABLE_ASSET_ADD_OPEN_BACK)
            execSQL(SQL_ALTER_TABLE_ASSET_ADD_CARD_NO)
            execSQL(SQL_ALTER_TABLE_ASSET_ADD_REMARK)
        }
    }
}
