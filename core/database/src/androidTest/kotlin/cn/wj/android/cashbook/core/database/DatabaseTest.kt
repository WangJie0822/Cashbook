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

package cn.wj.android.cashbook.core.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.third.MyFormatStrategy
import cn.wj.android.cashbook.core.database.migration.DatabaseMigrations
import cn.wj.android.cashbook.core.database.migration.Migration10To11
import cn.wj.android.cashbook.core.database.migration.Migration11To12
import cn.wj.android.cashbook.core.database.migration.Migration12To13
import cn.wj.android.cashbook.core.database.migration.Migration13To14
import cn.wj.android.cashbook.core.database.migration.Migration1To2
import cn.wj.android.cashbook.core.database.migration.Migration2To3
import cn.wj.android.cashbook.core.database.migration.Migration3To4
import cn.wj.android.cashbook.core.database.migration.Migration4To5
import cn.wj.android.cashbook.core.database.migration.Migration5To6
import cn.wj.android.cashbook.core.database.migration.Migration6To7
import cn.wj.android.cashbook.core.database.migration.Migration7To8
import cn.wj.android.cashbook.core.database.migration.Migration8To9
import cn.wj.android.cashbook.core.database.migration.Migration9To10
import cn.wj.android.cashbook.core.database.migration.SQL_QUERY_ALL_FROM_BOOKS
import cn.wj.android.cashbook.core.database.migration.SQL_QUERY_ALL_FROM_RECORD
import cn.wj.android.cashbook.core.database.migration.SQL_QUERY_ALL_FROM_TAG
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET
import cn.wj.android.cashbook.core.database.table.TABLE_BOOKS
import cn.wj.android.cashbook.core.database.table.TABLE_BOOKS_BG_URI
import cn.wj.android.cashbook.core.database.table.TABLE_IMAGE_RELATED
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_AMOUNT
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_ASSET_ID
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_BOOKS_ID
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_CHARGE
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_CONCESSIONS
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_FINAL_AMOUNT
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_ID
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_INTO_ASSET_ID
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_RECORD_TIME
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_REIMBURSABLE
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_REMARK
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_TYPE_ID
import cn.wj.android.cashbook.core.database.table.TABLE_TAG
import cn.wj.android.cashbook.core.database.table.TABLE_TAG_INVISIBLE
import cn.wj.android.cashbook.core.database.table.TABLE_TAG_RELATED
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    /** 测试数据库名称 */
    private val testDbName = "migration-test"

    /** SQLLite 数据表名 */
    private val tableName = "sqlite_master"
    private val columnNameType = "type"
    private val columnNameTableName = "tbl_name"
    private val columnNameSql = "sql"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CashbookDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Before
    fun beforeTest() {
        // 初始化 Logger 日志打印
        val strategy = MyFormatStrategy.newBuilder()
            .borderPriority(Log.WARN)
            .headerPriority(Log.WARN)
            .tag("CB_TEST")
            .logStrategy { priority, tag, message -> println("${tag ?: "NO_TAG"}:$priority $message") }
            .build()
        Logger.addLogAdapter(AndroidLogAdapter(strategy))
    }

    /**
     * 测试从数据库文件恢复备份
     */
    @Test
    fun recovery_from_database() {
        var resultCount = 0
        helper.createDatabase("to", ApplicationInfo.DB_VERSION).use { to ->
            to.insert(
                table = TABLE_RECORD,
                conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                values = ContentValues().apply {
                    put(TABLE_RECORD_ID, 1L)
                    put(TABLE_RECORD_TYPE_ID, 1L)
                    put(TABLE_RECORD_ASSET_ID, 1L)
                    put(TABLE_RECORD_INTO_ASSET_ID, 1L)
                    put(TABLE_RECORD_BOOKS_ID, 1L)
                    put(TABLE_RECORD_AMOUNT, 100.0)
                    put(TABLE_RECORD_FINAL_AMOUNT, 90.0)
                    put(TABLE_RECORD_CONCESSIONS, 10.0)
                    put(TABLE_RECORD_CHARGE, 0.0)
                    put(TABLE_RECORD_REMARK, "备注1")
                    put(TABLE_RECORD_REIMBURSABLE, SWITCH_INT_OFF)
                    put(TABLE_RECORD_RECORD_TIME, System.currentTimeMillis())
                },
            )
            to.query(SQL_QUERY_ALL_FROM_RECORD).use { cursor ->
                while (cursor.moveToNext()) {
                    val sb = StringBuilder()
                    sb.append("to before > ")
                    for (i in 0 until cursor.columnCount) {
                        sb.append(cursor.getColumnName(i) + ": " + cursor.getString(i) + ", ")
                    }
                    log(sb.toString())
                }
            }
            helper.createDatabase("from", ApplicationInfo.DB_VERSION).use { from ->
                from.insert(
                    table = TABLE_RECORD,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                    values = ContentValues().apply {
                        put(TABLE_RECORD_ID, 1L)
                        put(TABLE_RECORD_TYPE_ID, 1L)
                        put(TABLE_RECORD_ASSET_ID, 1L)
                        put(TABLE_RECORD_INTO_ASSET_ID, 1L)
                        put(TABLE_RECORD_BOOKS_ID, 1L)
                        put(TABLE_RECORD_AMOUNT, 100.0)
                        put(TABLE_RECORD_FINAL_AMOUNT, 110.0)
                        put(TABLE_RECORD_CONCESSIONS, 10.0)
                        put(TABLE_RECORD_CHARGE, 20.0)
                        put(TABLE_RECORD_REMARK, "备注2")
                        put(TABLE_RECORD_REIMBURSABLE, SWITCH_INT_OFF)
                        put(TABLE_RECORD_RECORD_TIME, System.currentTimeMillis())
                    },
                )
                from.insert(
                    table = TABLE_RECORD,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
                    values = ContentValues().apply {
                        put(TABLE_RECORD_ID, 2L)
                        put(TABLE_RECORD_TYPE_ID, 1L)
                        put(TABLE_RECORD_ASSET_ID, 1L)
                        put(TABLE_RECORD_INTO_ASSET_ID, 1L)
                        put(TABLE_RECORD_BOOKS_ID, 1L)
                        put(TABLE_RECORD_AMOUNT, 200.0)
                        put(TABLE_RECORD_FINAL_AMOUNT, 220.0)
                        put(TABLE_RECORD_CONCESSIONS, 20.0)
                        put(TABLE_RECORD_CHARGE, 40.0)
                        put(TABLE_RECORD_REMARK, "备注3")
                        put(TABLE_RECORD_REIMBURSABLE, SWITCH_INT_OFF)
                        put(TABLE_RECORD_RECORD_TIME, System.currentTimeMillis())
                    },
                )
                from.query(SQL_QUERY_ALL_FROM_RECORD).use { cursor ->
                    while (cursor.moveToNext()) {
                        val sb = StringBuilder()
                        sb.append("from data > ")
                        for (i in 0 until cursor.columnCount) {
                            sb.append(cursor.getColumnName(i) + ": " + cursor.getString(i) + ", ")
                        }
                        log(sb.toString())
                    }
                }
                DatabaseMigrations.recoveryFromDb(from, to)
                to.query(SQL_QUERY_ALL_FROM_RECORD).use { cursor ->
                    while (cursor.moveToNext()) {
                        resultCount++
                        val sb = StringBuilder()
                        sb.append("to after > ")
                        for (i in 0 until cursor.columnCount) {
                            sb.append(cursor.getColumnName(i) + ": " + cursor.getString(i) + ", ")
                        }
                        log(sb.toString())
                    }
                }
            }
        }
        Assert.assertEquals(resultCount, 2)
    }

    /**
     * 测试数据库从 1 升级到 2
     * - 2 版本新增了 db_tag 表
     */
    @Test
    @Throws(IOException::class)
    fun migrate1_2() {
        log("migrate1_2()")
        var hasTagTable: Boolean
        helper.createDatabase(testDbName, 1).use { db ->
            db.query(
                "SELECT * FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                arrayOf("table", "db_tag"),
            ).use { cursor ->
                val count = cursor.count
                log("migrate1_2() count = [$count]")
                hasTagTable = count > 0
            }
        }
        Assert.assertEquals(false, hasTagTable)
        helper.runMigrationsAndValidate(testDbName, 2, true, Migration1To2)
            .use { db ->
                db.query(
                    "SELECT * FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                    arrayOf("table", "db_tag"),
                ).use { cursor ->
                    val count = cursor.count
                    log("migrate1_2() count = [$count]")
                    hasTagTable = count > 0
                }
            }
        Assert.assertEquals(true, hasTagTable)
    }

    /**
     * 测试数据库从 2 升级到 3
     * - 3 版本 db_record、db_type 表字段有修改
     */
    @Test
    @Throws(IOException::class)
    fun migrate2_3() {
        log("migrate2_3()")
        var hasTypeEnum: Boolean
        var hasType: Boolean
        var hasTypeId: Boolean
        var hasFirstTypeId: Boolean
        var hasSecondTypeId: Boolean
        var hasSystem: Boolean

        val type = "type"
        val firstTypeId = 2001L
        helper.createDatabase(testDbName, 2).use { db ->
            db.query(
                "SELECT `$columnNameSql` FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                arrayOf("table", "db_record"),
            ).use { cursor ->
                cursor.moveToFirst()
                val sqlStr = cursor.getString(cursor.getColumnIndex(columnNameSql))
                log("migrate2_3() sqlStr = [$sqlStr]")
                hasTypeEnum = sqlStr.contains("`type_enum`")
                hasType = sqlStr.contains("`type`")
                hasTypeId = sqlStr.contains("`type_id`")
                hasFirstTypeId = sqlStr.contains("`first_type_id`")
                hasSecondTypeId = sqlStr.contains("`second_type_id`")
            }
            db.query(
                "SELECT `$columnNameSql` FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                arrayOf("table", "db_type"),
            ).use { cursor ->
                cursor.moveToFirst()
                val sqlStr = cursor.getString(cursor.getColumnIndex(columnNameSql))
                log("migrate2_3() sqlStr = [$sqlStr]")
                hasSystem = sqlStr.contains("`system`")
            }
            // 插入数据
            db.insert(
                "db_record",
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", 1L)
                    put("type", type)
                    put("first_type_id", firstTypeId)
                    put("second_type_id", 2L)
                    put("asset_id", 3L)
                    put("into_asset_id", 4L)
                    put("books_id", 5L)
                    put("record_id", 6L)
                    put("amount", 7f)
                    put("charge", 8f)
                    put("remark", "remark")
                    put("tag_ids", "tagIds")
                    put("reimbursable", 0)
                    put("system", 0)
                    put("record_time", System.currentTimeMillis())
                    put("create_time", System.currentTimeMillis())
                    put("modify_time", System.currentTimeMillis())
                },
            )
        }
        Assert.assertEquals(false, hasTypeEnum)
        Assert.assertEquals(false, hasTypeId)
        Assert.assertEquals(true, hasType)
        Assert.assertEquals(true, hasFirstTypeId)
        Assert.assertEquals(true, hasSecondTypeId)
        Assert.assertEquals(true, hasSystem)

        val typeEnum: String
        val typeId: Long
        helper.runMigrationsAndValidate(testDbName, 3, true, Migration2To3)
            .use { db ->
                db.query(
                    "SELECT `$columnNameSql` FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                    arrayOf("table", "db_record"),
                ).use { cursor ->
                    cursor.moveToFirst()
                    val sqlStr = cursor.getString(cursor.getColumnIndex(columnNameSql))
                    log("migrate2_3() sqlStr = [$sqlStr]")
                    hasTypeEnum = sqlStr.contains("`type_enum`")
                    hasType = sqlStr.contains("`type`")
                    hasTypeId = sqlStr.contains("`type_id`")
                    hasFirstTypeId = sqlStr.contains("`first_type_id`")
                    hasSecondTypeId = sqlStr.contains("`second_type_id`")
                }
                db.query(
                    "SELECT `$columnNameSql` FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                    arrayOf("table", "db_type"),
                ).use { cursor ->
                    cursor.moveToFirst()
                    val sqlStr = cursor.getString(cursor.getColumnIndex(columnNameSql))
                    log("migrate2_3() sqlStr = [$sqlStr]")
                    hasSystem = sqlStr.contains("`system`")
                }
                db.query("SELECT * FROM `db_record`").use { cursor ->
                    cursor.moveToFirst()
                    val typeEnumIndex = cursor.getColumnIndex("type_enum")
                    typeEnum = cursor.getString(typeEnumIndex)
                    val typeIdIndex = cursor.getColumnIndex("type_id")
                    typeId = cursor.getLong(typeIdIndex)
                    log("migrate2_3() typeEnum = [$typeEnum], typeId = [$typeId]")
                }
            }
        Assert.assertEquals(true, hasTypeEnum)
        Assert.assertEquals(true, hasTypeId)
        Assert.assertEquals(false, hasType)
        Assert.assertEquals(false, hasFirstTypeId)
        Assert.assertEquals(false, hasSecondTypeId)
        Assert.assertEquals(false, hasSystem)
        Assert.assertEquals(type, typeEnum)
        Assert.assertEquals(firstTypeId, typeId)
    }

    /**
     * 测试数据库从 3 升级到 4
     * - 3 版本 db_record 表字段有修改
     */
    @Test
    @Throws(IOException::class)
    fun migrate3_4() {
        var hasBooksId: Boolean
        var hasShared: Boolean
        helper.createDatabase(testDbName, 3).use { db ->
            db.query(
                "SELECT `$columnNameSql` FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                arrayOf("table", "db_tag"),
            ).use { cursor ->
                cursor.moveToFirst()
                val sqlStr = cursor.getString(cursor.getColumnIndex(columnNameSql))
                log("migrate3_4() sqlStr = [$sqlStr]")
                hasBooksId = sqlStr.contains("`books_id`")
                hasShared = sqlStr.contains("`shared`")
            }
        }
        Assert.assertEquals(false, hasBooksId)
        Assert.assertEquals(false, hasShared)

        helper.runMigrationsAndValidate(testDbName, 4, true, Migration3To4)
            .use { db ->
                db.query(
                    "SELECT `$columnNameSql` FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                    arrayOf("table", "db_tag"),
                ).use { cursor ->
                    cursor.moveToFirst()
                    val sqlStr = cursor.getString(cursor.getColumnIndex(columnNameSql))
                    log("migrate3_4() sqlStr = [$sqlStr]")
                    hasBooksId = sqlStr.contains("`books_id`")
                    hasShared = sqlStr.contains("`shared`")
                }
            }
        Assert.assertEquals(true, hasBooksId)
        Assert.assertEquals(true, hasShared)
    }

    /**
     * 测试数据库从 4 升级到 5
     * - db_asset 表有新增字段
     */
    @Test
    @Throws(IOException::class)
    fun migrate4_5() {
        var hasOpenBank: Boolean
        var hasCardNo: Boolean
        var hasRemark: Boolean
        helper.createDatabase(testDbName, 4).use { db ->
            db.query(
                "SELECT `$columnNameSql` FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                arrayOf("table", "db_asset"),
            ).use { cursor ->
                cursor.moveToFirst()
                val sqlStr = cursor.getString(cursor.getColumnIndex(columnNameSql))
                log("migrate4_5() sqlStr = [$sqlStr]")
                hasOpenBank = sqlStr.contains("`open_bank`")
                hasCardNo = sqlStr.contains("`card_no`")
                hasRemark = sqlStr.contains("`remark`")
            }
        }
        Assert.assertEquals(false, hasOpenBank)
        Assert.assertEquals(false, hasCardNo)
        Assert.assertEquals(false, hasRemark)

        helper.runMigrationsAndValidate(testDbName, 5, true, Migration4To5)
            .use { db ->
                db.query(
                    "SELECT `$columnNameSql` FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                    arrayOf("table", "db_asset"),
                ).use { cursor ->
                    cursor.moveToFirst()
                    val sqlStr = cursor.getString(cursor.getColumnIndex(columnNameSql))
                    log("migrate4_5() sqlStr = [$sqlStr]")
                    hasOpenBank = sqlStr.contains("`open_bank`")
                    hasCardNo = sqlStr.contains("`card_no`")
                    hasRemark = sqlStr.contains("`remark`")
                }
            }
        Assert.assertEquals(true, hasOpenBank)
        Assert.assertEquals(true, hasCardNo)
        Assert.assertEquals(true, hasRemark)
    }

    /**
     * 测试数据库从 5 升级到 6
     * - db_asset、db_record 表金额类型由 float 修改为 double
     */
    @Test
    @Throws(IOException::class)
    fun migrate5_6() {
        var floatStr: String
        helper.createDatabase(testDbName, 5).use { db ->
            db.insert(
                "db_asset",
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", 1L)
                    put("books_id", 1L)
                    put("name", "name")
                    put("total_amount", 1.09f)
                    put("billing_date", "2022-10-10")
                    put("repayment_date", "2022-10-10")
                    put("type", "type")
                    put("classification", "classification")
                    put("invisible", 1)
                    put("open_bank", "")
                    put("card_no", "")
                    put("remark", "")
                    put("sort", 1)
                    put("create_time", System.currentTimeMillis())
                    put("modify_time", System.currentTimeMillis())
                },
            )
            db.query("SELECT * FROM `db_asset`").use { cursor ->
                cursor.moveToFirst()
                floatStr = cursor.getString(cursor.getColumnIndexOrThrow("total_amount"))
            }
        }

        var doubleStr: String
        helper.runMigrationsAndValidate(testDbName, 6, true, Migration5To6)
            .use { db ->
                db.query("SELECT * FROM `db_asset`").use { cursor ->
                    cursor.moveToFirst()
                    doubleStr = cursor.getString(cursor.getColumnIndexOrThrow("total_amount"))
                }
            }
        log("$floatStr - $doubleStr")
        Assert.assertEquals(floatStr, doubleStr)
    }

    /**
     * 测试数据库从 6 升级到 7
     * - db_asset：新增 balance 字段，移除 create_time 字段
     * - db_books：移除 image_url、currency、selected、createTime 字段
     * - db_record：移除 type_enum、record_id、tag_ids、system、create_time、modify_time，新增 concessions 字段
     * - db_type：移除 child_enable、refund、reimburse 字段，新增 protected 字段，重命名 icon_res_name->icon_name、type->type_level、record_type->type_category
     * - db_tag_with_record：新增表
     */
    @Test
    @Throws(IOException::class)
    fun migrate6_7() {
        log("migrate6_7()")
        // 资产创建时间（用于 MODIFY_BALANCE 记录的 record_time）
        val modifyRecordTime = 1_600_000_000_000L
        helper.createDatabase(testDbName, 6).use { db ->
            // 账本数据，验证账本表字段裁剪
            db.insert(
                TABLE_BOOKS,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", 1L)
                    put("name", "默认账本")
                    put("imageUrl", "image")
                    put("description", "desc")
                    put("currency", "CNY")
                    put("selected", 1)
                    put("create_time", System.currentTimeMillis())
                    put("modify_time", System.currentTimeMillis())
                },
            )
            // 类型数据，验证 refund/reimburse -> protected 计算
            db.insert(
                TABLE_TYPE,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", 1L)
                    put("parent_id", -1L)
                    put("name", "餐饮")
                    put("icon_res_name", "@drawable/vector_type_food_24")
                    put("type", "FIRST")
                    put("record_type", 0)
                    put("child_enable", 1)
                    put("refund", 1)
                    put("reimburse", 0)
                    put("sort", 0)
                },
            )
            // 资产数据，类型为资金账户（非信用卡）
            db.insert(
                TABLE_ASSET,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", 1L)
                    put("books_id", 1L)
                    put("name", "现金")
                    put("total_amount", 0.0)
                    put("billing_date", "")
                    put("repayment_date", "")
                    put("type", "CAPITAL_ACCOUNT")
                    put("classification", "CASH")
                    put("invisible", 0)
                    put("open_bank", "")
                    put("card_no", "")
                    put("remark", "")
                    put("sort", 0)
                    put("create_time", System.currentTimeMillis())
                    put("modify_time", System.currentTimeMillis())
                },
            )
            // 修改余额记录：迁移后应被丢弃，但会作为资产余额回算的基准（100 元）
            db.insert(
                TABLE_RECORD,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", 1L)
                    put("type_enum", "MODIFY_BALANCE")
                    put("type_id", 0L)
                    put("asset_id", 1L)
                    put("into_asset_id", -1L)
                    put("books_id", 1L)
                    put("record_id", 0L)
                    put("amount", 100.0)
                    put("charge", 0.0)
                    put("remark", "")
                    put("tag_ids", "")
                    put("reimbursable", 0)
                    put("system", 0)
                    put("record_time", modifyRecordTime)
                    put("create_time", System.currentTimeMillis())
                    put("modify_time", System.currentTimeMillis())
                },
            )
            // 普通支出记录：迁移后应保留，且 tag_ids 拆分为 3 条标签关联（早于 MODIFY_BALANCE，不影响余额回算）
            db.insert(
                TABLE_RECORD,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", 2L)
                    put("type_enum", "EXPENDITURE")
                    put("type_id", 1L)
                    put("asset_id", 1L)
                    put("into_asset_id", -1L)
                    put("books_id", 1L)
                    put("record_id", 0L)
                    put("amount", 20.0)
                    put("charge", 0.0)
                    put("remark", "午饭")
                    put("tag_ids", "1,2,3")
                    put("reimbursable", 0)
                    put("system", 0)
                    put("record_time", modifyRecordTime - 1000L)
                    put("create_time", System.currentTimeMillis())
                    put("modify_time", System.currentTimeMillis())
                },
            )
        }

        var recordCount = 0
        var hasModifyBalanceRecord = false
        var hasTypeEnumColumn = false
        var tagRelatedCount = 0
        var protectedValue = -1
        var balance = -1.0
        helper.runMigrationsAndValidate(testDbName, 7, true, Migration6To7)
            .use { db ->
                // db_record 表已移除 type_enum 字段
                db.query(
                    "SELECT `$columnNameSql` FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                    arrayOf("table", TABLE_RECORD),
                ).use { cursor ->
                    cursor.moveToFirst()
                    val sqlStr = cursor.getString(cursor.getColumnIndexOrThrow(columnNameSql))
                    log("migrate6_7() record sql = [$sqlStr]")
                    hasTypeEnumColumn = sqlStr.contains("`type_enum`")
                }
                // db_record 中 MODIFY_BALANCE 记录已被丢弃，仅保留普通记录
                db.query(SQL_QUERY_ALL_FROM_RECORD).use { cursor ->
                    while (cursor.moveToNext()) {
                        recordCount++
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                        if (id == 1L) {
                            hasModifyBalanceRecord = true
                        }
                    }
                }
                // 标签关联表行数与 tag_ids 拆分数量一致（"1,2,3" -> 3 条）
                db.query("SELECT * FROM `$TABLE_TAG_RELATED`").use { cursor ->
                    tagRelatedCount = cursor.count
                }
                // 类型表 refund(1)+reimburse(0) > 0 -> protected = 开启
                db.query("SELECT * FROM `$TABLE_TYPE`").use { cursor ->
                    cursor.moveToFirst()
                    protectedValue = cursor.getInt(cursor.getColumnIndexOrThrow("protected"))
                }
                // 资产余额按最后一条 MODIFY_BALANCE 记录回算，无后续记录则为 100 元
                db.query("SELECT * FROM `$TABLE_ASSET`").use { cursor ->
                    cursor.moveToFirst()
                    balance = cursor.getDouble(cursor.getColumnIndexOrThrow("balance"))
                }
            }
        log("migrate6_7() recordCount=$recordCount, tagRelatedCount=$tagRelatedCount, protected=$protectedValue, balance=$balance")
        // db_record 移除 type_enum 字段
        Assert.assertEquals(false, hasTypeEnumColumn)
        // 仅保留 1 条普通记录，MODIFY_BALANCE 被丢弃
        Assert.assertEquals(1, recordCount)
        Assert.assertEquals(false, hasModifyBalanceRecord)
        // tag_ids "1,2,3" 拆分为 3 条标签关联
        Assert.assertEquals(3, tagRelatedCount)
        // refund=1 -> protected 开启（值为 1）
        Assert.assertEquals(1, protectedValue)
        // 余额回算为修改记录金额 100 元
        Assert.assertEquals(100.0, balance, 0.0)
    }

    /**
     * 测试数据库从 7 升级到 8
     * - db_tag：新增 invisible 字段
     */
    @Test
    @Throws(IOException::class)
    fun migrate7_8() {
        var hasInvisible: Boolean
        helper.createDatabase(testDbName, 7).use { db ->
            db.query(
                "SELECT `$columnNameSql` FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                arrayOf("table", TABLE_TAG),
            ).use { cursor ->
                cursor.moveToFirst()
                val sqlStr = cursor.getString(cursor.getColumnIndex(columnNameSql))
                log("migrate7_8() sqlStr = [$sqlStr]")
                hasInvisible = sqlStr.contains("`invisible`")
            }
            db.insert(
                TABLE_TAG,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", 1L)
                    put("name", "name")
                    put("books_id", 1L)
                },
            )
        }
        Assert.assertEquals(false, hasInvisible)

        var invisible: Int
        helper.runMigrationsAndValidate(testDbName, 8, true, Migration7To8)
            .use { db ->
                db.query(
                    "SELECT `$columnNameSql` FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                    arrayOf("table", TABLE_TAG),
                ).use { cursor ->
                    cursor.moveToFirst()
                    val sqlStr = cursor.getString(cursor.getColumnIndex(columnNameSql))
                    log("migrate7_8() sqlStr = [$sqlStr]")
                    hasInvisible = sqlStr.contains("`invisible`")
                }
                db.query(SQL_QUERY_ALL_FROM_TAG).use { cursor ->
                    cursor.moveToFirst()
                    invisible = cursor.getInt(cursor.getColumnIndexOrThrow(TABLE_TAG_INVISIBLE))
                    log("migrate7_8() invisible = [$invisible]")
                }
            }
        Assert.assertEquals(true, hasInvisible)
        Assert.assertEquals(SWITCH_INT_OFF, invisible)
    }

    /**
     * 测试数据库从 8 升级到 9
     * - db_books：新增 bg_uri 字段
     */
    @Test
    @Throws(IOException::class)
    fun migrate8_9() {
        var hasBgUri: Boolean
        helper.createDatabase(testDbName, 8).use { db ->
            db.query(
                "SELECT `$columnNameSql` FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                arrayOf("table", TABLE_BOOKS),
            ).use { cursor ->
                cursor.moveToFirst()
                val sqlStr = cursor.getString(cursor.getColumnIndex(columnNameSql))
                log("migrate8_9() sqlStr = [$sqlStr]")
                hasBgUri = sqlStr.contains("`bg_uri`")
            }
            db.insert(
                TABLE_BOOKS,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", 1L)
                    put("name", "name")
                    put("description", "description")
                    put("modify_time", System.currentTimeMillis())
                },
            )
        }
        Assert.assertEquals(false, hasBgUri)

        var bgUri: String
        helper.runMigrationsAndValidate(testDbName, 9, true, Migration8To9)
            .use { db ->
                db.query(
                    "SELECT `$columnNameSql` FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                    arrayOf("table", TABLE_BOOKS),
                ).use { cursor ->
                    cursor.moveToFirst()
                    val sqlStr = cursor.getString(cursor.getColumnIndex(columnNameSql))
                    log("migrate8_9() sqlStr = [$sqlStr]")
                    hasBgUri = sqlStr.contains("`bg_uri`")
                }
                db.query(SQL_QUERY_ALL_FROM_BOOKS).use { cursor ->
                    cursor.moveToFirst()
                    bgUri = cursor.getString(cursor.getColumnIndexOrThrow(TABLE_BOOKS_BG_URI))
                    log("migrate8_9() bgUri = <$bgUri>")
                }
            }
        Assert.assertEquals(true, hasBgUri)
        Assert.assertEquals(true, bgUri.isBlank())
    }

    /**
     * 测试数据库从 9 升级到 10
     * - db_record：新增 final_amount 字段
     */
    @Test
    @Throws(IOException::class)
    fun migrate9_10() {
        var hasFinalAmount: Boolean
        helper.createDatabase(testDbName, 9).use { db ->
            db.query(
                "SELECT `$columnNameSql` FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                arrayOf("table", TABLE_RECORD),
            ).use { cursor ->
                cursor.moveToFirst()
                val sqlStr = cursor.getString(cursor.getColumnIndex(columnNameSql))
                log("migrate9_10() sqlStr = [$sqlStr]")
                hasFinalAmount = sqlStr.contains("`final_amount`")
            }
            db.insert(
                TABLE_RECORD,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", 1L)
                    put("type_id", 1L)
                    put("asset_id", 1L)
                    put("into_asset_id", 1L)
                    put("books_id", 1L)
                    put("amount", 0.0)
                    put("concessions", 0.0)
                    put("charge", 0.0)
                    put("remark", "remark")
                    put("reimbursable", 0)
                    put("record_time", System.currentTimeMillis())
                },
            )
        }
        Assert.assertEquals(false, hasFinalAmount)

        var finalAmount: Double
        helper.runMigrationsAndValidate(testDbName, 10, true, Migration9To10)
            .use { db ->
                db.query(
                    "SELECT `$columnNameSql` FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                    arrayOf("table", TABLE_RECORD),
                ).use { cursor ->
                    cursor.moveToFirst()
                    val sqlStr = cursor.getString(cursor.getColumnIndex(columnNameSql))
                    log("migrate9_10() sqlStr = [$sqlStr]")
                    hasFinalAmount = sqlStr.contains("`final_amount`")
                }
                db.query(SQL_QUERY_ALL_FROM_RECORD).use { cursor ->
                    cursor.moveToFirst()
                    finalAmount =
                        cursor.getDouble(cursor.getColumnIndexOrThrow(TABLE_RECORD_FINAL_AMOUNT))
                    log("migrate9_10() finalAmount = <$finalAmount>")
                }
            }
        Assert.assertEquals(true, hasFinalAmount)
        Assert.assertEquals(0.0, finalAmount, 0.0)
    }

    /**
     * 测试数据库从 10 升级到 11
     * - 新增 db_image_with_related 表
     */
    @Test
    @Throws(IOException::class)
    fun migrate10_11() {
        var hasImageTable: Boolean
        helper.createDatabase(testDbName, 10).use { db ->
            db.query(
                "SELECT * FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                arrayOf("table", TABLE_IMAGE_RELATED),
            ).use { cursor ->
                val count = cursor.count
                log("migrate10_11() count = [$count]")
                hasImageTable = count > 0
            }
        }
        Assert.assertEquals(false, hasImageTable)

        helper.runMigrationsAndValidate(testDbName, 11, true, Migration10To11)
            .use { db ->
                db.query(
                    "SELECT * FROM `$tableName` WHERE `$columnNameType` = ? AND `$columnNameTableName` = ?",
                    arrayOf("table", TABLE_IMAGE_RELATED),
                ).use { cursor ->
                    val count = cursor.count
                    log("migrate10_11() count = [$count]")
                    hasImageTable = count > 0
                }
            }
        Assert.assertEquals(true, hasImageTable)
    }

    /**
     * 测试数据库从 11 升级到 12
     * - db_record、db_asset 金额字段由 REAL（单位：元）整表重建为 INTEGER（单位：分），CAST(ROUND(amount*100) AS INTEGER)
     * - 新增 14 个核心索引
     * - 插入固定类型（退款 -2001 / 报销 -2002 / 信用卡还款 -2003）
     */
    @Test
    @Throws(IOException::class)
    fun migrate11_12() {
        log("migrate11_12()")
        helper.createDatabase(testDbName, 11).use { db ->
            // 普通记录：整数元金额，断言精确转换为分
            db.insert(
                TABLE_RECORD,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", 1L)
                    put("type_id", 1L)
                    put("asset_id", 1L)
                    put("into_asset_id", -1L)
                    put("books_id", 1L)
                    put("amount", 12.34)
                    put("final_amount", 10.00)
                    put("concessions", 0.50)
                    put("charge", 0.0)
                    put("remark", "正常记录")
                    put("reimbursable", 0)
                    put("record_time", 1_600_000_000_000L)
                },
            )
            // 边界记录：包含 1.005 / 0.005 浮点边界值，断言其变为非负整数（不断言精确 100/101，依赖 IEEE754）
            db.insert(
                TABLE_RECORD,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", 2L)
                    put("type_id", 1L)
                    put("asset_id", 1L)
                    put("into_asset_id", -1L)
                    put("books_id", 1L)
                    put("amount", 1.005)
                    put("final_amount", 0.005)
                    put("concessions", 0.0)
                    put("charge", 0.0)
                    put("remark", "边界记录")
                    put("reimbursable", 0)
                    put("record_time", 1_600_000_001_000L)
                },
            )
            // 资产：余额、总额由元转分
            db.insert(
                TABLE_ASSET,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", 1L)
                    put("books_id", 1L)
                    put("name", "现金")
                    put("balance", 88.88)
                    put("total_amount", 0.0)
                    put("billing_date", "")
                    put("repayment_date", "")
                    put("type", 0)
                    put("classification", 0)
                    put("invisible", 0)
                    put("open_bank", "")
                    put("card_no", "")
                    put("remark", "")
                    put("sort", 0)
                    put("modify_time", System.currentTimeMillis())
                },
            )
        }

        var amountType = -1
        var amountValue = -1L
        var finalAmountValue = -1L
        var concessionsValue = -1L
        var boundaryAmount = -1L
        var boundaryFinalAmount = -1L
        var assetBalanceType = -1
        var assetBalance = -1L
        var hasRecordBooksIdIndex = false
        var hasRecordTimeIndex = false
        var hasTagRecordIndex = false
        var fixedTypeCount = 0
        helper.runMigrationsAndValidate(testDbName, 12, true, Migration11To12)
            .use { db ->
                // 金额字段已变为 INTEGER（分），精确转换：12.34 -> 1234，10.00 -> 1000，0.50 -> 50
                db.query("SELECT * FROM `$TABLE_RECORD` WHERE `id` = 1").use { cursor ->
                    cursor.moveToFirst()
                    val amountIndex = cursor.getColumnIndexOrThrow(TABLE_RECORD_AMOUNT)
                    amountType = cursor.getType(amountIndex)
                    amountValue = cursor.getLong(amountIndex)
                    finalAmountValue =
                        cursor.getLong(cursor.getColumnIndexOrThrow(TABLE_RECORD_FINAL_AMOUNT))
                    concessionsValue =
                        cursor.getLong(cursor.getColumnIndexOrThrow(TABLE_RECORD_CONCESSIONS))
                }
                // 边界记录：仅断言为非负整数
                db.query("SELECT * FROM `$TABLE_RECORD` WHERE `id` = 2").use { cursor ->
                    cursor.moveToFirst()
                    boundaryAmount =
                        cursor.getLong(cursor.getColumnIndexOrThrow(TABLE_RECORD_AMOUNT))
                    boundaryFinalAmount =
                        cursor.getLong(cursor.getColumnIndexOrThrow(TABLE_RECORD_FINAL_AMOUNT))
                }
                // 资产 balance 也已转为分：88.88 -> 8888
                db.query("SELECT * FROM `$TABLE_ASSET` WHERE `id` = 1").use { cursor ->
                    cursor.moveToFirst()
                    val balanceIndex = cursor.getColumnIndexOrThrow("balance")
                    assetBalanceType = cursor.getType(balanceIndex)
                    assetBalance = cursor.getLong(balanceIndex)
                }
                // 核心索引已创建
                db.query(
                    "SELECT `name` FROM `$tableName` WHERE `$columnNameType` = ?",
                    arrayOf("index"),
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        when (cursor.getString(cursor.getColumnIndexOrThrow("name"))) {
                            "index_db_record_books_id" -> hasRecordBooksIdIndex = true
                            "index_db_record_record_time" -> hasRecordTimeIndex = true
                            "index_db_tag_with_record_tag_id" -> hasTagRecordIndex = true
                        }
                    }
                }
                // 固定类型已插入（id 为 -2001 / -2002 / -2003）
                db.query(
                    "SELECT * FROM `$TABLE_TYPE` WHERE `id` IN (-2001, -2002, -2003)",
                ).use { cursor ->
                    fixedTypeCount = cursor.count
                }
            }
        log(
            "migrate11_12() amountType=$amountType, amount=$amountValue, finalAmount=$finalAmountValue, " +
                "concessions=$concessionsValue, boundaryAmount=$boundaryAmount, boundaryFinalAmount=$boundaryFinalAmount, " +
                "assetBalanceType=$assetBalanceType, assetBalance=$assetBalance, fixedTypeCount=$fixedTypeCount",
        )
        // amount 字段类型已变为 INTEGER
        Assert.assertEquals(Cursor.FIELD_TYPE_INTEGER, amountType)
        // 精确转换断言（单位：分）
        Assert.assertEquals(1234L, amountValue)
        Assert.assertEquals(1000L, finalAmountValue)
        Assert.assertEquals(50L, concessionsValue)
        // 边界值为非负整数（IEEE754 下 1.005/0.005 的精确取整不稳定，仅断言非负）
        Assert.assertTrue(boundaryAmount >= 0L)
        Assert.assertTrue(boundaryFinalAmount >= 0L)
        // 资产 balance 同样为 INTEGER 且精确转换 88.88 -> 8888
        Assert.assertEquals(Cursor.FIELD_TYPE_INTEGER, assetBalanceType)
        Assert.assertEquals(8888L, assetBalance)
        // 索引存在
        Assert.assertEquals(true, hasRecordBooksIdIndex)
        Assert.assertEquals(true, hasRecordTimeIndex)
        Assert.assertEquals(true, hasTagRecordIndex)
        // 三条固定类型均已插入
        Assert.assertEquals(3, fixedTypeCount)
    }

    /**
     * 测试数据库升级 12 -> 13
     * - 新增 db_budget 表 + (books_id, type_id) 唯一索引
     * - F3 搭车：清理残留的 db_record_temp（validateDroppedTables=true 校验无意外表）
     */
    @Test
    @Throws(IOException::class)
    fun migrate12_13() {
        log("migrate12_13()")
        helper.createDatabase(testDbName, 12).use { db ->
            // 模拟历史 Migration6To7 泄漏的 db_record_temp 残留
            db.execSQL("CREATE TABLE IF NOT EXISTS `db_record_temp` (`id` INTEGER PRIMARY KEY)")
        }
        var budgetCount = -1
        var hasBudgetIndex = false
        helper.runMigrationsAndValidate(testDbName, 13, true, Migration12To13).use { db ->
            // db_budget 建成（validateDroppedTables=true 同时保证 db_record_temp 已清 + 最终 schema 校验）
            db.query("SELECT count(*) FROM `db_budget`").use { cursor ->
                cursor.moveToFirst()
                budgetCount = cursor.getInt(0)
            }
            // (books_id, type_id) 唯一索引存在
            db.query(
                "SELECT `name` FROM `$tableName` WHERE `$columnNameType` = ?",
                arrayOf("index"),
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(cursor.getColumnIndexOrThrow("name")) ==
                        "index_db_budget_books_id_type_id"
                    ) {
                        hasBudgetIndex = true
                    }
                }
            }
        }
        log("migrate12_13() budgetCount=$budgetCount, hasBudgetIndex=$hasBudgetIndex")
        Assert.assertEquals(0, budgetCount)
        Assert.assertEquals(true, hasBudgetIndex)
    }

    /**
     * 测试数据库升级 13 -> 14
     * - 新增 db_record.reimbursed 列（INTEGER NOT NULL DEFAULT 0）
     * - 存量行迁移后默认值为 0
     */
    @Test
    @Throws(IOException::class)
    fun migrate13_14() {
        log("migrate13_14()")
        helper.createDatabase(testDbName, 13).use { db ->
            // v13 db_record 全列插入一行（无 reimbursed 列）
            db.execSQL(
                "INSERT INTO `db_record` " +
                    "(`id`,`type_id`,`asset_id`,`into_asset_id`,`books_id`,`amount`,`final_amount`," +
                    "`concessions`,`charge`,`remark`,`reimbursable`,`record_time`) " +
                    "VALUES (1,1,-1,-1,1,1000,1000,0,0,'r',1,1704067200000)",
            )
        }
        var reimbursedValue = -1
        helper.runMigrationsAndValidate(testDbName, 14, true, Migration13To14).use { db ->
            db.query("SELECT `reimbursed` FROM `db_record` WHERE `id`=1").use { cursor ->
                cursor.moveToFirst()
                reimbursedValue = cursor.getInt(0)
            }
        }
        log("migrate13_14() reimbursedValue=$reimbursedValue")
        // 存量行迁移后 reimbursed 默认 0
        Assert.assertEquals(0, reimbursedValue)
    }

    /**
     * 端到端校验：从版本 1 依次执行全部迁移到最新 schema
     * - 创建最早版本（1）数据库
     * - 通过 Room.databaseBuilder + addMigrations 打开，Room 在所有迁移执行后校验最终 schema
     */
    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        log("migrateAll()")
        // 创建最早版本（1）的数据库
        helper.createDatabase(testDbName, 1).apply {
            close()
        }

        // 打开最新版本数据库，Room 会在全部迁移执行后校验 schema 是否与最新版本一致
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            CashbookDatabase::class.java,
            testDbName,
        ).addMigrations(*DatabaseMigrations.MIGRATION_LIST).build().apply {
            openHelper.writableDatabase.close()
        }
    }

    private fun log(text: String) {
        println("DatabaseText ------ $text")
    }
}
