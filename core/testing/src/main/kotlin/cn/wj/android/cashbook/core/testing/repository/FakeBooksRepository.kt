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

package cn.wj.android.cashbook.core.testing.repository

import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.model.model.BooksModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBooksRepository : BooksRepository {

    private val books = mutableListOf<BooksModel>()
    private val _booksListData = MutableStateFlow<List<BooksModel>>(emptyList())
    private val _currentBook = MutableStateFlow(
        BooksModel(
            id = 1L,
            name = "默认账本",
            description = "",
            bgUri = "",
            modifyTime = 0L,
        ),
    )
    private var nextId = 100L

    override val booksListData: Flow<List<BooksModel>> = _booksListData
    override val currentBook: Flow<BooksModel> = _currentBook

    fun addBook(book: BooksModel) {
        books.add(book)
        _booksListData.value = books.toList()
    }

    fun setCurrentBook(book: BooksModel) {
        _currentBook.value = book
    }

    override suspend fun selectBook(id: Long) {
        val book = books.find { it.id == id }
        if (book != null) {
            _currentBook.value = book
        }
    }

    override suspend fun insertBook(book: BooksModel): Long {
        val id = nextId++
        val newBook = book.copy(id = id)
        books.add(newBook)
        _booksListData.value = books.toList()
        return id
    }

    override suspend fun deleteBook(id: Long): Boolean {
        val removed = books.removeAll { it.id == id }
        _booksListData.value = books.toList()
        return removed
    }

    override suspend fun getDefaultBook(id: Long): BooksModel {
        return books.find { it.id == id } ?: _currentBook.value
    }

    override suspend fun isDuplicated(book: BooksModel): Boolean {
        return books.any { it.name == book.name && it.id != book.id }
    }

    override suspend fun updateBook(book: BooksModel) {
        val index = books.indexOfFirst { it.id == book.id }
        if (index >= 0) {
            books[index] = book
        }
        _booksListData.value = books.toList()
    }
}
