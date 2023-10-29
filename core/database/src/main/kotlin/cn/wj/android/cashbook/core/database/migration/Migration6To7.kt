package cn.wj.android.cashbook.core.database.migration

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET
import cn.wj.android.cashbook.core.database.table.TABLE_BOOKS
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_RELATED
import cn.wj.android.cashbook.core.database.table.TABLE_TAG
import cn.wj.android.cashbook.core.database.table.TABLE_TAG_RELATED
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import java.math.BigDecimal
import org.intellij.lang.annotations.Language

/**
 * 数据库升级 6 -> 7
 * - db_asset：新增 balance 字段，移除 create_time 字段
 * - db_books：移除 image_url、currency、selected、createTime 字段
 * - db_record：移除 type_enum、record_id、tag_ids、system、create_time、modify_time，新增 concessions 字段
 * - db_type：移除 shared 字段
 * - db_tag_with_record：新增表
 * - db_type：移除 child_enable、refund、reimburse 字段，新增 protected 字段，重命名 icon_res_name->icon_name、type->type_level、record_type->type_category
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/1
 */
object Migration6To7 : Migration(6, 7) {

    override fun migrate(database: SupportSQLiteDatabase) = with(database) {
        migrateAsset()
        migrateBooks()
        migrateTag()
        migrateTagRelated()
        migrateType()
        migrateRecordRelated()
        migrateRecord()
    }

