package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.database.table.BooksTable
import cn.wj.android.cashbook.core.model.model.BooksModel
import kotlinx.coroutines.flow.Flow

interface BooksRepository {

    val booksListData: Flow<List<BooksModel>>

    val currentBook: Flow<BooksModel>

    suspend fun selectBook(id: Long)

    suspend fun deleteBook(id: Long): Boolean
}

internal fun BooksTable.asModel(): BooksModel {
    return BooksModel(
        id = this.id ?: -1L,
        name = this.name,
        description = this.description,
        modifyTime = this.modifyTime,
    )
}
