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
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_AMOUNT
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_CHARGE
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_CONCESSIONS
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_FINAL_AMOUNT
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_ID
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_TYPE_ID
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE_TYPE_CATEGORY
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import org.intellij.lang.annotations.Language

/**
 * 数据库升级 9 -> 10
 * - db_books：新增 bg_uri 字段
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2025/1/14
 */
object Migration9To10 : Migration(9, 10) {

    override fun migrate(db: SupportSQLiteDatabase) {
        logger().i("migrate(db)")
        with(db) {
            migrateRecord()
            refreshFinalAmount()
        }
    }

    /** 增加 final_amount 字段 */
    @Language("SQL")
    private const val SQL_ALTER_TABLE_RECORD_ADD_FINAL_AMOUNT = """
        ALTER TABLE `$TABLE_RECORD` ADD `$TABLE_RECORD_FINAL_AMOUNT` REAL DEFAULT 0 NOT NULL
    """

    /**
     * 升级记录表
     * - db_record：新增 final_amount 字段
     */
    private fun SupportSQLiteDatabase.migrateRecord() {
        // 添加 final_amount 字段
        execSQL(SQL_ALTER_TABLE_RECORD_ADD_FINAL_AMOUNT)
    }

    /** 查询转账记录数据 */
    @Language("SQL")
    private val SQL_QUERY_TRANSFER_RECORD_FROM_RECORD_WHERE = """
        SELECT `$TABLE_RECORD_ID`, `$TABLE_RECORD_TYPE_ID`, `$TABLE_RECORD_AMOUNT`, `$TABLE_RECORD_CONCESSIONS`, `$TABLE_RECORD_CHARGE`
        FROM `$TABLE_RECORD`
        WHERE `type_id` IN (
            SELECT `id`
            FROM `$TABLE_TYPE`
            WHERE `$TABLE_TYPE_TYPE_CATEGORY` = ${RecordTypeCategoryEnum.TRANSFER.ordinal}
        )
    """

    /**
     * TODO 刷新已有数据中的 final_amount 字段
     */
    private fun SupportSQLiteDatabase.refreshFinalAmount() {
        query(SQL_QUERY_TRANSFER_RECORD_FROM_RECORD_WHERE).use { cursor ->
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(TABLE_RECORD_ID))
            val amount = cursor.getDouble(cursor.getColumnIndexOrThrow(TABLE_RECORD_AMOUNT))
        }
    }
}
