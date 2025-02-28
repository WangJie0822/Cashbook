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
 * 数据库升级 10 -> 11
 * - db_image_with_related：新增表格
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2025/2/24
 */
object Migration10To11 : Migration(10, 11) {

    override fun migrate(db: SupportSQLiteDatabase) {
        logger().i("migrate(db)")
        with(db) {
            migrateImage()
        }
    }

    /** 创建 image 表 */
    @Language("SQL")
    private const val SQL_CREATE_TABLE_IMAGE_WITH_RELATED_11 = """
        CREATE TABLE IF NOT EXISTS `db_image_with_related` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `record_id` INTEGER NOT NULL, `image_path` TEXT NOT NULL, `image_bytes` BLOB NOT NULL)
    """

    /**
     * 创建 image 表
     */
    private fun SupportSQLiteDatabase.migrateImage() {
        // 创建 image 表
        execSQL(SQL_CREATE_TABLE_IMAGE_WITH_RELATED_11)
    }
}
