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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
            if (booksRepository.deleteBook(id)) {
                onDismissDialog()
            }
        }
    }

    fun onDismissDialog() {
        dialogState = DialogState.Dismiss
    }
}

sealed interface MyBooksUiState {

    object Loading : MyBooksUiState

    data class Success(
        val booksList: List<Selectable<BooksModel>>,
    ) : MyBooksUiState
}
