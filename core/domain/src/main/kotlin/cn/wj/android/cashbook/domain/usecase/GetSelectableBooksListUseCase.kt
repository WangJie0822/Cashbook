package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.model.model.BooksModel
import cn.wj.android.cashbook.core.model.model.Selectable
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetSelectableBooksListUseCase @Inject constructor(
    private val booksRepository: BooksRepository,
) {

    operator fun invoke(): Flow<List<Selectable<BooksModel>>> {
        return combine(
            booksRepository.booksListData,
            booksRepository.currentBook
        ) { list, book ->
            list.map {
                Selectable(it, it.id == book.id)
            }
        }
    }
}