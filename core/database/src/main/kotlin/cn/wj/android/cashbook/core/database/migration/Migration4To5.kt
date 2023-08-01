package cn.wj.android.cashbook.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.wj.android.cashbook.core.database.table.TABLE_ASSET
import org.intellij.lang.annotations.Language

/**
 * 数据库升级 4 -> 5
 * - db_asset 表新增 open_bank、card_no、remark 字段
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/1
 */
object Migration4To5 : Migration(4, 5) {

    /** 增加 open_bank 字段 */
    @Language("SQL")
    private const val SQL_ALTER_TABLE_ASSET_ADD_OPEN_BACK = """
        ALTER TABLE `${TABLE_ASSET}` ADD `open_bank` TEXT DEFAULT '' NOT NULL
    """

    /** 增加 card_no 字段 */
    @Language("SQL")
    private const val SQL_ALTER_TABLE_ASSET_ADD_CARD_NO = """
        ALTER TABLE `${TABLE_ASSET}` ADD `card_no` TEXT DEFAULT '' NOT NULL
    """

    /** 增加 remark 字段 */
    @Language("SQL")
    private const val SQL_ALTER_TABLE_ASSET_ADD_REMARK = """
        ALTER TABLE `${TABLE_ASSET}` ADD `remark` TEXT DEFAULT '' NOT NULL
    """

    override fun migrate(database: SupportSQLiteDatabase) = with(database) {
        execSQL(SQL_ALTER_TABLE_ASSET_ADD_OPEN_BACK)
        execSQL(SQL_ALTER_TABLE_ASSET_ADD_CARD_NO)
        execSQL(SQL_ALTER_TABLE_ASSET_ADD_REMARK)
    }
}