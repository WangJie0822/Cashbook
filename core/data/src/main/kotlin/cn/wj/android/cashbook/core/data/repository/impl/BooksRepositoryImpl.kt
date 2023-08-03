package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.model.bookDataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.database.dao.BooksDao
import cn.wj.android.cashbook.core.database.dao.TransactionDao
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.model.model.BooksModel
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

class BooksRepositoryImpl @Inject constructor(
    private val booksDao: BooksDao,
    private val transactionDao: TransactionDao,
    private val appPreferencesDataSource: AppPreferencesDataSource,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : BooksRepository {

    override val booksListData: Flow<List<BooksModel>> =
        bookDataVersion
            .mapLatest { getAllBooksList() }

    override val currentBook: Flow<BooksModel> =
        combine(booksListData, appPreferencesDataSource.appData) { list, appData ->
            var selected = list.firstOrNull { it.id == appData.currentBookId }
            if (null == selected) {
                // 没有找到当前账本，默认选择第一个
                selected = list.first()
                appPreferencesDataSource.updateCurrentBookId(selected.id)
            }
            selected
        }

    override suspend fun selectBook(id: Long): Unit = withContext(coroutineContext) {
        appPreferencesDataSource.updateCurrentBookId(id)
    }

    override suspend fun deleteBook(id: Long): Boolean = withContext(coroutineContext) {
        val result = runCatching {
            transactionDao.deleteBookTransaction(id)
            true
        }.getOrElse { throwable ->
            this@BooksRepositoryImpl.logger().e(throwable, "deleteBook()")
            false
        }
        if (result) {
            // 删除成功
            bookDataVersion.updateVersion()
        }
        result
    }

    private suspend fun getAllBooksList(): List<BooksModel> = withContext(coroutineContext) {
        booksDao.queryAll().map { it.asModel() }
    }
}
