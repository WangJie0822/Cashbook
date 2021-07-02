package cn.wj.android.cashbook.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cn.wj.android.cashbook.data.database.dao.AssetDao
import cn.wj.android.cashbook.data.database.dao.BooksDao
import cn.wj.android.cashbook.data.database.dao.RecordDao
import cn.wj.android.cashbook.data.database.dao.TagDao
import cn.wj.android.cashbook.data.database.dao.TypeDao
import cn.wj.android.cashbook.data.database.table.AssetTable
import cn.wj.android.cashbook.data.database.table.BooksTable
import cn.wj.android.cashbook.data.database.table.RecordTable
import cn.wj.android.cashbook.data.database.table.TagTable
import cn.wj.android.cashbook.data.database.table.TypeTable

/**
 * 记账本数据库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/15
 */
@Database(entities = [BooksTable::class, AssetTable::class, TypeTable::class, RecordTable::class, TagTable::class], version = 3)
abstract class CashbookDatabase : RoomDatabase() {

    /** 获取账本相关数据库操作接口 */
    abstract fun booksDao(): BooksDao

    /** 获取资产相关数据库操作接口 */
    abstract fun assetDao(): AssetDao

    /** 获取类型数据库操作接口 */
    abstract fun typeDao(): TypeDao

    /** 获取记录数据库操作接口 */
    abstract fun recordDao(): RecordDao

    /** 获取标签数据库操作接口 */
    abstract fun tagDao(): TagDao
}