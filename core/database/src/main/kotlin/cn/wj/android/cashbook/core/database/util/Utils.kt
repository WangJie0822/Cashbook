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

package cn.wj.android.cashbook.core.database.util

import android.content.ContentValues
import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.wj.android.cashbook.core.common.ext.logger

/** 从 [SupportSQLiteDatabase] 数据库获取所有数据表名称列表 */
internal fun SupportSQLiteDatabase.getTableNameList(): List<String> {
    val result = mutableListOf<String>()
    query(
        "SELECT `tbl_name` FROM `sqlite_master` WHERE `type` = ?",
        arrayOf("table"),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            val tableName = runCatching {
                cursor.getString(cursor.getColumnIndexOrThrow("tbl_name"))
            }.getOrElse { throwable ->
                logger().e(throwable, "getTableNameList()")
                null
            }
            if (!tableName.isNullOrBlank()) {
                result.add(tableName)
            }
        }
    }
    logger().i("getTableNameList(), result = <$result>")
    return result
}

/** 从 [Cursor] 中获取 [ContentValues] 数据，用于将数据插入到另一张表中 */
internal fun Cursor.getContentValues(): ContentValues? = runCatching {
    val result = ContentValues().apply {
        for (name in columnNames) {
            val index = getColumnIndexOrThrow(name)
            val type = getType(index)
            when (type) {
                Cursor.FIELD_TYPE_INTEGER -> {
                    put(name, getLong(index))
                }

                Cursor.FIELD_TYPE_FLOAT -> {
                    put(name, getDouble(index))
                }

                Cursor.FIELD_TYPE_STRING -> {
                    put(name, getString(index))
                }

                Cursor.FIELD_TYPE_BLOB -> {
                    put(name, getBlob(index))
                }

                else -> {}
            }
        }
    }
    logger().d("getContentValues(), result = <$result>")
    result
}.getOrElse { throwable ->
    logger().e(throwable, "getContentValues()")
    null
}
