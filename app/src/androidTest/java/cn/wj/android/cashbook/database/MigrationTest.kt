package cn.wj.android.cashbook.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.wj.android.cashbook.TestBase
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.data.database.CashbookDatabase
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 数据库迁移测试
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/8/10
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest : TestBase() {

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
        arrayListOf(),
        FrameworkSQLiteOpenHelperFactory()
    )

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
        assertEquals(false, hasTagTable)
        helper.runMigrationsAndValidate(testDbName, 2, true, CashbookDatabase.MIGRATION_1_2)
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
        assertEquals(true, hasTagTable)
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
        assertEquals(false, hasTypeEnum)
        assertEquals(false, hasTypeId)
        assertEquals(true, hasType)
        assertEquals(true, hasFirstTypeId)
        assertEquals(true, hasSecondTypeId)
        assertEquals(true, hasSystem)

        val typeEnum: String
        val typeId: Long
        helper.runMigrationsAndValidate(testDbName, 3, true, CashbookDatabase.MIGRATION_2_3)
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
        assertEquals(true, hasTypeEnum)
        assertEquals(true, hasTypeId)
        assertEquals(false, hasType)
        assertEquals(false, hasFirstTypeId)
        assertEquals(false, hasSecondTypeId)
        assertEquals(false, hasSystem)
        assertEquals(type, typeEnum)
        assertEquals(firstTypeId, typeId)
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
        assertEquals(false, hasBooksId)
        assertEquals(false, hasShared)

        helper.runMigrationsAndValidate(testDbName, 4, true, CashbookDatabase.MIGRATION_3_4)
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
        assertEquals(true, hasBooksId)
        assertEquals(true, hasShared)
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
        assertEquals(false, hasOpenBank)
        assertEquals(false, hasCardNo)
        assertEquals(false, hasRemark)

        helper.runMigrationsAndValidate(testDbName, 5, true, CashbookDatabase.MIGRATION_4_5)
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
        assertEquals(true, hasOpenBank)
        assertEquals(true, hasCardNo)
        assertEquals(true, hasRemark)
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
        helper.runMigrationsAndValidate(testDbName, 6, true, CashbookDatabase.MIGRATION_5_6)
            .use { db ->
                db.query("SELECT * FROM `db_asset`").use { cursor ->
                    cursor.moveToFirst()
                    doubleStr = cursor.getString(cursor.getColumnIndexOrThrow("total_amount"))
                }
            }
        logger().d("$floatStr - $doubleStr")
        assertEquals(floatStr, doubleStr)
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Create earliest version of the database.
        helper.createDatabase(testDbName, 1).apply {
            close()
        }

        // Open latest version of the database. Room will validate the schema
        // once all migrations execute.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            CashbookDatabase::class.java,
            testDbName
        ).addMigrations(*CashbookDatabase.MIGRATION_LIST).build().apply {
            openHelper.writableDatabase.close()
        }
    }
}