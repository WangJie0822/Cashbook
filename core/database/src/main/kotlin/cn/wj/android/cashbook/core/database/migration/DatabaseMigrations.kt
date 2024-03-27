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

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.WorkerThread
import androidx.core.database.getLongOrNull
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET_BALANCE
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET_BILLING_DATE
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET_BOOKS_ID
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET_CARD_NO
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET_CLASSIFICATION
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET_ID
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET_INVISIBLE
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET_MODIFY_TIME
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET_NAME
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET_OPEN_BANK
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET_REMARK
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET_REPAYMENT_DATE
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET_SORT
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET_TOTAL_AMOUNT
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET_TYPE
import cn.wj.android.cashbook.core.database.table.TABLE_BOOKS
import cn.wj.android.cashbook.core.database.table.TABLE_BOOKS_DESCRIPTION
import cn.wj.android.cashbook.core.database.table.TABLE_BOOKS_ID
import cn.wj.android.cashbook.core.database.table.TABLE_BOOKS_MODIFY_TIME
import cn.wj.android.cashbook.core.database.table.TABLE_BOOKS_NAME
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_AMOUNT
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_ASSET_ID
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_BOOKS_ID
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_CHARGE
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_CONCESSIONS
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_ID
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_INTO_ASSET_ID
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_RECORD_TIME
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_REIMBURSABLE
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_RELATED
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_RELATED_ID
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_RELATED_RECORD_ID
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_RELATED_RELATED_RECORD_ID
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_REMARK
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_TYPE_ID
import cn.wj.android.cashbook.core.database.table.TABLE_TAG
import cn.wj.android.cashbook.core.database.table.TABLE_TAG_BOOKS_ID
import cn.wj.android.cashbook.core.database.table.TABLE_TAG_ID
import cn.wj.android.cashbook.core.database.table.TABLE_TAG_NAME
import cn.wj.android.cashbook.core.database.table.TABLE_TAG_RELATED
import cn.wj.android.cashbook.core.database.table.TABLE_TAG_RELATED_ID
import cn.wj.android.cashbook.core.database.table.TABLE_TAG_RELATED_RECORD_ID
import cn.wj.android.cashbook.core.database.table.TABLE_TAG_RELATED_TAG_ID
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE_ICON_NAME
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE_ID
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE_NAME
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE_PARENT_ID
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE_PROTECTED
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE_SORT
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE_TYPE_CATEGORY
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE_TYPE_LEVEL

/**
 * 数据库迁移工具类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/9
 */
object DatabaseMigrations {

    /** 数据库升级列表 */
    val MIGRATION_LIST =
        arrayOf(
            Migration1To2,
            Migration2To3,
            Migration3To4,
            Migration4To5,
            Migration5To6,
            Migration6To7,
            Migration7To8,
        )

    /** 在数据库升级列表中找到开始版本为 [db] 版本号的迁移类，对数据库进行升级后返回升级后的版本 */
    private fun migrate(db: SupportSQLiteDatabase): Int {
        val current = db.version
        val migration = MIGRATION_LIST.firstOrNull { it.startVersion == current } ?: return -1
        logger().i("migrate(db = <$db>, current = <$current>), migrate to ${migration.endVersion}")
        migration.migrate(db)
        db.version = migration.endVersion
        return migration.endVersion
    }

    /** 从 [from] 数据库备份数据到 [to] 数据库，要求数据库版本一致 */
    @Suppress("unused")
    @WorkerThread
    fun backupFromDb(from: SupportSQLiteDatabase, to: SupportSQLiteDatabase): Boolean {
        if (from.version != to.version) {
            return false
        }
        copyData(from, to)
        return true
    }

    /** 从 [from] 数据库恢复数据到 [to] 数据库，恢复前会将数据库版本升级到一致 */
    @WorkerThread
    fun recoveryFromDb(from: SupportSQLiteDatabase, to: SupportSQLiteDatabase): Boolean {
        val targetVersion = ApplicationInfo.DB_VERSION
        val toVersion = to.version
        var currentVersion = from.version
        logger().i("recoveryFromDb(from = <$from>), fromVersion = <$currentVersion>, targetVersion = <$targetVersion>, toVersion = <$toVersion>")
        if (toVersion != targetVersion) {
            logger().e("recoveryFromDb(), Database version error")
            return false
        }
        while (currentVersion != -1 && targetVersion != currentVersion) {
            currentVersion = migrate(from)
        }
        if (currentVersion != targetVersion) {
            logger().e("recoveryFromDb(), Database migration not found")
            return false
        }
        // 数据版本一致，复制数据
        copyData(from, to)

        return true
    }

