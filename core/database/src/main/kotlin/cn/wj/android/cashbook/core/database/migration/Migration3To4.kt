package cn.wj.android.cashbook.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.table.TABLE_TAG
import org.intellij.lang.annotations.Language

/**
 * 数据库升级 3 -> 4
 * - db_tag 表新增 books_id、shared
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/1
 */
object Migration3To4 : Migration(3, 4) {

    /** 增加 books_id 字段 */
    @Language("SQL")
    private const val SQL_ALTER_TABLE_TAG_ADD_BOOKS_ID = """
        ALTER TABLE `${TABLE_TAG}` ADD `books_id` INTEGER DEFAULT -1 NOT NULL
    """

    /** 增加 books_id 字段 */
    @Language("SQL")
    private const val SQL_ALTER_TABLE_TAG_ADD_SHARED = """
        ALTER TABLE `${TABLE_TAG}` ADD `shared` INTEGER DEFAULT $SWITCH_INT_ON NOT NULL
    """

    override fun migrate(database: SupportSQLiteDatabase) = with(database) {
        execSQL(SQL_ALTER_TABLE_TAG_ADD_BOOKS_ID)
        execSQL(SQL_ALTER_TABLE_TAG_ADD_SHARED)
    }
}