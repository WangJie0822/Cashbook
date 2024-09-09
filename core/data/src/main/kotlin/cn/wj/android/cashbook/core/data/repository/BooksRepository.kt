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

package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.database.table.BooksTable
import cn.wj.android.cashbook.core.model.model.BooksModel
import kotlinx.coroutines.flow.Flow

interface BooksRepository {

    val booksListData: Flow<List<BooksModel>>

    val currentBook: Flow<BooksModel>

    suspend fun selectBook(id: Long)

    suspend fun insertBook(book: BooksModel): Long

    suspend fun deleteBook(id: Long): Boolean

    suspend fun getDefaultBook(id: Long): BooksModel

    suspend fun isDuplicated(book: BooksModel): Boolean

    suspend fun updateBook(book: BooksModel)
}

internal fun BooksTable.asModel(): BooksModel {
    return BooksModel(
        id = this.id ?: -1L,
        name = this.name,
        description = this.description,
        bgUri = this.bgUri,
        modifyTime = this.modifyTime,
    )
}

internal fun BooksModel.asTable(): BooksTable {
    return BooksTable(
        id = if (this.id == -1L) null else this.id,
        name = this.name,
        description = this.description,
        bgUri = this.bgUri,
        modifyTime = this.modifyTime,
    )
}
