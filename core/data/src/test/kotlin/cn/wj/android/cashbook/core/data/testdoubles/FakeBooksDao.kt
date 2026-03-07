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

package cn.wj.android.cashbook.core.data.testdoubles

import cn.wj.android.cashbook.core.database.dao.BooksDao
import cn.wj.android.cashbook.core.database.table.BooksTable

/**
 * BooksDao 的测试替身，使用内存列表存储数据
 */
class FakeBooksDao : BooksDao {

    /** 账本数据列表 */
    val books = mutableListOf<BooksTable>()

    /** 自增主键计数器 */
    private var nextId = 1L

    override suspend fun queryAll(): List<BooksTable> {
        return books.toList()
    }

    override suspend fun insert(book: BooksTable): Long {
        val id = book.id ?: nextId++
        val withId = book.copy(id = id)
        books.add(withId)
        return id
    }

    override suspend fun insertOrReplace(book: BooksTable) {
        val index = books.indexOfFirst { it.id == book.id }
        if (index >= 0) {
            books[index] = book
        } else {
            val id = if (book.id == null) nextId++ else book.id
            books.add(book.copy(id = id))
        }
    }
}
