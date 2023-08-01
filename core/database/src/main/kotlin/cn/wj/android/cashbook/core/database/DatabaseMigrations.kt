@file:Suppress("unused")

package cn.wj.android.cashbook.core.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.WorkerThread
import androidx.core.database.getLongOrNull
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
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
import org.intellij.lang.annotations.Language

/**
 * 数据库迁移工具类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/9
 */
object DatabaseMigrations {

    /**
     * 数据库升级 1 -> 2
     * - 新增 db_tag 表
     */
    val MIGRATION_1_2 = Migration(1, 2) { database ->
        // 新增 标签 表
        database.execSQL("CREATE TABLE IF NOT EXISTS `db_tag` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `name` TEXT NOT NULL)")
    }

    /**
     * 数据库升级 2 -> 3
     * - db_record 表 type 重命名为 type_enum，first_type_id 重命名为 type_id，删除了 second_type_id
     * - db_type 表删除了 system
     */
    val MIGRATION_2_3 = Migration(2, 3) { database ->
        // 更新记录表数据
        // 将旧表重命名
        database.execSQL("ALTER TABLE `db_record` RENAME TO `db_record_temp`")
        // 新建新表
        database.execSQL("CREATE TABLE IF NOT EXISTS `db_record` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `type_enum` TEXT NOT NULL, `type_id` INTEGER NOT NULL, `asset_id` INTEGER NOT NULL, `into_asset_id` INTEGER NOT NULL, `books_id` INTEGER NOT NULL, `record_id` INTEGER NOT NULL, `amount` REAL NOT NULL, `charge` REAL NOT NULL, `remark` TEXT NOT NULL, `tag_ids` TEXT NOT NULL, `reimbursable` INTEGER NOT NULL, `system` INTEGER NOT NULL, `record_time` INTEGER NOT NULL, `create_time` INTEGER NOT NULL, `modify_time` INTEGER NOT NULL)")
        // 从旧表中查询数据插入新表
        database.execSQL("INSERT INTO `db_record` SELECT `id`, `type`, `first_type_id`, `asset_id`, `into_asset_id`, `books_id`, `record_id`, `amount`, `charge`, `remark`, `tag_ids`, `reimbursable`, `system`, `record_time`, `create_time`, `modify_time` FROM `db_record_temp`")
        // 删除旧表
        database.execSQL("DROP TABLE `db_record_temp`")

        // 更新分类表数据
        // 将旧表重命名
        database.execSQL("ALTER TABLE `db_type` RENAME TO `db_type_temp`")
        // 新建新表
        database.execSQL("CREATE TABLE IF NOT EXISTS `db_type` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `parent_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `icon_res_name` TEXT NOT NULL, `type` TEXT NOT NULL, `record_type` INTEGER NOT NULL, `child_enable` INTEGER NOT NULL, `refund` INTEGER NOT NULL, `reimburse` INTEGER NOT NULL, `sort` INTEGER NOT NULL)")
        // 从旧表中查询数据插入新表
        database.execSQL("INSERT INTO `db_type` SELECT `id`, `parent_id`, `name`, `icon_res_name`, `type`, `record_type`, `child_enable`, `refund`, `reimburse`, `sort` FROM `db_type_temp`")
        // 删除旧表
        database.execSQL("DROP TABLE `db_type_temp`")
    }

    /**
     * 数据库升级 3 -> 4
     * - db_tag 表新增 books_id、shared
     */
    val MIGRATION_3_4 = Migration(3, 4) { database ->
        // 标签表新增所属账本主键
        database.execSQL("ALTER TABLE `db_tag` ADD `books_id` INTEGER DEFAULT -1 NOT NULL")
        database.execSQL("ALTER TABLE `db_tag` ADD `shared` INTEGER DEFAULT $SWITCH_INT_ON NOT NULL")
    }

    /**
     * 数据库升级 4 -> 5
     * - db_asset 表新增 open_bank、card_no、remark
     */
    val MIGRATION_4_5 = Migration(4, 5) { database ->
        // 资产表新增字段
        database.execSQL("ALTER TABLE `db_asset` ADD `open_bank` TEXT DEFAULT '' NOT NULL")
        database.execSQL("ALTER TABLE `db_asset` ADD `card_no` TEXT DEFAULT '' NOT NULL")
        database.execSQL("ALTER TABLE `db_asset` ADD `remark` TEXT DEFAULT '' NOT NULL")
    }

