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

package cn.wj.android.cashbook.core.data.repository.fake

import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.model.model.BooksModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object FakeBooksRepository : BooksRepository {

    private var coroutineContext = Dispatchers.IO

    private val _selectedBookId = MutableStateFlow(1L)

    private val _booksList = MutableStateFlow(
        listOf(
            BooksModel(
                1L,
                "默认账本1",
                "默认账本1说明",
                "",
                System.currentTimeMillis(),
            ),
        ),
    )

    override val booksListData: Flow<List<BooksModel>>
        get() = _booksList

    override val currentBook: Flow<BooksModel>
        get() = combine(_selectedBookId, _booksList) { selectedId, list ->
            list.first { it.id == selectedId }
        }

    override suspend fun selectBook(id: Long): Unit = withContext(coroutineContext) {
        _selectedBookId.tryEmit(id)
    }

    override suspend fun insertBook(book: BooksModel): Long = withContext(coroutineContext) {
        val id = System.currentTimeMillis()
        val currentList = ArrayList(_booksList.first())
        currentList.add(book.copy(id = id))
        _booksList.tryEmit(currentList.filter { it.id != id })
        id
    }

    override suspend fun deleteBook(id: Long): Boolean = withContext(coroutineContext) {
        val currentList = _booksList.first()
        _booksList.tryEmit(currentList.filter { it.id != id })
        true
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
        val currentList = booksListData.first()
        if (currentList.count { it.id == book.id } > 0) {
            _booksList.tryEmit(
                currentList.map {
                    if (it.id == book.id) {
                        book
                    } else {
                        it
                    }
                },
            )
        } else {
            val newBook = book.copy(id = (currentList.lastOrNull()?.id ?: 0L) + 1L)
            val newList = ArrayList(currentList)
            newList.add(newBook)
            _booksList.tryEmit(newList)
        }
    }
}
