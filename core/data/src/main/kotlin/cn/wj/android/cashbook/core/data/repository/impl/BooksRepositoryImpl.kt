package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.model.bookDataVersion
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.database.dao.BooksDao
import cn.wj.android.cashbook.core.model.model.BooksModel
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class BooksRepositoryImpl @Inject constructor(
    private val booksDao: BooksDao,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : BooksRepository {

    override val booksListData: Flow<List<BooksModel>> =
        bookDataVersion
            .mapLatest { getAllBooksList() }

    private suspend fun getAllBooksList(): List<BooksModel> = withContext(coroutineContext) {
        booksDao.queryAll().map { it.asModel() }
    }
}
