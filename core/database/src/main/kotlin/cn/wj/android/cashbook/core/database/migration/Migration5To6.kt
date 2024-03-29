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
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD
import org.intellij.lang.annotations.Language

/**
 * 数据库升级 5 -> 6
 * - db_asset、db_record 表金额类型由 float 修改为 double
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/1
 */
object Migration5To6 : Migration(5, 6) {

    override fun migrate(db: SupportSQLiteDatabase) {
        logger().i("migrate(db)")
        with(db) {
            migrateAsset()
            migrateRecord()
        }
    }

    /** 查询资产列表中的资产和余额 */
    @Language("SQL")
    private const val SQL_QUERY_ID_TOTAL_AMOUNT_FROM_ASSET = """
        SELECT `id`, `total_amount` 
        FROM `$TABLE_ASSET`
    """

    /** 查询资产列表中的资产和余额 */
    @Language("SQL")
    private const val SQL_UPDATE_TOTAL_AMOUNT_TO_ASSET = """
        UPDATE `$TABLE_ASSET` 
        SET `total_amount` = %1${"$"}s
        WHERE `id` = %2${"$"}s
    """

    private fun SupportSQLiteDatabase.migrateAsset() {
        query(SQL_QUERY_ID_TOTAL_AMOUNT_FROM_ASSET).use {
            while (it.moveToNext()) {
                val assetId = it.getLong(it.getColumnIndexOrThrow("id"))
                val totalAmount =
                    it.getFloat(it.getColumnIndexOrThrow("total_amount")).toString()
                        .toDoubleOrZero()
                execSQL(SQL_UPDATE_TOTAL_AMOUNT_TO_ASSET.format(totalAmount, assetId))
            }
        }
    }

    /** 查询资产列表中的资产和余额 */
    @Language("SQL")
    private const val SQL_QUERY_ID_AMOUNT_CHART_FROM_RECORD = """
        SELECT `id`, `amount`, `charge`
        FROM `$TABLE_RECORD`
    """

    /** 查询资产列表中的资产和余额 */
    @Language("SQL")
    private const val SQL_UPDATE_AMOUNT_CHARGE_TO_RECORD = """
        UPDATE `$TABLE_RECORD` 
        SET `amount` = %1${"$"}s, `charge` = %2${"$"}s
        WHERE `id` = %3${"$"}s
    """

    private fun SupportSQLiteDatabase.migrateRecord() {
        query(SQL_QUERY_ID_AMOUNT_CHART_FROM_RECORD).use {
            while (it.moveToNext()) {
                val recordId = it.getLong(it.getColumnIndexOrThrow("id"))
                val amount =
                    it.getFloat(it.getColumnIndexOrThrow("amount")).toString().toDoubleOrZero()
                val charge =
                    it.getFloat(it.getColumnIndexOrThrow("charge")).toString().toDoubleOrZero()
                execSQL(SQL_UPDATE_AMOUNT_CHARGE_TO_RECORD.format(amount, charge, recordId))
            }
        }
    }
}