    /** 从 [from] 数据库复制数据到 [to] 数据库，主键重复时覆盖 */
    @WorkerThread
    fun copyData(from: SupportSQLiteDatabase, to: SupportSQLiteDatabase) {
        // 资产数据
        from.query(SQL_QUERY_ALL_FROM_ASSET).use {
            while (it.moveToNext()) {
                to.insert(
                    table = TABLE_ASSET,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                    values = ContentValues().apply {
                        put(
                            TABLE_ASSET_ID,
                            it.getLongOrNull(it.getColumnIndexOrThrow(TABLE_ASSET_ID)),
                        )
                        put(
                            TABLE_ASSET_BOOKS_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_ASSET_BOOKS_ID)),
                        )
                        put(
                            TABLE_ASSET_NAME,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_NAME)),
                        )
                        put(
                            TABLE_ASSET_BALANCE,
                            it.getDouble(it.getColumnIndexOrThrow(TABLE_ASSET_BALANCE)),
                        )
                        put(
                            TABLE_ASSET_TOTAL_AMOUNT,
                            it.getDouble(it.getColumnIndexOrThrow(TABLE_ASSET_TOTAL_AMOUNT)),
                        )
                        put(
                            TABLE_ASSET_BILLING_DATE,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_BILLING_DATE)),
                        )
                        put(
                            TABLE_ASSET_REPAYMENT_DATE,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_REPAYMENT_DATE)),
                        )
                        put(
                            TABLE_ASSET_TYPE,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_TYPE)),
                        )
                        put(
                            TABLE_ASSET_CLASSIFICATION,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_CLASSIFICATION)),
                        )
                        put(
                            TABLE_ASSET_INVISIBLE,
                            it.getInt(it.getColumnIndexOrThrow(TABLE_ASSET_INVISIBLE)),
                        )
                        put(
                            TABLE_ASSET_OPEN_BANK,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_OPEN_BANK)),
                        )
                        put(
                            TABLE_ASSET_CARD_NO,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_CARD_NO)),
                        )
                        put(
                            TABLE_ASSET_REMARK,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_REMARK)),
                        )
                        put(TABLE_ASSET_SORT, it.getInt(it.getColumnIndexOrThrow(TABLE_ASSET_SORT)))
                        put(
                            TABLE_ASSET_MODIFY_TIME,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_ASSET_MODIFY_TIME)),
                        )
                    },
                )
            }
        }
        // 账本数据
        from.query(SQL_QUERY_ALL_FROM_BOOKS).use {
            while (it.moveToNext()) {
                to.insert(
                    table = TABLE_BOOKS,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                    values = ContentValues().apply {
                        put(
                            TABLE_BOOKS_ID,
                            it.getLongOrNull(it.getColumnIndexOrThrow(TABLE_BOOKS_ID)),
                        )
                        put(
                            TABLE_BOOKS_NAME,
                            it.getString(it.getColumnIndexOrThrow(TABLE_BOOKS_NAME)),
                        )
                        put(
                            TABLE_BOOKS_DESCRIPTION,
                            it.getString(it.getColumnIndexOrThrow(TABLE_BOOKS_DESCRIPTION)),
                        )
                        put(
                            TABLE_BOOKS_MODIFY_TIME,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_BOOKS_MODIFY_TIME)),
                        )
                    },
                )
            }
        }
        // 记录数据
        from.query(SQL_QUERY_ALL_FROM_RECORD).use {
            while (it.moveToNext()) {
                to.insert(
                    table = TABLE_RECORD,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                    values = ContentValues().apply {
                        put(
                            TABLE_RECORD_ID,
                            it.getLongOrNull(it.getColumnIndexOrThrow(TABLE_RECORD_ID)),
                        )
                        put(
                            TABLE_RECORD_TYPE_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_RECORD_TYPE_ID)),
                        )
                        put(
                            TABLE_RECORD_ASSET_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_RECORD_ASSET_ID)),
                        )
                        put(
                            TABLE_RECORD_INTO_ASSET_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_RECORD_INTO_ASSET_ID)),
                        )
                        put(
                            TABLE_RECORD_BOOKS_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_RECORD_BOOKS_ID)),
                        )
                        put(
                            TABLE_RECORD_AMOUNT,
                            it.getDouble(it.getColumnIndexOrThrow(TABLE_RECORD_AMOUNT)),
                        )
                        put(
                            TABLE_RECORD_CONCESSIONS,
                            it.getDouble(it.getColumnIndexOrThrow(TABLE_RECORD_CONCESSIONS)),
                        )
                        put(
                            TABLE_RECORD_CHARGE,
                            it.getDouble(it.getColumnIndexOrThrow(TABLE_RECORD_CHARGE)),
                        )
                        put(
                            TABLE_RECORD_REMARK,
                            it.getString(it.getColumnIndexOrThrow(TABLE_RECORD_REMARK)),
                        )
                        put(
                            TABLE_RECORD_REIMBURSABLE,
                            it.getInt(it.getColumnIndexOrThrow(TABLE_RECORD_REIMBURSABLE)),
                        )
                        put(
                            TABLE_RECORD_RECORD_TIME,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_RECORD_RECORD_TIME)),
                        )
                    },
                )
            }
        }
        // 记录关联关系
        from.query(SQL_QUERY_ALL_FROM_RECORD_RELATED).use {
            while (it.moveToNext()) {
                to.insert(
                    table = TABLE_RECORD_RELATED,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                    values = ContentValues().apply {
                        put(
                            TABLE_RECORD_RELATED_ID,
                            it.getLongOrNull(it.getColumnIndexOrThrow(TABLE_RECORD_RELATED_ID)),
                        )
                        put(
                            TABLE_RECORD_RELATED_RECORD_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_RECORD_RELATED_RECORD_ID)),
                        )
                        put(
                            TABLE_RECORD_RELATED_RELATED_RECORD_ID,
                            it.getLong(
                                it.getColumnIndexOrThrow(TABLE_RECORD_RELATED_RELATED_RECORD_ID),
                            ),
                        )
                    },
                )
            }
        }
        // 标签数据
        from.query(SQL_QUERY_ALL_FROM_TAG).use {
            while (it.moveToNext()) {
                to.insert(
                    table = TABLE_TAG,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                    values = ContentValues().apply {
                        put(
                            TABLE_TAG_ID,
                            it.getLongOrNull(it.getColumnIndexOrThrow(TABLE_TAG_ID)),
                        )
                        put(
                            TABLE_TAG_NAME,
                            it.getString(it.getColumnIndexOrThrow(TABLE_TAG_NAME)),
                        )
                        put(
                            TABLE_TAG_BOOKS_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_TAG_BOOKS_ID)),
                        )
                    },
                )
            }
        }
        // 标签关联数据
        from.query(SQL_QUERY_ALL_FROM_TAG_RELATED).use {
            while (it.moveToNext()) {
                to.insert(
                    table = TABLE_TAG_RELATED,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                    values = ContentValues().apply {
                        put(
                            TABLE_TAG_RELATED_ID,
                            it.getLongOrNull(it.getColumnIndexOrThrow(TABLE_TAG_RELATED_ID)),
                        )
                        put(
                            TABLE_TAG_RELATED_RECORD_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_TAG_RELATED_RECORD_ID)),
                        )
                        put(
                            TABLE_TAG_RELATED_TAG_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_TAG_RELATED_TAG_ID)),
                        )
                    },
                )
            }
        }
        // 类型数据
        from.query(SQL_QUERY_ALL_FROM_TYPE).use {
            while (it.moveToNext()) {
                to.insert(
                    table = TABLE_TYPE,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                    values = ContentValues().apply {
                        put(
                            TABLE_TYPE_ID,
                            it.getLongOrNull(it.getColumnIndexOrThrow(TABLE_TYPE_ID)),
                        )
                        put(
                            TABLE_TYPE_PARENT_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_TYPE_PARENT_ID)),
                        )
                        put(
                            TABLE_TYPE_NAME,
                            it.getString(it.getColumnIndexOrThrow(TABLE_TYPE_NAME)),
                        )
                        put(
                            TABLE_TYPE_ICON_NAME,
                            it.getString(it.getColumnIndexOrThrow(TABLE_TYPE_ICON_NAME)),
                        )
                        put(
                            TABLE_TYPE_TYPE_LEVEL,
                            it.getString(it.getColumnIndexOrThrow(TABLE_TYPE_TYPE_LEVEL)),
                        )
                        put(
                            TABLE_TYPE_TYPE_CATEGORY,
                            it.getString(it.getColumnIndexOrThrow(TABLE_TYPE_TYPE_CATEGORY)),
                        )
                        put(
                            TABLE_TYPE_PROTECTED,
                            it.getInt(it.getColumnIndexOrThrow(TABLE_TYPE_PROTECTED)),
                        )
                        put(TABLE_TYPE_SORT, it.getInt(it.getColumnIndexOrThrow(TABLE_TYPE_SORT)))
                    },
                )
            }
        }
    }
}
