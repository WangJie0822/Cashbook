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

import android.database.sqlite.SQLiteDatabase
import androidx.annotation.WorkerThread
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.database.util.getContentValues
import cn.wj.android.cashbook.core.database.util.getTableNameList

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
            Migration8To9,
            Migration9To10,
            Migration10To11,
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
        from.getTableNameList()
            .filter { it.startsWith("db_") }
            .forEach { tableName ->
                from.query(SQL_QUERY_ALL_FROM_TABLE_FORMAT.format(tableName)).use { cursor ->
                    while (cursor.moveToNext()) {
                        cursor.getContentValues()?.let { values ->
                            to.insert(
                                table = tableName,
                                conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                                values = values,
                            )
                        }
                    }
                }
            }
    }
}
