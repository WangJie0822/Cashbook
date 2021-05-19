package cn.wj.android.cashbook.data.store

import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.database.dao.BooksDao
import cn.wj.android.cashbook.data.database.table.BooksTable
import cn.wj.android.cashbook.data.database.table.toBooksEntity
import cn.wj.android.cashbook.data.database.table.toBooksTable
import cn.wj.android.cashbook.data.entity.BooksEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 本地数据存储
 *
 * @param database 数据库对象
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/19
 */
class LocalDataStore(private val database: CashbookDatabase) {

    /** 账本数据库操作对象 */
    private val booksDao: BooksDao by lazy {
        database.booksDao()
    }

    /** 将账本数据 [books] 插入到数据库中 */
    suspend fun insertBooks(vararg books: BooksEntity) = withContext(Dispatchers.IO) {
        val ls = arrayListOf<BooksTable>()
        books.forEach {
            ls.add(it.toBooksTable())
        }
        booksDao.insert(*ls.toTypedArray())
    }

    /** 获取所有账本数据并返回 */
    suspend fun getBooksList(): List<BooksEntity> = withContext(Dispatchers.IO) {
        booksDao.queryAll().map {
            it.toBooksEntity()
        }
    }

    /** 获取并返回默认选中的账本，没有返回 `null` */
    suspend fun getDefaultBooks(): BooksEntity? = withContext(Dispatchers.IO) {
        booksDao.queryDefault().firstOrNull()?.toBooksEntity()
    }
}