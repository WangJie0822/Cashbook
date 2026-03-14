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
 * 数据库升级 11 -> 12
 * - db_record: 金额字段从 REAL 改为 INTEGER（单位：分）
 * - db_asset: 金额字段从 REAL 改为 INTEGER（单位：分）
 * - 添加核心表索引
 *
 * > 创建于 2026/3/14
 */
object Migration11To12 : Migration(11, 12) {

    override fun migrate(db: SupportSQLiteDatabase) {
        logger().i("migrate(db)")
        with(db) {
            migrateRecord()
            migrateAsset()
            createIndices()
        }
    }

    // region Record 表重建

    @Language("SQL")
    private const val SQL_CREATE_RECORD_NEW = """
        CREATE TABLE IF NOT EXISTS `db_record_new` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT,
            `type_id` INTEGER NOT NULL,
            `asset_id` INTEGER NOT NULL,
            `into_asset_id` INTEGER NOT NULL,
            `books_id` INTEGER NOT NULL,
            `amount` INTEGER NOT NULL,
            `final_amount` INTEGER NOT NULL,
            `concessions` INTEGER NOT NULL,
            `charge` INTEGER NOT NULL,
            `remark` TEXT NOT NULL,
            `reimbursable` INTEGER NOT NULL,
            `record_time` INTEGER NOT NULL
        )
    """

    @Language("SQL")
    private const val SQL_COPY_RECORD = """
        INSERT INTO `db_record_new`
        SELECT `id`, `type_id`, `asset_id`, `into_asset_id`, `books_id`,
            CAST(ROUND(`amount` * 100) AS INTEGER),
            CAST(ROUND(`final_amount` * 100) AS INTEGER),
            CAST(ROUND(`concessions` * 100) AS INTEGER),
            CAST(ROUND(`charge` * 100) AS INTEGER),
            `remark`, `reimbursable`, `record_time`
        FROM `db_record`
    """

    @Language("SQL")
    private const val SQL_DROP_RECORD = "DROP TABLE IF EXISTS `db_record`"

    @Language("SQL")
    private const val SQL_RENAME_RECORD = "ALTER TABLE `db_record_new` RENAME TO `db_record`"

    private fun SupportSQLiteDatabase.migrateRecord() {
        execSQL(SQL_CREATE_RECORD_NEW)
        execSQL(SQL_COPY_RECORD)
        execSQL(SQL_DROP_RECORD)
        execSQL(SQL_RENAME_RECORD)
    }

    // endregion

    // region Asset 表重建

    @Language("SQL")
    private const val SQL_CREATE_ASSET_NEW = """
        CREATE TABLE IF NOT EXISTS `db_asset_new` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT,
            `books_id` INTEGER NOT NULL,
            `name` TEXT NOT NULL,
            `balance` INTEGER NOT NULL,
            `total_amount` INTEGER NOT NULL,
            `billing_date` TEXT NOT NULL,
            `repayment_date` TEXT NOT NULL,
            `type` INTEGER NOT NULL,
            `classification` INTEGER NOT NULL,
            `invisible` INTEGER NOT NULL,
            `open_bank` TEXT NOT NULL,
            `card_no` TEXT NOT NULL,
            `remark` TEXT NOT NULL,
            `sort` INTEGER NOT NULL,
            `modify_time` INTEGER NOT NULL
        )
    """

    @Language("SQL")
    private const val SQL_COPY_ASSET = """
        INSERT INTO `db_asset_new`
        SELECT `id`, `books_id`, `name`,
            CAST(ROUND(`balance` * 100) AS INTEGER),
            CAST(ROUND(`total_amount` * 100) AS INTEGER),
            `billing_date`, `repayment_date`, `type`, `classification`,
            `invisible`, `open_bank`, `card_no`, `remark`, `sort`, `modify_time`
        FROM `db_asset`
    """

    @Language("SQL")
    private const val SQL_DROP_ASSET = "DROP TABLE IF EXISTS `db_asset`"

    @Language("SQL")
    private const val SQL_RENAME_ASSET = "ALTER TABLE `db_asset_new` RENAME TO `db_asset`"

    private fun SupportSQLiteDatabase.migrateAsset() {
        execSQL(SQL_CREATE_ASSET_NEW)
        execSQL(SQL_COPY_ASSET)
        execSQL(SQL_DROP_ASSET)
        execSQL(SQL_RENAME_ASSET)
    }

