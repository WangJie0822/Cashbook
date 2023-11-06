package cn.wj.android.cashbook.core.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.database.migration.DatabaseMigrations
import cn.wj.android.cashbook.core.database.migration.Migration1To2
import cn.wj.android.cashbook.core.database.migration.Migration2To3
import cn.wj.android.cashbook.core.database.migration.Migration3To4
import cn.wj.android.cashbook.core.database.migration.Migration4To5
import cn.wj.android.cashbook.core.database.migration.Migration5To6
import cn.wj.android.cashbook.core.database.migration.SQL_QUERY_ALL_FROM_RECORD
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
import java.io.IOException
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun recovery_from_database() {
        var resultCount = 0
        helper.createDatabase("to", 7).use { to ->
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
                }
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
            helper.createDatabase("from", 7).use { from ->
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
                    }
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
                    }
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
                arrayOf("table", "db_tag")
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
                    arrayOf("table", "db_tag")
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
                arrayOf("table", "db_record")
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
                arrayOf("table", "db_type")
            ).use { cursor ->
                cursor.moveToFirst()
                val sqlStr = cursor.getString(cursor.getColumnIndex(columnNameSql))
                log("migrate2_3() sqlStr = [$sqlStr]")
                hasSystem = sqlStr.contains("`system`")
            }
            // 插入数据
            db.insert("db_record", SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
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
            })
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
                    arrayOf("table", "db_record")
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
                    arrayOf("table", "db_type")
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
                arrayOf("table", "db_tag")
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
                    arrayOf("table", "db_tag")
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
                arrayOf("table", "db_asset")
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
                    arrayOf("table", "db_asset")
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
     * -
     */
    @Test
    @Throws(IOException::class)
    fun migrate5_6() {
        var floatStr: String
        helper.createDatabase(testDbName, 5).use { db ->
            db.insert("db_asset", SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
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
            })
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