    /** 创建资产表，版本 7 */
    @Language("SQL")
    private const val SQL_CREATE_TABLE_ASSET_7 = """
        CREATE TABLE IF NOT EXISTS `${TABLE_ASSET}` 
        (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT,
            `books_id` INTEGER NOT NULL,
            `name` TEXT NOT NULL,
            `balance` REAL NOT NULL,
            `total_amount` REAL NOT NULL, 
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

    /** 查询指定资产 id 的最后一条修改余额记录 */
    @Language("SQL")
    private const val SQL_QUERY_LAST_MODIFY_RECORD_BY_ASSET_ID = """
        SELECT `amount`, `record_time` 
        FROM $TABLE_RECORD
        WHERE `asset_id` = %1${'$'}s
        AND `type_enum` = 'MODIFY_BALANCE'
        ORDER BY `record_time` 
        DESC LIMIT 1
    """

    /** 查询指定资产 id 的指定时间之后的所有记录 */
    @Language("SQL")
    private const val SQL_QUERY_RECORD_BY_ASSET_ID_AFTER_TIME = """
        SELECT `type_enum`, `amount`, `charge`
        FROM $TABLE_RECORD
        WHERE `asset_id` = %1${"$"}s
        AND `record_time` >= %2${"$"}s
    """

    /** 查询指定资产 id 的指定时间之后的所有转入记录 */
    @Language("SQL")
    private const val SQL_QUERY_TRANSFER_RECORD_BY_ASSET_ID_AFTER_TIME = """
        SELECT `type_enum`, `amount`, `charge`
        FROM $TABLE_RECORD
        WHERE `into_asset_id` = %1${"$"}s
        AND `type_enum` = 'TRANSFER'
        AND `record_time` >= %2${"$"}s
    """

    /**
     * 升级资产表
     *  - db_asset：新增 balance 字段，移除 create_time 字段
     */
    private fun SupportSQLiteDatabase.migrateAsset() {
        // 重命名资产旧表
        execSQL(SQL_RENAME_TABLE_ASSET_TO_TEMP)
        // 创建资产新表
        execSQL(SQL_CREATE_TABLE_ASSET_7)
        // 复制数据到新表
        query(SQL_QUERY_ALL_FROM_ASSET_TEMP).use {
            while (it.moveToNext()) {
                // 计算余额
                val assetId = it.getLong(it.getColumnIndexOrThrow("id"))
                val type =
                    ClassificationTypeEnum.valueOf(it.getString(it.getColumnIndexOrThrow("type")))
                val isCreditCard = type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT
                // 查询最后一条修改记录
                var balance = BigDecimal.ZERO
                var lastModifyRecordTime = 0L
                query(SQL_QUERY_LAST_MODIFY_RECORD_BY_ASSET_ID.format(assetId)).use { last ->
                    while (last.moveToNext()) {
                        balance = last.getDouble(last.getColumnIndexOrThrow("amount"))
                            .toBigDecimalOrZero()
                        lastModifyRecordTime =
                            last.getLong(last.getColumnIndexOrThrow("record_time"))
                    }
                }
                if (lastModifyRecordTime > 0L) {
                    // 有修改记录，查询修改记录之后的所有记录
                    query(
                        SQL_QUERY_RECORD_BY_ASSET_ID_AFTER_TIME.format(
                            assetId,
                            lastModifyRecordTime
                        )
                    ).use { after ->
                        while (after.moveToNext()) {
                            val typeEnum = after.getString(after.getColumnIndexOrThrow("type_enum"))
                            val amount = after.getDouble(after.getColumnIndexOrThrow("amount"))
                            when (typeEnum) {
                                "INCOME" -> {
                                    if (isCreditCard) {
                                        balance -= amount.toBigDecimalOrZero()
                                    } else {
                                        balance += amount.toBigDecimalOrZero()
                                    }
                                }

                                "EXPENDITURE" -> {
                                    if (isCreditCard) {
                                        balance += amount.toBigDecimalOrZero()
                                    } else {
                                        balance -= amount.toBigDecimalOrZero()
                                    }
                                }

                                "TRANSFER" -> {
                                    val charge =
                                        after.getDouble(after.getColumnIndexOrThrow("charge"))
                                    if (isCreditCard) {
                                        balance += amount.toBigDecimalOrZero()
                                        balance += charge.toBigDecimalOrZero()
                                    } else {
                                        balance -= amount.toBigDecimalOrZero()
                                        balance -= charge.toBigDecimalOrZero()
                                    }
                                }
                            }
                        }
                    }
                    // 查询转账转入数据
                    query(
                        SQL_QUERY_TRANSFER_RECORD_BY_ASSET_ID_AFTER_TIME.format(
                            assetId,
                            lastModifyRecordTime
                        )
                    ).use { transfer ->
                        while (transfer.moveToNext()) {
                            val typeEnum =
                                transfer.getString(transfer.getColumnIndexOrThrow("type_enum"))
                            val amount =
                                transfer.getDouble(transfer.getColumnIndexOrThrow("amount"))
                            if (typeEnum == "TRANSFER") {
                                if (isCreditCard) {
                                    // 信用卡，减少欠款
                                    balance -= amount.toBigDecimalOrZero()
                                } else {
                                    balance += amount.toBigDecimalOrZero()
                                }
                            }
                        }
                    }
                }
                insert(
                    table = TABLE_ASSET,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                    values = ContentValues().apply {
                        put("id", assetId)
                        put("books_id", it.getLong(it.getColumnIndexOrThrow("books_id")))
                        put("name", it.getString(it.getColumnIndexOrThrow("name")))
                        put("balance", balance.toDouble())
                        put("total_amount", it.getDouble(it.getColumnIndexOrThrow("total_amount")))
                        put("billing_date", it.getString(it.getColumnIndexOrThrow("billing_date")))
                        put(
                            "repayment_date",
                            it.getString(it.getColumnIndexOrThrow("repayment_date"))
                        )
                        put("type", type.ordinal)
                        put(
                            "classification",
                            AssetClassificationEnum.valueOf(it.getString(it.getColumnIndexOrThrow("classification"))).ordinal
                        )
                        put("invisible", it.getInt(it.getColumnIndexOrThrow("invisible")))
                        put("open_bank", it.getString(it.getColumnIndexOrThrow("open_bank")))
                        put("card_no", it.getString(it.getColumnIndexOrThrow("card_no")))
                        put("remark", it.getString(it.getColumnIndexOrThrow("remark")))
                        put("sort", it.getInt(it.getColumnIndexOrThrow("sort")))
                        put("modify_time", it.getLong(it.getColumnIndexOrThrow("modify_time")))
                    },
                )
            }
        }
        // 删除旧表
        execSQL(SQL_DROP_TABLE_ASSET_TEMP)
    }

    /** 创建账本表，版本 7 */
    @Language("SQL")
    private const val SQL_CREATE_TABLE_BOOKS_7 = """
        CREATE TABLE IF NOT EXISTS `${TABLE_BOOKS}` 
        (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT, 
            `name` TEXT NOT NULL, 
            `description` TEXT NOT NULL, 
            `modify_time` INTEGER NOT NULL
        )
    """

    /**
     * 升级账本表
     * - db_books：移除 image_url、currency、selected、createTime 字段
     */
    private fun SupportSQLiteDatabase.migrateBooks() {
        // 重命名账本旧表
        execSQL(SQL_RENAME_TABLE_BOOKS_TO_TEMP)
        // 创建账本新表
        execSQL(SQL_CREATE_TABLE_BOOKS_7)
        // 复制数据到新表
        query(SQL_QUERY_ALL_FROM_BOOKS_TEMP).use {
            while (it.moveToNext()) {
                insert(
                    table = TABLE_BOOKS,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                    values = ContentValues().apply {
                        put("id", it.getLong(it.getColumnIndexOrThrow("id")))
                        put("name", it.getString(it.getColumnIndexOrThrow("name")))
                        put("description", it.getString(it.getColumnIndexOrThrow("description")))
                        put("modify_time", it.getLong(it.getColumnIndexOrThrow("modify_time")))
                    },
                )
            }
        }
        // 删除旧表
        execSQL(SQL_DROP_TABLE_BOOKS_TEMP)
    }

    /** 创建标签表，版本 7 */
    @Language("SQL")
    private const val SQL_CREATE_TABLE_TAG_7 = """
        CREATE TABLE IF NOT EXISTS `${TABLE_TAG}` 
        (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT, 
            `name` TEXT NOT NULL, 
            `books_id` INTEGER NOT NULL
        )
    """

    /** 从标签临时表复制数据到新表 */
    @Language("SQL")
    private const val SQL_COPY_TABLE_TAG_FROM_6_7 = """
        INSERT INTO `${TABLE_TAG}` 
        SELECT `id`, `name`, `books_id`
        FROM `${TABLE_TAG}_temp`
    """

    /**
     * 升级标签表
     * - db_tag：移除 shared 字段
     */
    private fun SupportSQLiteDatabase.migrateTag() {
        // 重命名旧表
        execSQL(SQL_RENAME_TABLE_TAG_TO_TEMP)
        // 创建新表
        execSQL(SQL_CREATE_TABLE_TAG_7)
        // 从旧表复制数据到新表
        execSQL(SQL_COPY_TABLE_TAG_FROM_6_7)
        // 删除旧表
        execSQL(SQL_DROP_TABLE_TAG_TEMP)
    }

    /** 创建标签关联表，版本 7 */
    @Language("SQL")
    private const val SQL_CREATE_TABLE_TAG_RELATED_7 = """
        CREATE TABLE IF NOT EXISTS `${TABLE_TAG_RELATED}` 
        (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT, 
            `record_id` INTEGER NOT NULL, 
            `tag_id` INTEGER NOT NULL
        )
    """

    /** 查询记录表中 `tag_ids` 字段不为空的记录 `id` 及 `tag_ids` 数据*/
    @Language("SQL")
    private const val SQL_QUERY_TAG_IDS_FROM_RECORD_WHERE_TAG_IDS_NOT_EMPTY = """
        SELECT `id`, `tag_ids` 
        FROM `${TABLE_RECORD}`
        WHERE `tag_ids` != ''
    """

    private fun SupportSQLiteDatabase.migrateTagRelated() {
        // 创建表
        execSQL(SQL_CREATE_TABLE_TAG_RELATED_7)
        // 查询有关联标签的记录数据
        query(SQL_QUERY_TAG_IDS_FROM_RECORD_WHERE_TAG_IDS_NOT_EMPTY).use {
            while (it.moveToNext()) {
                val recordId = it.getLong(it.getColumnIndexOrThrow("id"))
                val tagIds = it.getString(it.getColumnIndexOrThrow("tag_ids"))
                tagIds.split(",").forEach { tagId ->
                    // 插入关联数据
                    insert(
                        table = TABLE_TAG_RELATED,
                        conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                        values = ContentValues().apply {
                            put("record_id", recordId)
                            put("tag_id", tagId.toLong())
                        }
                    )
                }
            }
        }
    }

    /** 创建类型表，版本 7 */
    @Language("SQL")
    private const val SQL_CREATE_TABLE_TYPE_7 = """
        CREATE TABLE IF NOT EXISTS `${TABLE_TYPE}` 
        (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT,
            `parent_id` INTEGER NOT NULL, 
            `name` TEXT NOT NULL, 
            `icon_name` TEXT NOT NULL, 
            `type_level` INTEGER NOT NULL, 
            `type_category` INTEGER NOT NULL, 
            `protected` INTEGER NOT NULL, 
            `sort` INTEGER NOT NULL
        )
    """

    /**
     * 升级类型表
     * - db_type：移除 child_enable、refund、reimburse 字段，新增 protected 字段，重命名 icon_res_name->icon_name、type->type_level、record_type->type_category
     */
    private fun SupportSQLiteDatabase.migrateType() {
        // 重命名旧表
        execSQL(SQL_RENAME_TABLE_TYPE_TO_TEMP)
        // 创建新表
        execSQL(SQL_CREATE_TABLE_TYPE_7)
        // 从旧表中查询数据
        query(SQL_QUERY_ALL_FROM_TYPE_TEMP).use {
            while (it.moveToNext()) {
                insert(
                    table = TABLE_TYPE,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                    values = ContentValues().apply {
                        put("id", it.getLong(it.getColumnIndexOrThrow("id")))
                        put("parent_id", it.getLong(it.getColumnIndexOrThrow("parent_id")))
                        put("name", it.getString(it.getColumnIndexOrThrow("name")))
                        put(
                            "icon_name",
                            it.getString(it.getColumnIndexOrThrow("icon_res_name"))
                                .replace("@drawable/", "")
                        )
                        put(
                            "type_level",
                            TypeLevelEnum.valueOf(it.getString(it.getColumnIndexOrThrow("type"))).ordinal
                        )
                        put("type_category", it.getInt(it.getColumnIndexOrThrow("record_type")))
                        val refund = it.getInt(it.getColumnIndexOrThrow("refund"))
                        val reimburse = it.getInt(it.getColumnIndexOrThrow("reimburse"))
                        val protected = if (refund + reimburse > 0) {
                            SWITCH_INT_ON
                        } else {
                            SWITCH_INT_OFF
                        }
                        put("protected", protected)
                        put("sort", it.getInt(it.getColumnIndexOrThrow("sort")))
                    },
                )
            }
        }
        // 删除旧表
        execSQL(SQL_DROP_TABLE_TYPE_TEMP)
    }

    /** 创建记录关联表，版本 7 */
    @Language("SQL")
    private const val SQL_CREATE_TABLE_RECORD_RELATED_7 = """
        CREATE TABLE IF NOT EXISTS `${TABLE_RECORD_RELATED}` 
        (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT, 
            `record_id` INTEGER NOT NULL, 
            `related_record_id` INTEGER NOT NULL
        )
    """

    /** 查询记录表中 `tag_ids` 字段不为空的记录 `id` 及 `tag_ids` 数据*/
    @Language("SQL")
    private const val SQL_QUERY_RECORD_ID_FROM_RECORD_WHERE_RECORD_ID_NOT_EMPTY = """
        SELECT `id`, `record_id` 
        FROM `${TABLE_RECORD}`
        WHERE `record_id` > 0
    """

    /**
     * 升级记录关联表
     * - 新增表
     */
    private fun SupportSQLiteDatabase.migrateRecordRelated() {
        // 创建新表
        execSQL(SQL_CREATE_TABLE_RECORD_RELATED_7)
        // 获取有关联记录的记录
        query(SQL_QUERY_RECORD_ID_FROM_RECORD_WHERE_RECORD_ID_NOT_EMPTY).use {
            while (it.moveToNext()) {
                // 插入关联信息
                insert(
                    table = TABLE_RECORD_RELATED,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                    values = ContentValues().apply {
                        put("record_id", it.getLong(it.getColumnIndexOrThrow("id")))
                        put("related_record_id", it.getLong(it.getColumnIndexOrThrow("record_id")))
                    },
                )
            }
        }
    }

    /** 创建记录表，版本 7 */
    @Language("SQL")
    private const val SQL_CREATE_TABLE_RECORD_7 = """
        CREATE TABLE IF NOT EXISTS `${TABLE_RECORD}` 
        (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT, 
            `type_id` INTEGER NOT NULL, 
            `asset_id` INTEGER NOT NULL, 
            `into_asset_id` INTEGER NOT NULL, 
            `books_id` INTEGER NOT NULL, 
            `amount` REAL NOT NULL, 
            `concessions` REAL NOT NULL, 
            `charge` REAL NOT NULL, 
            `remark` TEXT NOT NULL, 
            `reimbursable` INTEGER NOT NULL, 
            `record_time` INTEGER NOT NULL
        )
    """

    /**
     * 升级记录表
     * - 升级记录表之前需先更新 记录关联表和标签关联表
     * - db_record：移除 type_enum、record_id、tag_ids、system、create_time、modify_time，新增 concessions 字段
     */
    private fun SupportSQLiteDatabase.migrateRecord() {
        // 重命名旧表
        execSQL(SQL_RENAME_TABLE_RECORD_TO_TEMP)
        // 创建新表
        execSQL(SQL_CREATE_TABLE_RECORD_7)
        // 从旧表中查询数据
        query(SQL_QUERY_ALL_FROM_RECORD_TEMP).use {
            while (it.moveToNext()) {
                insert(
                    table = TABLE_RECORD,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                    values = ContentValues().apply {
                        put("id", it.getLong(it.getColumnIndexOrThrow("id")))
                        put("type_id", it.getLong(it.getColumnIndexOrThrow("type_id")))
                        put("asset_id", it.getLong(it.getColumnIndexOrThrow("asset_id")))
                        put("into_asset_id", it.getLong(it.getColumnIndexOrThrow("into_asset_id")))
                        put("books_id", it.getLong(it.getColumnIndexOrThrow("books_id")))
                        put("amount", it.getDouble(it.getColumnIndexOrThrow("amount")))
                        put("concessions", 0.0)
                        put("charge", it.getDouble(it.getColumnIndexOrThrow("charge")))
                        put("remark", it.getString(it.getColumnIndexOrThrow("remark")))
                        put("reimbursable", it.getInt(it.getColumnIndexOrThrow("reimbursable")))
                        put("record_time", it.getString(it.getColumnIndexOrThrow("record_time")))
                    },
                )
            }
        }
    }
}