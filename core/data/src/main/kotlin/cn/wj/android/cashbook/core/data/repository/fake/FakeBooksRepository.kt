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
                System.currentTimeMillis()
            )
        )
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
            modifyTime = System.currentTimeMillis()
        )
    }

    override suspend fun isDuplicated(book: BooksModel): Boolean = withContext(coroutineContext) {
        booksListData.first().count { it.id != book.id && it.name == book.name } > 0
    }

    override suspend fun updateBook(book: BooksModel): Unit = withContext(coroutineContext) {
        val currentList = booksListData.first()
        if (currentList.count { it.id == book.id } > 0) {
            _booksList.tryEmit(currentList.map {
                if (it.id == book.id) {
                    book
                } else {
                    it
                }
            })
        } else {
            val newBook = book.copy(id = (currentList.lastOrNull()?.id ?: 0L) + 1L)
            val newList = ArrayList(currentList)
            newList.add(newBook)
            _booksList.tryEmit(newList)
        }
    }

    fun initData() {
        _booksList.tryEmit(
            listOf(
                BooksModel(
                    1L,
                    "默认账本1",
                    "默认账本1说明",
                    System.currentTimeMillis()
                ),
                BooksModel(
                    2L,
                    "默认账本2",
                    "默认账本2说明默认账本2说明默认账本2说明默认账本2说明默认账本2说明",
                    System.currentTimeMillis()
                )
            )
        )
    }
}