    /**
     * 数据库升级 5 -> 6
     * - db_asset、db_record 表金额类型由 float 修改为 double
     */
    val MIGRATION_5_6 = Migration(5, 6) { database ->
        // 将数据从 Float 更新为 Double
        database.query("SELECT `id`, `total_amount` FROM `db_asset`").use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow("id"))
                val totalAmount =
                    it.getFloat(it.getColumnIndexOrThrow("total_amount"))
                val newAmount = totalAmount.toString().toDoubleOrZero()
                database.execSQL("UPDATE db_asset SET `total_amount`=$newAmount WHERE `id`=$id")
            }
        }
        database.query("SELECT `id`, `amount`, `charge` FROM `db_record`").use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow("id"))
                val amount =
                    it.getFloat(it.getColumnIndexOrThrow("amount"))
                val charge = it.getFloat(it.getColumnIndexOrThrow("charge"))
                val newAmount = amount.toString().toDoubleOrZero()
                val newCharge = charge.toString().toDoubleOrZero()
                database.execSQL("UPDATE db_record SET `amount`=$newAmount, `charge`=$newCharge WHERE `id`=$id")
            }
        }
    }

    /**
     * TODO 数据库升级 6 -> 7
     */
    val MIGRATION_6_7 = Migration(6, 7) { database ->

    }

    /** 数据库升级列表 */
    val MIGRATION_LIST =
        arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
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
        val targetVersion = ApplicationInfo.dbVersion
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

    @Language("SQL")
    private const val SQL_QUERY_ALL_FROM_ASSET = "SELECT * FROM `$TABLE_ASSET`"

    @Language("SQL")
    private const val SQL_QUERY_ALL_FROM_BOOKS = "SELECT * FROM `$TABLE_BOOKS`"

    @Language("SQL")
    private const val SQL_QUERY_ALL_FROM_RECORD = "SELECT * FROM `$TABLE_RECORD`"

    @Language("SQL")
    private const val SQL_QUERY_ALL_FROM_RECORD_RELATED = "SELECT * FROM `$TABLE_RECORD_RELATED`"

    @Language("SQL")
    private const val SQL_QUERY_ALL_FROM_TAG = "SELECT * FROM `$TABLE_TAG`"

    @Language("SQL")
    private const val SQL_QUERY_ALL_FROM_TAG_RELATED = "SELECT * FROM `$TABLE_TAG_RELATED`"

    @Language("SQL")
    private const val SQL_QUERY_ALL_FROM_TYPE = "SELECT * FROM `$TABLE_TYPE`"

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
                            it.getLongOrNull(it.getColumnIndexOrThrow(TABLE_ASSET_ID))
                        )
                        put(
                            TABLE_ASSET_BOOKS_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_ASSET_BOOKS_ID))
                        )
                        put(
                            TABLE_ASSET_NAME,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_NAME))
                        )
                        put(
                            TABLE_ASSET_BALANCE,
                            it.getDouble(it.getColumnIndexOrThrow(TABLE_ASSET_BALANCE))
                        )
                        put(
                            TABLE_ASSET_TOTAL_AMOUNT,
                            it.getDouble(it.getColumnIndexOrThrow(TABLE_ASSET_TOTAL_AMOUNT))
                        )
                        put(
                            TABLE_ASSET_BILLING_DATE,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_BILLING_DATE))
                        )
                        put(
                            TABLE_ASSET_REPAYMENT_DATE,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_REPAYMENT_DATE))
                        )
                        put(
                            TABLE_ASSET_TYPE,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_TYPE))
                        )
                        put(
                            TABLE_ASSET_CLASSIFICATION,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_CLASSIFICATION))
                        )
                        put(
                            TABLE_ASSET_INVISIBLE,
                            it.getInt(it.getColumnIndexOrThrow(TABLE_ASSET_INVISIBLE))
                        )
                        put(
                            TABLE_ASSET_OPEN_BANK,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_OPEN_BANK))
                        )
                        put(
                            TABLE_ASSET_CARD_NO,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_CARD_NO))
                        )
                        put(
                            TABLE_ASSET_REMARK,
                            it.getString(it.getColumnIndexOrThrow(TABLE_ASSET_REMARK))
                        )
                        put(TABLE_ASSET_SORT, it.getInt(it.getColumnIndexOrThrow(TABLE_ASSET_SORT)))
                        put(
                            TABLE_ASSET_MODIFY_TIME,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_ASSET_MODIFY_TIME))
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
                            it.getLongOrNull(it.getColumnIndexOrThrow(TABLE_BOOKS_ID))
                        )
                        put(
                            TABLE_BOOKS_NAME,
                            it.getString(it.getColumnIndexOrThrow(TABLE_BOOKS_NAME))
                        )
                        put(
                            TABLE_BOOKS_DESCRIPTION,
                            it.getString(it.getColumnIndexOrThrow(TABLE_BOOKS_DESCRIPTION))
                        )
                        put(
                            TABLE_BOOKS_MODIFY_TIME,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_BOOKS_MODIFY_TIME))
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
                            it.getLongOrNull(it.getColumnIndexOrThrow(TABLE_RECORD_ID))
                        )
                        put(
                            TABLE_RECORD_TYPE_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_RECORD_TYPE_ID))
                        )
                        put(
                            TABLE_RECORD_ASSET_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_RECORD_ASSET_ID))
                        )
                        put(
                            TABLE_RECORD_INTO_ASSET_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_RECORD_INTO_ASSET_ID))
                        )
                        put(
                            TABLE_RECORD_BOOKS_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_RECORD_BOOKS_ID))
                        )
                        put(
                            TABLE_RECORD_AMOUNT,
                            it.getDouble(it.getColumnIndexOrThrow(TABLE_RECORD_AMOUNT))
                        )
                        put(
                            TABLE_RECORD_CONCESSIONS,
                            it.getDouble(it.getColumnIndexOrThrow(TABLE_RECORD_CONCESSIONS))
                        )
                        put(
                            TABLE_RECORD_CHARGE,
                            it.getDouble(it.getColumnIndexOrThrow(TABLE_RECORD_CHARGE))
                        )
                        put(
                            TABLE_RECORD_REMARK,
                            it.getString(it.getColumnIndexOrThrow(TABLE_RECORD_REMARK))
                        )
                        put(
                            TABLE_RECORD_REIMBURSABLE,
                            it.getInt(it.getColumnIndexOrThrow(TABLE_RECORD_REIMBURSABLE))
                        )
                        put(
                            TABLE_RECORD_RECORD_TIME,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_RECORD_RECORD_TIME))
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
                            it.getLongOrNull(it.getColumnIndexOrThrow(TABLE_RECORD_RELATED_ID))
                        )
                        put(
                            TABLE_RECORD_RELATED_RECORD_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_RECORD_RELATED_RECORD_ID))
                        )
                        put(
                            TABLE_RECORD_RELATED_RELATED_RECORD_ID,
                            it.getLong(
                                it.getColumnIndexOrThrow(TABLE_RECORD_RELATED_RELATED_RECORD_ID)
                            )
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
                            it.getLongOrNull(it.getColumnIndexOrThrow(TABLE_TAG_ID))
                        )
                        put(
                            TABLE_TAG_NAME,
                            it.getString(it.getColumnIndexOrThrow(TABLE_TAG_NAME))
                        )
                        put(
                            TABLE_TAG_BOOKS_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_TAG_BOOKS_ID))
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
                            it.getLongOrNull(it.getColumnIndexOrThrow(TABLE_TAG_RELATED_ID))
                        )
                        put(
                            TABLE_TAG_RELATED_RECORD_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_TAG_RELATED_RECORD_ID))
                        )
                        put(
                            TABLE_TAG_RELATED_TAG_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_TAG_RELATED_TAG_ID))
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
                            it.getLongOrNull(it.getColumnIndexOrThrow(TABLE_TYPE_ID))
                        )
                        put(
                            TABLE_TYPE_PARENT_ID,
                            it.getLong(it.getColumnIndexOrThrow(TABLE_TYPE_PARENT_ID))
                        )
                        put(
                            TABLE_TYPE_NAME,
                            it.getString(it.getColumnIndexOrThrow(TABLE_TYPE_NAME))
                        )
                        put(
                            TABLE_TYPE_ICON_NAME,
                            it.getString(it.getColumnIndexOrThrow(TABLE_TYPE_ICON_NAME))
                        )
                        put(
                            TABLE_TYPE_TYPE_LEVEL,
                            it.getString(it.getColumnIndexOrThrow(TABLE_TYPE_TYPE_LEVEL))
                        )
                        put(
                            TABLE_TYPE_TYPE_CATEGORY,
                            it.getString(it.getColumnIndexOrThrow(TABLE_TYPE_TYPE_CATEGORY))
                        )
                        put(
                            TABLE_TYPE_PROTECTED,
                            it.getInt(it.getColumnIndexOrThrow(TABLE_TYPE_PROTECTED))
                        )
                        put(TABLE_TYPE_SORT, it.getInt(it.getColumnIndexOrThrow(TABLE_TYPE_SORT)))
                    },
                )
            }
        }
    }
}