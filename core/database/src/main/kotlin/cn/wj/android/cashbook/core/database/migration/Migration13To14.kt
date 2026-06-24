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
import org.intellij.lang.annotations.Language

/**
 * 数据库升级 13 -> 14
 * - 新增 db_record.reimbursed 列（手动「已报销」标记，INTEGER NOT NULL DEFAULT 0）
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/24
 */
object Migration13To14 : Migration(13, 14) {

    @Language("SQL")
    private const val SQL_ADD_REIMBURSED =
        "ALTER TABLE db_record ADD COLUMN reimbursed INTEGER NOT NULL DEFAULT 0"

    override fun migrate(db: SupportSQLiteDatabase) {
        logger().i("migrate(db)")
        db.execSQL(SQL_ADD_REIMBURSED)
    }
}
