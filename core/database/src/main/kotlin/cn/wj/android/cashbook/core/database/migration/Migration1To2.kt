package cn.wj.android.cashbook.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.wj.android.cashbook.core.database.table.TABLE_TAG
import org.intellij.lang.annotations.Language

/**
 * 数据库升级 1 -> 2
 * - 新增 db_tag 表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/1
 */
object Migration1To2 : Migration(1, 2) {

    /** 创建标签表，版本 2 */
    @Language("SQL")
    private const val SQL_CREATE_TABLE_TAG_2 = """
        CREATE TABLE IF NOT EXISTS `${TABLE_TAG}` 
        (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT, 
            `name` TEXT NOT NULL
        )
    """

    override fun migrate(db: SupportSQLiteDatabase) = with(db) {
        // 创建标签表
        execSQL(SQL_CREATE_TABLE_TAG_2)
    }

}