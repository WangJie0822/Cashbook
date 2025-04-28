/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.model.bookDataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.database.dao.BooksDao
import cn.wj.android.cashbook.core.database.dao.TransactionDao
import cn.wj.android.cashbook.core.datastore.datasource.CombineProtoDataSource
import cn.wj.android.cashbook.core.model.model.BooksModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class BooksRepositoryImpl @Inject constructor(
    private val booksDao: BooksDao,
    private val transactionDao: TransactionDao,
    private val combineProtoDataSource: CombineProtoDataSource,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : BooksRepository {

    override val booksListData: Flow<List<BooksModel>> =
        bookDataVersion
            .mapLatest { getAllBooksList() }

    override val currentBook: Flow<BooksModel> =
        combine(booksListData, combineProtoDataSource.recordSettingsData) { list, data ->
            var selected = list.firstOrNull { it.id == data.currentBookId }
            if (null == selected) {
                // 没有找到当前账本，默认选择第一个
                selected = list.first()
                combineProtoDataSource.updateCurrentBookId(selected.id)
            }
            selected
        }

    override suspend fun selectBook(id: Long): Unit = withContext(coroutineContext) {
        combineProtoDataSource.updateCurrentBookId(id)
    }

    override suspend fun insertBook(book: BooksModel): Long = withContext(coroutineContext) {
        booksDao.insert(book.asTable())
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

    override suspend fun getDefaultBook(id: Long): BooksModel = withContext(coroutineContext) {
        booksListData.first().firstOrNull { it.id == id } ?: BooksModel(
            id = id,
            name = "",
            description = "",
            bgUri = "",
            modifyTime = System.currentTimeMillis(),
        )
    }

    override suspend fun isDuplicated(book: BooksModel): Boolean = withContext(coroutineContext) {
        booksListData.first().count { it.id != book.id && it.name == book.name } > 0
    }

    override suspend fun updateBook(book: BooksModel): Unit = withContext(coroutineContext) {
        booksDao.insertOrReplace(book.asTable())
        bookDataVersion.updateVersion()
    }

    private suspend fun getAllBooksList(): List<BooksModel> = withContext(coroutineContext) {
        booksDao.queryAll().map { it.asModel() }
    }
}
