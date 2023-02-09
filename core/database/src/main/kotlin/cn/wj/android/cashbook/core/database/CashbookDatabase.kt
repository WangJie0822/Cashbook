package cn.wj.android.cashbook.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.BooksTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.database.table.TypeTable

/**
 * 记账本数据库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/15
 */
@Database(
    entities = [BooksTable::class, AssetTable::class, TypeTable::class, RecordTable::class, TagTable::class],
    version = 6
)
abstract class CashbookDatabase : RoomDatabase() {


}