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
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.third.MyFormatStrategy
import cn.wj.android.cashbook.core.database.migration.DatabaseMigrations
import cn.wj.android.cashbook.core.database.migration.Migration1To2
import cn.wj.android.cashbook.core.database.migration.Migration2To3
import cn.wj.android.cashbook.core.database.migration.Migration3To4
import cn.wj.android.cashbook.core.database.migration.Migration4To5
import cn.wj.android.cashbook.core.database.migration.Migration5To6
import cn.wj.android.cashbook.core.database.migration.Migration7To8
import cn.wj.android.cashbook.core.database.migration.Migration8To9
import cn.wj.android.cashbook.core.database.migration.SQL_QUERY_ALL_FROM_BOOKS
import cn.wj.android.cashbook.core.database.migration.SQL_QUERY_ALL_FROM_RECORD
import cn.wj.android.cashbook.core.database.migration.SQL_QUERY_ALL_FROM_TAG
import cn.wj.android.cashbook.core.database.table.TABLE_BOOKS
import cn.wj.android.cashbook.core.database.table.TABLE_BOOKS_BG_URI
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
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_REMARK
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_TYPE_ID
import cn.wj.android.cashbook.core.database.table.TABLE_TAG
import cn.wj.android.cashbook.core.database.table.TABLE_TAG_INVISIBLE
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
     * 测试数据库从 8 升级到 0
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

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
//        // Create earliest version of the database.
//        helper.createDatabase(testDbName, 1).apply {
//            close()
//        }
//
//        // Open latest version of the database. Room will validate the schema
//        // once all migrations execute.
//        Room.databaseBuilder(
//            InstrumentationRegistry.getInstrumentation().targetContext,
//            CashbookDatabase::class.java,
//            testDbName
//        ).addMigrations(*DatabaseMigrations.MIGRATION_LIST).build().apply {
//            openHelper.writableDatabase.close()
//        }
    }

    private fun log(text: String) {
        println("DatabaseText ------ $text")
    }
}
