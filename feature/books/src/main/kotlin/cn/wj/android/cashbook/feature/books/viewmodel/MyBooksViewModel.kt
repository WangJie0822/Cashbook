package cn.wj.android.cashbook.feature.books.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.model.model.BooksModel
import cn.wj.android.cashbook.core.model.model.Selectable
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.domain.usecase.GetSelectableBooksListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 我的账本 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/3
 */
@HiltViewModel
class MyBooksViewModel @Inject constructor(
    getSelectableBooksListUseCase: GetSelectableBooksListUseCase,
    private val booksRepository: BooksRepository,
) : ViewModel() {

    var dialogState: DialogState by mutableStateOf(DialogState.Dismiss)
        private set

    val uiState = getSelectableBooksListUseCase()
        .mapLatest { list ->
            MyBooksUiState.Success(list)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = MyBooksUiState.Loading,
        )

    fun onBookSelected(id: Long) {
        viewModelScope.launch {
            booksRepository.selectBook(id)
        }
    }

    fun onDeleteBookClick(id: Long) {
        dialogState = DialogState.Shown(id)
    }

    fun confirmDeleteBook(id: Long) {
        viewModelScope.launch {
            booksRepository.deleteBook(id)
        }
    }

    fun onDismissDialog() {
        dialogState = DialogState.Dismiss
    }
}

sealed interface MyBooksUiState {

    object Loading : MyBooksUiState

    data class Success(
        val booksList: List<Selectable<BooksModel>>
    ) : MyBooksUiState
}