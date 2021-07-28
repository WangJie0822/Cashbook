package cn.wj.android.cashbook.data.repository.books

import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.database.table.BooksTable
import cn.wj.android.cashbook.data.entity.BooksEntity
import cn.wj.android.cashbook.data.repository.Repository
import cn.wj.android.cashbook.data.transform.toAssetTable
import cn.wj.android.cashbook.data.transform.toBooksEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 账本相关数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/28
 */
class BooksRepository(database: CashbookDatabase) : Repository(database) {

    /** 是否存在名称为 [name] 的账本 */
    suspend fun hasBooksCountByName(name: String): Boolean = withContext(Dispatchers.IO) {
        booksDao.getCountByName(name) > 0
    }

    /** 获取所有账本数据并返回 */
    suspend fun getBooksList(): List<BooksEntity> = withContext(Dispatchers.IO) {
        booksDao.queryAll().map {
            it.toBooksEntity()
        }
    }

    /** 将账本数据 [books] 插入到数据库中 */
    suspend fun insertBooks(books: BooksEntity) = withContext(Dispatchers.IO) {
        booksDao.insert(books.toAssetTable())
    }

    /** 从数据库中删除 [books] */
    suspend fun deleteBooks(books: BooksEntity) = withContext(Dispatchers.IO) {
        booksDao.delete(books.toAssetTable())
    }

    /** 更新账单数据 [books] */
    suspend fun updateBooks(vararg books: BooksEntity) = withContext(Dispatchers.IO) {
        val ls = arrayListOf<BooksTable>()
        books.forEach {
            ls.add(it.toAssetTable())
        }
        booksDao.update(*ls.toTypedArray())
    }
}