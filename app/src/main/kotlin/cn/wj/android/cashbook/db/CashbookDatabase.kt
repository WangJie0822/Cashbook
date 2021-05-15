package cn.wj.android.cashbook.db

import androidx.room.Database
import androidx.room.RoomDatabase
import cn.wj.android.cashbook.db.dao.BooksDao
import cn.wj.android.cashbook.db.table.BooksTable

/**
 * 记账本数据库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/15
 */
@Database(entities = [BooksTable::class], version = 1)
abstract class CashbookDatabase : RoomDatabase() {

    /** 获取账本相关数据库操作接口 */
    abstract fun booksDao(): BooksDao
}