    // endregion

    // region 索引创建

    @Language("SQL")
    private const val SQL_INDEX_RECORD_BOOKS_ID =
        "CREATE INDEX IF NOT EXISTS `index_db_record_books_id` ON `db_record`(`books_id`)"

    @Language("SQL")
    private const val SQL_INDEX_RECORD_TYPE_ID =
        "CREATE INDEX IF NOT EXISTS `index_db_record_type_id` ON `db_record`(`type_id`)"

    @Language("SQL")
    private const val SQL_INDEX_RECORD_ASSET_ID =
        "CREATE INDEX IF NOT EXISTS `index_db_record_asset_id` ON `db_record`(`asset_id`)"

    @Language("SQL")
    private const val SQL_INDEX_RECORD_INTO_ASSET_ID =
        "CREATE INDEX IF NOT EXISTS `index_db_record_into_asset_id` ON `db_record`(`into_asset_id`)"

    @Language("SQL")
    private const val SQL_INDEX_RECORD_TIME =
        "CREATE INDEX IF NOT EXISTS `index_db_record_record_time` ON `db_record`(`record_time`)"

    @Language("SQL")
    private const val SQL_INDEX_RELATED_RECORD_ID =
        "CREATE INDEX IF NOT EXISTS `index_db_record_with_related_record_id` ON `db_record_with_related`(`record_id`)"

    @Language("SQL")
    private const val SQL_INDEX_RELATED_RELATED_ID =
        "CREATE INDEX IF NOT EXISTS `index_db_record_with_related_related_record_id` ON `db_record_with_related`(`related_record_id`)"

    @Language("SQL")
    private const val SQL_INDEX_TAG_RECORD_RECORD_ID =
        "CREATE INDEX IF NOT EXISTS `index_db_tag_with_record_record_id` ON `db_tag_with_record`(`record_id`)"

    @Language("SQL")
    private const val SQL_INDEX_TAG_RECORD_TAG_ID =
        "CREATE INDEX IF NOT EXISTS `index_db_tag_with_record_tag_id` ON `db_tag_with_record`(`tag_id`)"

    @Language("SQL")
    private const val SQL_INDEX_ASSET_BOOKS_ID =
        "CREATE INDEX IF NOT EXISTS `index_db_asset_books_id` ON `db_asset`(`books_id`)"

    @Language("SQL")
    private const val SQL_INDEX_IMAGE_RECORD_ID =
        "CREATE INDEX IF NOT EXISTS `index_db_image_with_related_record_id` ON `db_image_with_related`(`record_id`)"

    @Language("SQL")
    private const val SQL_INDEX_RECORD_BOOKS_ID_RECORD_TIME =
        "CREATE INDEX IF NOT EXISTS `index_db_record_books_id_record_time` ON `db_record`(`books_id`, `record_time`)"

    @Language("SQL")
    private const val SQL_INDEX_TYPE_PARENT_ID =
        "CREATE INDEX IF NOT EXISTS `index_db_type_parent_id` ON `db_type`(`parent_id`)"

    @Language("SQL")
    private const val SQL_INDEX_TYPE_CATEGORY =
        "CREATE INDEX IF NOT EXISTS `index_db_type_type_category` ON `db_type`(`type_category`)"

    private fun SupportSQLiteDatabase.createIndices() {
        execSQL(SQL_INDEX_RECORD_BOOKS_ID)
        execSQL(SQL_INDEX_RECORD_TYPE_ID)
        execSQL(SQL_INDEX_RECORD_ASSET_ID)
        execSQL(SQL_INDEX_RECORD_INTO_ASSET_ID)
        execSQL(SQL_INDEX_RECORD_TIME)
        execSQL(SQL_INDEX_RECORD_BOOKS_ID_RECORD_TIME)
        execSQL(SQL_INDEX_RELATED_RECORD_ID)
        execSQL(SQL_INDEX_RELATED_RELATED_ID)
        execSQL(SQL_INDEX_TAG_RECORD_RECORD_ID)
        execSQL(SQL_INDEX_TAG_RECORD_TAG_ID)
        execSQL(SQL_INDEX_ASSET_BOOKS_ID)
        execSQL(SQL_INDEX_IMAGE_RECORD_ID)
        execSQL(SQL_INDEX_TYPE_PARENT_ID)
        execSQL(SQL_INDEX_TYPE_CATEGORY)
    }

    // endregion
}
